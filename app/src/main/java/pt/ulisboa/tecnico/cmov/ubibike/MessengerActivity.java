package pt.ulisboa.tecnico.cmov.ubibike;

import android.content.ComponentName;
import android.content.Context;
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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;

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

public class MessengerActivity extends AppCompatActivity implements PeerListListener, GroupInfoListener {

    public static final String PREFS_NAME = "UserAccount";
    private String userName;

    private boolean mBound = false;
    private SimWifiP2pManager mManager = null;
    private Messenger mService = null;
    private Channel mChannel = null;
    private WifiP2PBroadcastReceiver mReceiver = null;
    private SimWifiP2pSocketServer mSrvSocket = null;
    private SimWifiP2pSocket mCliSocket = null;

    private CustomAdapter cAdapter = null;
    private ListView lv = null;
    private TextView mTextInput;
    private EditText mTextOutput;
    private boolean isMessenger = false;

    private IncommingCommTask iCommTask = null;
    private OutgoingCommTask oCommTask = null;

    private LinkedHashMap<String, String> peersIP = new LinkedHashMap<>();
    private String name = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_list_messenger);
        this.buttonUpdateInitState();
        this.guiUpdateInitState();

        // initialize the WDSim API
        SimWifiP2pSocketManager.Init(getApplicationContext());

        // register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION);
        mReceiver = new WifiP2PBroadcastReceiver(this);
        registerReceiver(mReceiver, filter);

        cAdapter = new CustomAdapter(this);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        userName = settings.getString("userName", "");
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    private View.OnClickListener listenerWifiOnButton = new View.OnClickListener() {
        public void onClick(View v) {

            Intent intent = new Intent(getApplicationContext(), SimWifiP2pService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

            findViewById(R.id.buttonWifiOn).setEnabled(false);
            findViewById(R.id.buttonWifiOff).setEnabled(true);
            findViewById(R.id.buttonSearch).setEnabled(true);

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mManager.requestPeers(mChannel, MessengerActivity.this);
                }
            }, 3000);
        }
    };

    private View.OnClickListener listenerWifiOffButton = new View.OnClickListener() {
        public void onClick(View v) {
            if (mBound) {
                unbindService(mConnection);
                mBound = false;

                findViewById(R.id.buttonWifiOn).setEnabled(true);
                findViewById(R.id.buttonWifiOff).setEnabled(false);
                findViewById(R.id.buttonSearch).setEnabled(false);
                lv.setAdapter(null);
            }
        }
    };

    private View.OnClickListener listenerInRangeButton = new View.OnClickListener() {
        public void onClick(View v) {
            if (mBound) {
                mManager.requestPeers(mChannel, MessengerActivity.this);
                mManager.requestGroupInfo(mChannel, MessengerActivity.this);
            } else {
                Toast.makeText(v.getContext(), "Service not bound", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private View.OnClickListener listenerSendButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new SendCommTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    mTextInput.getText().toString());

            CharSequence text = "" + userName + ": " + mTextInput.getText().toString() + "\n";
            mTextOutput.append(text);
        }
    };

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

    public void updateGroupAvailable() {
        if (mBound) {
            System.err.println("=======================================================================> before UPDATING");
            mManager.requestGroupInfo(mChannel, MessengerActivity.this);
        } else {
            Toast.makeText(getApplicationContext(), "Service not bound",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onGroupInfoAvailable(SimWifiP2pDeviceList simWifiP2pDeviceList, SimWifiP2pInfo simWifiP2pInfo) {

        // compile list of network members
        for (String deviceName : simWifiP2pInfo.getDevicesInNetwork()) {
            System.err.println("=======================================================================> UPDATING");
            SimWifiP2pDevice device = simWifiP2pDeviceList.getByName(deviceName);
            String devstr = "" + deviceName + " (" + device.getVirtIp() + ")";

            peersIP.put(devstr, device.getVirtIp());

            cAdapter.updatePeerImg(devstr, R.drawable.on_state);

            /* ---------------------------------------------------------------------------> TODO
            if (isMessenger) {
                if (name.equals(devstr)) {

                    // alert the user
                    new AlertDialog.Builder(this)
                            .setTitle("Error")
                            .setMessage("User " + devstr + " left")
                            .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .show();
                }

            }*/
        }

        lv.setAdapter(cAdapter);
    }

    public void updatePeersAvailable() {
        if (mBound) {
            mManager.requestPeers(mChannel, MessengerActivity.this);
        } else {
            Toast.makeText(getApplicationContext(), "Service not bound",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPeersAvailable(SimWifiP2pDeviceList simWifiP2pDeviceList) {
        String[] peersStr = new String[simWifiP2pDeviceList.getDeviceList().size()];
        int i = 0;

        // compile list of devices in range
        for (SimWifiP2pDevice device : simWifiP2pDeviceList.getDeviceList()) {
            String devstr = "" + device.deviceName + " (" + device.getVirtIp() + ")";
            peersStr[i] = devstr;

            peersIP.put(devstr, device.getVirtIp());

            i++;
        }
        cAdapter.setPeersList(peersStr);
        lv.setAdapter(cAdapter);
    }


    public void connectTo(String name) {
        setContentView(R.layout.activity_messenger);

        this.name = name;

        // spawn the chat server background task
        iCommTask = new IncommingCommTask();
        iCommTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        oCommTask = new OutgoingCommTask();
        oCommTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, peersIP.get(name));

        findViewById(R.id.buttonSend).setOnClickListener(listenerSendButton);
        mTextInput = (TextView) findViewById(R.id.editTextToSend);
        mTextOutput = (EditText) findViewById(R.id.listViewMessenger);

        isMessenger = true;
    }

    public class OutgoingCommTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                mCliSocket = new SimWifiP2pSocket(params[0], Integer.parseInt(getString(R.string.port)));
            } catch (UnknownHostException e) {
                return "Unknown Host:" + e.getMessage();
            } catch (IOException e) {
                return "IO error:" + e.getMessage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }

    public class IncommingCommTask extends AsyncTask<Void, String, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                mSrvSocket = new SimWifiP2pSocketServer(Integer.parseInt(getString(R.string.port)));
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (!Thread.currentThread().isInterrupted()) {

                if (!isMessenger)
                    break;

                try {
                    SimWifiP2pSocket sock = mSrvSocket.accept();

                    try {
                        BufferedReader sockIn = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                        String st = sockIn.readLine();
                        publishProgress(st);
                        sock.getOutputStream().write(("\n").getBytes());
                    } catch (IOException e) {
                        Log.d("Error reading socket:", e.getMessage());
                    } finally {
                        sock.close();
                    }

                } catch (IOException e) {
                    Log.d("Error socket:", e.getMessage());
                    break;
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            mTextOutput.append(values[0] + "\n");
        }
    }

    public class SendCommTask extends AsyncTask<String, String, Void> {

        @Override
        protected Void doInBackground(String... msg) {
            try {
                mCliSocket.getOutputStream().write((msg[0] + "\n").getBytes());
                BufferedReader sockIn = new BufferedReader(new InputStreamReader(mCliSocket.getInputStream()));
                sockIn.readLine();
                mCliSocket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mTextInput.setText("");
        }
    }

    public void guiUpdateInitState() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        findViewById(R.id.buttonWifiOn).setOnClickListener(listenerWifiOnButton);
        findViewById(R.id.buttonWifiOff).setOnClickListener(listenerWifiOffButton);
        findViewById(R.id.buttonSearch).setOnClickListener(listenerInRangeButton);

        lv = (ListView) findViewById(R.id.listView);
    }

    public void buttonUpdateInitState() {
        findViewById(R.id.buttonWifiOn).setEnabled(true);
        findViewById(R.id.buttonWifiOff).setEnabled(false);
        findViewById(R.id.buttonSearch).setEnabled(false);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onBackPressed() {
        if (isMessenger) {
            if (mCliSocket != null) {
                try {
                    mCliSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mCliSocket = null;

            iCommTask = null;
            oCommTask = null;

            setContentView(R.layout.activity_list_messenger);
            this.guiUpdateInitState();
            findViewById(R.id.buttonWifiOn).setEnabled(false);
            findViewById(R.id.buttonWifiOff).setEnabled(true);
            findViewById(R.id.buttonSearch).setEnabled(true);
            lv.setAdapter(cAdapter);

            isMessenger = false;
        } else {
            if (mBound)
                unbindService(mConnection);

            super.onBackPressed();
        }

    }

}
