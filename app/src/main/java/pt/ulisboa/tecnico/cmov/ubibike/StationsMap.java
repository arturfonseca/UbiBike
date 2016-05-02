package pt.ulisboa.tecnico.cmov.ubibike;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class StationsMap extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLngBounds bounds;

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
        ArrayList<String> stations = getIntent().getStringArrayListExtra("stations");
        mMap.clear();
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for(String s: stations){
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
                mMap.moveCamera(cu);
            }
        });

    }
}
