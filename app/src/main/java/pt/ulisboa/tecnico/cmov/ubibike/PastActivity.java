package pt.ulisboa.tecnico.cmov.ubibike;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class PastActivity extends AppCompatActivity {

    private ArrayList<String> _trajectories = null;
    private ListView _listView;
    private ArrayAdapter<String> _adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        _listView = (ListView) findViewById(R.id.listView);
        _adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,_trajectories);
        _listView.setAdapter(_adapter);
        //Server.getPastTrajectories();
        _adapter.notifyDataSetChanged();
        _listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(getApplicationContext(),TrajectoryMapActivity.class);
                //ArrayList<GPSCoordinate> trajectory = Server.getTrajectory(_trajectories.get(position));
                //i.putExtra("trajectory",(ArrayList<GPSCoordinate>)trajectory);
                startActivity(i);
            }
        });
    }


}
