package pt.ulisboa.tecnico.cmov.ubibike;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import pt.inesc.termite.wifidirect.SimWifiP2pBroadcast;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.SimWifiP2pInfo;
import pt.inesc.termite.wifidirect.SimWifiP2pManager;
import pt.inesc.termite.wifidirect.SimWifiP2pManager.Channel;
import pt.inesc.termite.wifidirect.service.SimWifiP2pService;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketManager;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;
import pt.inesc.termite.wifidirect.SimWifiP2pDeviceList;
import pt.inesc.termite.wifidirect.SimWifiP2pManager.PeerListListener;
import pt.inesc.termite.wifidirect.SimWifiP2pManager.GroupInfoListener;
import pt.ulisboa.tecnico.cmov.ubibike.domain.HtmlConnections;

public class WifiPointsActivity extends AppCompatActivity implements PeerListListener, GroupInfoListener {

    /* THIS CLASS SEND/RECEIVE PONTS WITH WIFI P2P AND ALSO TO SERVER */

    private final static String TAG = "WifiPointsActivity";

    private static final String PREFS_NAME = "UserAccount";
    private String userName = null;
    private String name = null;

    private boolean mBound = false;
    private SimWifiP2pManager mManager = null;
    private Messenger mService = null;
    private Channel mChannel = null;
    private WifiP2PBroadcastReceiver mReceiver = null;
    private SimWifiP2pSocketServer mSrvSocket = null;
    private SimWifiP2pSocket mCliSocket = null;

    private ListView lv = null;
    private CustomAdapter cAdapter = null;
    private LinkedHashMap<String, String> peersIP = new LinkedHashMap<>();

    private EditText mTextInput;
    private boolean isSendMenu = false;
    private boolean wantToReceive = false;
    private IncommingCommTask iCommTask = null;

