package pt.ulisboa.tecnico.cmov.ubibike;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import pt.ulisboa.tecnico.cmov.ubibike.domain.HtmlConnections;

public class StationsMap extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private LatLngBounds bounds;
    private Activity thisActivity = this;
    private HashMap<String,String> stations;
    public static final String PREFS_NAME = "UserAccount";
    public static final String PREF_STATION = "Station";
    private String currentStation;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stations_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.clear();
        stations = (HashMap<String,String>) getIntent().getSerializableExtra("stations");
        mMap.setOnMarkerClickListener(this);
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for(String s: stations.keySet()){
            double latitude = Double.parseDouble(s.split(",")[0]);
            double longitude = Double.parseDouble(s.split(",")[1]);
            LatLng position = new LatLng(latitude,longitude);
            mMap.addMarker(new MarkerOptions().position(position));
            builder.include(position);
        }
        bounds = builder.build();
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds,50);
                mMap.animateCamera(cu);
            }
        });

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        userName = settings.getString("userName", "");
        SharedPreferences station = getSharedPreferences(PREF_STATION+userName,0);
        if(station.getString("station","null").equals("null")){
            String coordinates = marker.getPosition().latitude + "," + marker.getPosition().longitude;
            currentStation = stations.get(coordinates);
            AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity);
            builder.setMessage("Do you wish to book a bike at " + currentStation + "?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new GetResult().execute("book:" + userName + "," + currentStation);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            builder.create().show();
        }else{
            Toast.makeText(thisActivity,"You already have a bike booked at "+station.getString("station","null"), Toast.LENGTH_LONG).show();
        }
        return true;
    }

    private class GetResult extends AsyncTask<String, String, String> {

        protected String doInBackground(String... url) {
            return HtmlConnections.getResponse(url[0]);
        }

        protected void onPostExecute(String result) {
            if(!result.equals("error")){}
                Toast.makeText(thisActivity,"Bike Booked",Toast.LENGTH_LONG).show();
                SharedPreferences settings = getSharedPreferences(PREF_STATION+userName,0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("station", currentStation);
                editor.apply();
        }
    }
}
