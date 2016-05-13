package pt.ulisboa.tecnico.cmov.ubibike;

import android.content.Intent;
import android.graphics.Color;
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
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

import pt.ulisboa.tecnico.cmov.ubibike.domain.GPSCoordinate;

public class TrajectoryMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLngBounds bounds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trajectory_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.clear();
        Intent intent = getIntent();
        ArrayList<GPSCoordinate> trajectory = (ArrayList<GPSCoordinate>) intent.getSerializableExtra("trajectory");
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        LatLng startPosition = new LatLng(trajectory.get(0).getLatitude(),trajectory.get(0).getLongitude());
        builder.include(startPosition);
        LatLng endPosition = new LatLng(trajectory.get(trajectory.size()-1).getLatitude(),trajectory.get(trajectory.size()-1).getLongitude());
        builder.include(endPosition);
        mMap.addMarker(new MarkerOptions().position(endPosition));
        mMap.addMarker(new MarkerOptions().position(startPosition));
        PolylineOptions options = new PolylineOptions().width(15).color(Color.BLUE).geodesic(true);
        for(GPSCoordinate coordinate: trajectory){
            LatLng point = new LatLng(coordinate.getLatitude(),coordinate.getLongitude());
            options.add(point);
       }
        mMap.addPolyline(options);
        bounds = builder.build();
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds,50);
                mMap.animateCamera(cu);
            }
        });
    }
}
