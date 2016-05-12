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
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;

import android.widget.Toast;


import java.util.LinkedHashMap;

import pt.inesc.termite.wifidirect.SimWifiP2pBroadcast;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.SimWifiP2pDeviceList;
import pt.inesc.termite.wifidirect.SimWifiP2pManager;
import pt.inesc.termite.wifidirect.SimWifiP2pManager.Channel;
import pt.inesc.termite.wifidirect.SimWifiP2pManager.PeerListListener;
import pt.inesc.termite.wifidirect.service.SimWifiP2pService;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketManager;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;
import pt.ulisboa.tecnico.cmov.ubibike.domain.HtmlConnections;

public class PickDropActivity extends AppCompatActivity implements PeerListListener {


    private final static String TAG = "WifiPointsActivity";

    private static final String PREFS_NAME = "UserAccount";
    private String userName = null;
    private String name = null;

    private boolean mBound = false;
    private SimWifiP2pManager mManager = null;
    private Messenger mService = null;
    private Channel mChannel = null;
    private WifiP2PBroadcastReceiver mReceiver = null;

    private ListView lv = null;

    private static boolean riding = false;
    private static boolean inside = false;
    private static String insideStation = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_list_items);
        this.buttonUpdateOffState();
        this.guiUpdateInitState();

        // initialize the WDSim API
        SimWifiP2pSocketManager.Init(this);

        // register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION);
        this.mReceiver = new WifiP2PBroadcastReceiver(this);
        registerReceiver(this.mReceiver, filter);



        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        this.userName = settings.getString("userName", "");

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
                Intent intent = new Intent(PickDropActivity.this, SimWifiP2pService.class);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

                buttonUpdateOnState();

                // spawn the chat server background task


                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mManager.requestPeers(mChannel, PickDropActivity.this);

                    }
                }, 3000);

            } else { // turn Off
                if (mBound) {
                    unbindService(mConnection);
                    mBound = false;
                }
                buttonUpdateOffState();
                lv.setAdapter(null);
            }
        }
    };

    private View.OnClickListener listenerSearchButton = new View.OnClickListener() {
        public void onClick(View v) {
            if (mBound) {
                mManager.requestPeers(mChannel, PickDropActivity.this);

            } else {
                Toast.makeText(v.getContext(), "Service not bound",
                        Toast.LENGTH_SHORT).show();
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

    //- UPDATE BUTTON STATE OFF ----------------------------------------------------------------------
    private void buttonUpdateOffState() {
        ((Switch) findViewById(R.id.switchWifi)).setChecked(false);
        findViewById(R.id.buttonSearch).setEnabled(false);
    }

    //-- PEER CHANGE DISPLAY UPDATE ------------------------------------------------------------------
    protected void updatePeersAvailable() {
        if (this.mBound) {
            this.mManager.requestPeers(this.mChannel, PickDropActivity.this);
        } else {
            Toast.makeText(getApplicationContext(), "Service not bound",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPeersAvailable(SimWifiP2pDeviceList simWifiP2pDeviceList) {
        String station = "";
        String bike = "";
        // compile list of devices in range
        for (SimWifiP2pDevice device : simWifiP2pDeviceList.getDeviceList()) {
            if (device.deviceName.startsWith("station_")) {
                station = device.deviceName;
            } else if (device.deviceName.startsWith("bike_")) {
                bike = device.deviceName;

            }

        }
        pickupEvent(station, bike);
        dropoffEvent(station, bike);

    }

    private void dropoffEvent(String station, String bike) {
        if (!riding)
            return;
        try {
            if (!station.equals("") && !bike.equals("")) {
                System.out.println(1 + station + bike);
                inside = true;
                insideStation = station;
            } else if (station.equals("") && bike.equals("") && inside) {
                String result = new GetResult().execute("dropoff:" + userName + "," + insideStation).get();
                Toast.makeText(getApplicationContext(), "Drop on " + insideStation, Toast.LENGTH_SHORT).show();
                inside = false;


            }
        } catch (Exception e) {

        }

    }


    private void pickupEvent(String station, String bike) {
        if (riding)
            return;
        try {
            if (!station.equals("") && !bike.equals("")) {
                inside = true;
                insideStation = station;
            } else if (station.equals("") && !bike.equals("") && inside) {
                String booked = new GetResult().execute("booked:" + userName + "," + insideStation).get();
                if (booked.equals("OK")) {
                    System.out.println(3 + station + bike);
                    String result = new GetResult().execute("pickup:" + userName + "," + insideStation).get();
                    Toast.makeText(getApplicationContext(), "Pickup on " + insideStation, Toast.LENGTH_SHORT).show();
                    riding = true;
                }
                inside = false;

            }
        } catch (Exception e) {

        }

    }








    //- CONNECTION LOST ALERT DIALOG ----------------------------------------------------------------
    private void showError() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Error");
        alertDialogBuilder.setMessage("Book a bike and try again!");

        alertDialogBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                // User clicked OK
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }





    private class GetResult extends AsyncTask<String, Void, String> {
        String output;

        protected String doInBackground(String... url) {
            output = HtmlConnections.getResponse(url[0]);
            System.out.println(output);
            return output;
        }

        protected void onPostExecute(String result) {
            if (output.equals("ERROR")) {
                showError(); // warn user

            }
        }


    }
}