    private final long withinTimestamp = 60000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_list_items);
        findViewById(R.id.buttonSearch).setEnabled(false);
        this.guiUpdateInitState();

        // initialize the WDSim API
        SimWifiP2pSocketManager.Init(this);

        this.cAdapter = new CustomAdapter(this);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        this.userName = settings.getString("userName", "");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION);
        this.mReceiver = new WifiP2PBroadcastReceiver(this);
        registerReceiver(this.mReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }


    //- BUTTON CALLBACK -----------------------------------------------------------------------------
    private Switch.OnCheckedChangeListener listenerWifiSwitch = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {


            if (isChecked) { // turn On
                Intent intent = new Intent(WifiPointsActivity.this, SimWifiP2pService.class);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

                buttonUpdateOnState();

                // spawn the chat server background task
                iCommTask = new IncommingCommTask();
                iCommTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mManager.requestPeers(mChannel, WifiPointsActivity.this);
                        mManager.requestGroupInfo(mChannel, WifiPointsActivity.this);
                    }
                }, 3000);

            } else { // turn Off
                if (mBound) {
                    unbindService(mConnection);
                    mBound = false;
                }

                findViewById(R.id.buttonSearch).setEnabled(false);
                lv.setAdapter(null);
            }
        }
    };

    private View.OnClickListener listenerSearchButton = new View.OnClickListener() {
        public void onClick(View v) {
            if (mBound) {
                mManager.requestPeers(mChannel, WifiPointsActivity.this);
                mManager.requestGroupInfo(mChannel, WifiPointsActivity.this);
            } else {
                Toast.makeText(v.getContext(), "Service not bound",
                        Toast.LENGTH_SHORT).show();
            }
        }
    };

    private View.OnClickListener listenerSendButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (!mTextInput.getText().toString().equals("")) {

                //Integer toSend = new Integer(mTextInput.getText().toString());

                new SendCommTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        mTextInput.getText().toString());

            }
        }
    };

    private View.OnClickListener listenerReceiveButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            wantToReceive = true;
        }
    };

    //- SOCKET CONNECTION TO BIND -------------------------------------------------------------------
    private ServiceConnection mConnection = new ServiceConnection() {
        // callbacks for service binding, passed to bindService()
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            mManager = new SimWifiP2pManager(mService);
            mChannel = mManager.initialize(getApplication(), getMainLooper(), null);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
            mManager = null;
            mChannel = null;
            mBound = false;
        }
    };

    //- DISPLAY VIEW INITIAL STATE OFF --------------------------------------------------------------
    private void guiUpdateInitState() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ((Switch) findViewById(R.id.switchWifi)).setOnCheckedChangeListener(listenerWifiSwitch);
        findViewById(R.id.buttonSearch).setOnClickListener(listenerSearchButton);

        this.lv = (ListView) findViewById(R.id.listView);
    }

    //- UPDATE BUTTON STATE ON ----------------------------------------------------------------------
    private void buttonUpdateOnState() {
        ((Switch) findViewById(R.id.switchWifi)).setChecked(true);
        findViewById(R.id.buttonSearch).setEnabled(true);
    }

    /*/- UPDATE BUTTON STATE OFF ----------------------------------------------------------------------
    private void buttonUpdateOffState() {
        ((Switch) findViewById(R.id.switchWifi)).setChecked(false);
        findViewById(R.id.buttonSearch).setEnabled(false);
    }*/

    //-- PEER CHANGE DISPLAY UPDATE ------------------------------------------------------------------
    protected void updatePeersAvailable() {
        if (this.mBound) {
            this.mManager.requestPeers(this.mChannel, WifiPointsActivity.this);
        } else {
            Toast.makeText(getApplicationContext(), "Service not bound",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPeersAvailable(SimWifiP2pDeviceList simWifiP2pDeviceList) {
        List<String> peersStr = new ArrayList<>();

        // compile list of devices in range
        for (SimWifiP2pDevice device : simWifiP2pDeviceList.getDeviceList()) {

            if (device.deviceName.startsWith("bike_") || device.deviceName.startsWith("station_")) {
                continue; // ignore
            }

            String devstr = "" + device.deviceName + " (" + device.getVirtIp() + ")";
            peersStr.add(devstr);

            this.peersIP.put(devstr, device.getVirtIp());
        }

        this.cAdapter.setPeersList(peersStr); // cAdapter updates list
        this.lv.setAdapter(this.cAdapter);
    }

    //- GROUP CHANGE DISPLAY UPDATE -----------------------------------------------------------------
    protected void updateGroupAvailable() {
        if (this.mBound) {
            this.mManager.requestPeers(this.mChannel, WifiPointsActivity.this);
            this.mManager.requestGroupInfo(this.mChannel, WifiPointsActivity.this);
        } else {
            Toast.makeText(getApplicationContext(), "Service not bound",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onGroupInfoAvailable(SimWifiP2pDeviceList simWifiP2pDeviceList, SimWifiP2pInfo simWifiP2pInfo) {

        // compile list of network members
        for (String deviceName : simWifiP2pInfo.getDevicesInNetwork()) {

            if (deviceName.startsWith("bike_") || deviceName.startsWith("station_")) {
                continue; // ignore
            }

            String deviceIP = simWifiP2pDeviceList.getByName(deviceName).getVirtIp();
            String devstr = "" + deviceName + " (" + deviceIP + ")";

            this.peersIP.put(devstr, deviceIP);

            this.cAdapter.updatePeerImage(devstr);
        }

        this.lv.setAdapter(this.cAdapter);
    }

    //- CONNECT TO GIVEN SERVER ---------------------------------------------------------------------
    public void connectTo(String name) {
        setContentView(R.layout.activity_wifi_points);
        setSupportActionBar(((Toolbar) findViewById(R.id.toolbar)));

        this.name = name;

        /* GET MY CURRENT POINTS AND SHOW */
        new GetResult().execute("getpoints:" + userName);

        new OutgoingCommTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this.peersIP.get(name));

        findViewById(R.id.buttonSendPoints).setOnClickListener(listenerSendButton);
        findViewById(R.id.buttonCollectPoints).setOnClickListener(listenerReceiveButton);
        this.mTextInput = (EditText) findViewById(R.id.extractPoints);

        this.isSendMenu = true;
    }

    //- SERVER RECEIVE CONNECTION TASK --------------------------------------------------------------
    private class IncommingCommTask extends AsyncTask<Void, String, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            long timestamp;
            try {
                mSrvSocket = new SimWifiP2pSocketServer(Integer.parseInt(getString(R.string.points_port)));
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (!Thread.currentThread().isInterrupted()) {

                if (wantToReceive) {
                    try {
                        SimWifiP2pSocket sock = mSrvSocket.accept();
                        try {
                            BufferedReader sockIn = new BufferedReader(new InputStreamReader(sock.getInputStream()));

                            timestamp = new Long(sockIn.readLine());  // first receive timeStamp
                            if ((new Date().getTime() - timestamp) < withinTimestamp) { // if within one minute then is valid

                                String st = sockIn.readLine();
                                if (st == null) {
                                    publishProgress(null);
                                    break;
                                }
                                publishProgress(st);
                                sock.getOutputStream().write(("\n").getBytes());
                            }

                        } catch (IOException e) {
                            // Log.d("Error reading socket:", e.getMessage());
                            publishProgress(null);

                        } finally {
                            sock.close();
                        }
                    } catch (IOException e) {
                        // Log.d("Error socket:", e.getMessage());
                        break;
                    }

                    wantToReceive = false;
                } //else
                //Thread.currentThread().interrupt();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (values != null) {
                Toast.makeText(getApplicationContext(), "Received " + values[0] + " points!",
                        Toast.LENGTH_SHORT).show();

                /* UPDATE SERVER THAT WE RECEIVED POINTS --------- */
                new GetResult().execute("incpoints:" + userName + "," + values[0]);

            } else {
                if (isSendMenu)
                    showError(0);
            }
        }

    }

    //- CLIENT CREATE CONNECTION TASK ---------------------------------------------------------------
    private class OutgoingCommTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                mCliSocket = new SimWifiP2pSocket(params[0], Integer.parseInt(getString(R.string.points_port)));
            } catch (UnknownHostException e) {
                return "Unknown Host:" + e.getMessage();
            } catch (IOException e) {
                return "IO error:" + e.getMessage();
            }
            return "OK";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "RESULT SOCKET_CLIENT " + result);
            super.onPostExecute(result);
        }
    }

    //- CLIENT SEND DATA ----------------------------------------------------------------------------
    private class SendCommTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... msg) {
            try {
                Date timestamp = new Date();
                String time = String.valueOf(timestamp.getTime());

                mCliSocket.getOutputStream().write((time + "\n").getBytes());
                mCliSocket.getOutputStream().write((msg[0] + "\n").getBytes());
                BufferedReader sockIn = new BufferedReader(new InputStreamReader(mCliSocket.getInputStream()));
                sockIn.readLine();
                mCliSocket.close();

            } catch (IOException e) {
                //e.printStackTrace();
                publishProgress(null);
            }
            mCliSocket = null;

            return msg[0];
        }

        @Override
        protected void onPostExecute(String result) {
            mTextInput.setText("");

            /* UPDATE SERVER THAT WE SENT POINTS --------- */
            new GetResult().execute("decpoints:" + userName + "," + result);

            Toast.makeText(getApplicationContext(), "Sent " + result + " points",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if ((values == null) && isSendMenu)
                showError(0);
        }

    }

    //- CONNECTION LOST ALERT DIALOG ----------------------------------------------------------------
    private void showError(int i) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Error");

        if (i == 0)
            alertDialogBuilder.setMessage("Connection to " + this.name + " LOST!");
        else
            alertDialogBuilder.setMessage("Connection to server LOST!");

        alertDialogBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                // User clicked OK
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            onBackPressed();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (this.isSendMenu) {
            if (this.mCliSocket != null) {
                try {
                    this.mCliSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            this.mCliSocket = null;

            setContentView(R.layout.activity_list_items);
            this.buttonUpdateOnState();
            this.guiUpdateInitState();

            this.mManager.requestPeers(this.mChannel, WifiPointsActivity.this);
            this.mManager.requestGroupInfo(this.mChannel, WifiPointsActivity.this);

            this.isSendMenu = false;

        } else {

            if (this.iCommTask != null) {
                this.iCommTask.cancel(true);
                this.iCommTask = null;
                try {
                    this.mSrvSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.mSrvSocket = null;
            }

            findViewById(R.id.buttonSearch).setEnabled(false);

            super.onBackPressed();
        }

    }

    private class GetResult extends AsyncTask<String, Void, String> {
        String output;

        protected String doInBackground(String... url) {
            output = HtmlConnections.getResponse(url[0]);
            System.out.println(output);
            return null;
        }

        protected void onPostExecute(String result) {
            if (!output.equals("ERROR")) {
                System.out.println(output);
                TextView t = (TextView) findViewById(R.id.textViewShowPoints);
                t.setText(output);

            } else
                showError(1); // warn user

        }
    }

}
