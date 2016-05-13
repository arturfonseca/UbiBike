package pt.ulisboa.tecnico.cmov.ubibike;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;

import android.widget.Toast;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
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
import pt.ulisboa.tecnico.cmov.ubibike.domain.GPSCoordinate;
import pt.ulisboa.tecnico.cmov.ubibike.domain.HtmlConnections;
import pt.ulisboa.tecnico.cmov.ubibike.domain.Status;

public class PickDropActivity extends AppCompatActivity implements PeerListListener {

    private static final String PREFS_NAME = "UserAccount";
    private String userName = null;

    private boolean mBound = false;
    private SimWifiP2pManager mManager = null;
    private Messenger mService = null;
    private Channel mChannel = null;
    private WifiP2PBroadcastReceiver mReceiver = null;

    private ListView lv = null;

    private static boolean riding = false;
    private static boolean inside = false;
    private static String insideStation = "";

    private static ArrayList<GPSCoordinate> trajectory = new ArrayList<GPSCoordinate>();
    private static LocationManager locationManager;
    private static LocationListener locationListener;

    public static final String PREF_STATION = "Station";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_list_items);
        findViewById(R.id.buttonSearch).setEnabled(false);
        this.guiUpdateInitState();

        // initialize the WDSim API
        SimWifiP2pSocketManager.Init(this);


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
        //filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION);
        //filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION);
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
                Intent intent = new Intent(PickDropActivity.this, SimWifiP2pService.class);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

                findViewById(R.id.buttonSearch).setEnabled(true);

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
                findViewById(R.id.buttonSearch).setEnabled(false);
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
                Status.setS("Not riding");
                String result = new GetResult().execute("dropoff:" + userName + "," + insideStation).get();
                Toast.makeText(getApplicationContext(), "Drop on " + insideStation, Toast.LENGTH_SHORT).show();
                inside = false;
                int gpsPermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
                if(gpsPermission == PackageManager.PERMISSION_GRANTED) {
                    locationManager.removeUpdates(locationListener);
                }
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd@HH.mm.ss");
                Calendar c = Calendar.getInstance();
                String TrajectoryToString = ObjectToString(trajectory).split("\n")[0];
                Log.d("aruments","addTrajectory:" + this.userName + "," + dateFormat.format(c.getTime()) + "," + TrajectoryToString + "," + Integer.toString(trajectory.size()));
                new SendTrajectory().execute("addTrajectory:" + this.userName + "," + dateFormat.format(c.getTime()) + "," + TrajectoryToString + "," + Integer.toString(trajectory.size()));
                Log.d("String",TrajectoryToString);
                trajectory.clear();
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
                    Status.setS("Riding");
                    System.out.println(3 + station + bike);
                    String result = new GetResult().execute("pickup:" + userName + "," + insideStation).get();
                    Toast.makeText(getApplicationContext(), "Pickup on " + insideStation, Toast.LENGTH_SHORT).show();
                    riding = true;
                    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    locationListener = new MyLocationListener();
                    int gpsPermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
                    if(gpsPermission == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
                    }
                    SharedPreferences settings = getSharedPreferences(PREF_STATION+userName,0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("station", "null");
                    editor.apply();
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

    private void showTrajectoryError() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Error");
        alertDialogBuilder.setMessage("Could not send trajectory to server");

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

    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            //Toast.makeText(thisActivity,"Latitude: "+ latitude+" Longitude: "+longitude,Toast.LENGTH_SHORT).show();
            Log.v("--",latitude+";"+longitude);
            trajectory.add(new GPSCoordinate(latitude,longitude));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    }

    private class SendTrajectory extends AsyncTask<String, String, String> {

        protected String doInBackground(String... url) {
            Log.d("url",url[0]);
            return HtmlConnections.getResponse(url[0]);
        }

        protected void onPostExecute(String result) {
            if (result.equals("ERROR")) {
               showTrajectoryError(); // warn user
            }
        }
    }

    private String ObjectToString(Object object){
        byte[] buff = null;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(object);
            buff = bos.toByteArray();
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("String",Base64.encodeToString(buff,Base64.DEFAULT));
        return Base64.encodeToString(buff,Base64.NO_WRAP);
    }
}
