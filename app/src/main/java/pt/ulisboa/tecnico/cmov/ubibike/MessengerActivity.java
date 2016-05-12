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
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

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

/*
TODO
integridade nos pontos
*/

public class MessengerActivity extends AppCompatActivity implements PeerListListener, GroupInfoListener {

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

    private TextView mTextInput;
    private EditText mTextOutput;
    private boolean isMessenger = false;
    private boolean isServerRunning = false;
    private IncommingCommTask iCommTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_list_items);
        findViewById(R.id.buttonSearch).setEnabled(false);
        this.guiUpdateInitState();

        // initialize the WDSim API
        SimWifiP2pSocketManager.Init(getApplicationContext());

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
    protected void onPause() {
        super.onPause();
        unregisterReceiver(this.mReceiver);

    }

    //- BUTTON CALLBACK -----------------------------------------------------------------------------
    private Switch.OnCheckedChangeListener listenerWifiSwitch = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            if (isChecked) { // turn On
                Intent intent = new Intent(MessengerActivity.this, SimWifiP2pService.class);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                mBound = true;

                System.err.println("========================================================================================> DOING ON");
                findViewById(R.id.buttonSearch).setEnabled(true);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mManager.requestPeers(mChannel, MessengerActivity.this);
                        mManager.requestGroupInfo(mChannel, MessengerActivity.this);
                    }
                }, 3000);

            } else { // turn Off
                if (mBound) {
                    unbindService(mConnection);
                    mBound = false;
                }
                System.err.println("========================================================================================> DOING OFF");
                findViewById(R.id.buttonSearch).setEnabled(false);
                lv.setAdapter(null);
            }
        }
    };

    private View.OnClickListener listenerSearchButton = new View.OnClickListener() {
        public void onClick(View v) {
            if (mBound) {
                mManager.requestPeers(mChannel, MessengerActivity.this);
                mManager.requestGroupInfo(mChannel, MessengerActivity.this);
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

                String toSend = "" + userName + ": " + mTextInput.getText().toString();
                new SendCommTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        toSend);

                CharSequence text = "" + userName + ": " + mTextInput.getText().toString() + "\n";
                mTextOutput.append(text);
            }
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

    //-- PEER CHANGE DISPLAY UPDATE ------------------------------------------------------------------
    protected void updatePeersAvailable() {
        if (this.mBound) {
            this.mManager.requestPeers(this.mChannel, MessengerActivity.this);
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
            this.mManager.requestPeers(this.mChannel, MessengerActivity.this);
            this.mManager.requestGroupInfo(this.mChannel, MessengerActivity.this);
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

    //- CONNECTION LOST ALERT DIALOG ----------------------------------------------------------------
    private void showError() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Error");
        alertDialogBuilder.setMessage("Connection to " + this.name + " LOST!");

        alertDialogBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                // User clicked OK
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    //- CONNECT TO GIVEN SERVER ---------------------------------------------------------------------
    public void connectTo(String name) {
        setContentView(R.layout.activity_messenger);

        this.name = name;

        if (!isServerRunning) {
            // spawn the chat server background task
            iCommTask = new IncommingCommTask();
            iCommTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            isServerRunning = true;
        }

        new OutgoingCommTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this.peersIP.get(this.name));

        findViewById(R.id.buttonSend).setOnClickListener(listenerSendButton);
        this.mTextInput = (TextView) findViewById(R.id.editTextToSend);
        this.mTextOutput = (EditText) findViewById(R.id.listViewMessenger);

        this.isMessenger = true;
    }

    //- SERVER RECEIVE CONNECTION TASK --------------------------------------------------------------
    private class IncommingCommTask extends AsyncTask<Void, String, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            int count = 0;

            try {
                mSrvSocket = new SimWifiP2pSocketServer(Integer.parseInt(getString(R.string.msg_port)));
            } catch (IOException e) {
                System.err.println("mSrvSocket = new SimWifiP2pSocketServer(Integer.parseInt(getString(R.string.msg_port)));");
                e.printStackTrace();
            }

            SimWifiP2pSocket sock = null;
            try {
                sock = mSrvSocket.accept();
            } catch (IOException e) {
                System.err.println("sock = mSrvSocket.accept();");
                e.printStackTrace();
            }

            while (!Thread.currentThread().isInterrupted()) {

                try {
                    BufferedReader sockIn = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                    String st = sockIn.readLine();
                    if (st == null) {

                        if (count == 1) { // two times down => CANCEL
                            publishProgress(null);
                            break;
                        }
                        count++;
                    }
                    publishProgress(st);
                    sock.getOutputStream().write(("\n").getBytes());

                } catch (IOException e) {
                    //Log.d("Error reading socket:", e.getMessage());
                    publishProgress(null);
                }
            }

            try {
                sock.close();
                sock = null;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (values != null)
                mTextOutput.append(values[0] + "\n");
            else {
                if (isMessenger)
                    showError();
            }
        }

    }

    //- CLIENT CREATE CONNECTION TASK ---------------------------------------------------------------
    private class OutgoingCommTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                mCliSocket = new SimWifiP2pSocket(params[0], Integer.parseInt(getString(R.string.msg_port)));
            } catch (UnknownHostException e) {
                return "Unknown Host:" + e.getMessage();
            } catch (IOException e) {
                return "IO error:" + e.getMessage();
            }
            return "OK";
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }

    //- CLIENT SEND DATA ----------------------------------------------------------------------------
    private class SendCommTask extends AsyncTask<String, String, Void> {
        @Override
        protected Void doInBackground(String... msg) {
            try {
                mCliSocket.getOutputStream().write((msg[0] + "\n").getBytes());
                BufferedReader sockIn = new BufferedReader(new InputStreamReader(mCliSocket.getInputStream()));
                sockIn.readLine();

            } catch (IOException e) {
                //e.printStackTrace();
                publishProgress(null);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mTextInput.setText("");
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if ((values == null) && isMessenger)
                showError();
        }

    }

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

    /*- UPDATE BUTTON STATE OFF ----------------------------------------------------------------------
    private void buttonUpdateOffState() {
        //((Switch) findViewById(R.id.switchWifi)).setChecked(false);
        findViewById(R.id.buttonSearch).setEnabled(false);
    } */

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (this.isMessenger) {
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

            this.mManager.requestPeers(this.mChannel, MessengerActivity.this);
            this.mManager.requestGroupInfo(this.mChannel, MessengerActivity.this);

            this.isMessenger = false;

        } else {
            if (this.isServerRunning) {
                this.iCommTask.cancel(true);
                this.iCommTask = null;
                try {
                    this.mSrvSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.mSrvSocket = null;
            }

            if (mBound) {
                unbindService(mConnection);
            }

            super.onBackPressed();
        }

    }

}
