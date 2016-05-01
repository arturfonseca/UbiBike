package pt.ulisboa.tecnico.cmov.ubibike;

import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class TrajectoryMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

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

//        Intent intent = getIntent();
//        ArrayList<GPSCoordinate> trajectory = (ArrayList<GPSCoordinate>) intent.getSerializableExtra("trajectory");
//        LatLng startPosition = new LatLng(trajectory.get(0).getLatitude(),trajectory.get(0).getLongitude());
//        LatLng endPosition = new LatLng(trajectory.get(trajectory.size()-1).getLatitude(),trajectory.get(trajectory.size()-1).getLongitude());
//        mMap.addMarker(new MarkerOptions().position(endPosition));
//        mMap.addMarker(new MarkerOptions().position(startPosition));
//        PolylineOptions options = new PolylineOptions().width(15).color(Color.BLUE).geodesic(true);
//        for(GPSCoordinate coordinate: trajectory){
//            LatLng point = new LatLng(coordinate.getLatitude(),coordinate.getLongitude());
//            options.add(point);
//        }
//        mMap.addPolyline(options);
//        float zoomLevel = 16; //This goes up to 21
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPosition, zoomLevel));
    }
}
