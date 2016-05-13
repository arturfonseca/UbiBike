package pt.ulisboa.tecnico.cmov.ubibike;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.cmov.ubibike.domain.GPSCoordinate;
import pt.ulisboa.tecnico.cmov.ubibike.domain.HtmlConnections;

public class PastActivity extends AppCompatActivity {

    private ArrayList<String> _trajectories = new ArrayList<String>();
    private ListView _listView;
    private ArrayAdapter<String> _adapter;
    private ArrayList<GPSCoordinate> _trajectory = new ArrayList<GPSCoordinate>();
    private static final String PREFS_NAME = "UserAccount";
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        this.userName = settings.getString("userName", "");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        _listView = (ListView) findViewById(R.id.listView);
        new GetAllTrajectories().execute("getAllTrajectories:" + userName);

        _listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                _trajectory = null;
                new GetTrajectory().execute("getTrajectory:" + userName + "," + _trajectories.get(position));
            }
        });
    }

    private class GetAllTrajectories extends AsyncTask<String, String, String> {

        protected String doInBackground(String... url) {
            return HtmlConnections.getResponse(url[0]);
        }

        protected void onPostExecute(String result) {
            if (result.equals("ERROR")) {
                showTrajectoryError(); // warn user
            }
            _trajectories = parseDatesFromServer(result);
            Log.d("Trajectory", Arrays.toString(_trajectories.toArray()));
            _adapter = new ArrayAdapter<String>(PastActivity.this,android.R.layout.simple_list_item_1,_trajectories);
            _listView.setAdapter(_adapter);
        }
    }

    private class GetTrajectory extends AsyncTask<String, String, String> {

        protected String doInBackground(String... url) {
            return HtmlConnections.getResponse(url[0]);
        }

        protected void onPostExecute(String result) {
            if (result.equals("ERROR")) {
                showTrajectoryError(); // warn user
            }
            _trajectory = parseTrajectoryFromServer(result);
            Log.d("GPS","Starting the count");
            for(GPSCoordinate g: _trajectory){
                Log.d("GPS",Double.toString(g.getLatitude()));
            }
            Intent i = new Intent(PastActivity.this,TrajectoryMapActivity.class);
            i.putExtra("trajectory",_trajectory);
            startActivity(i);

        }
    }

    private ArrayList<String> parseDatesFromServer(String result){
        byte[] buff2 = Base64.decode(result,Base64.DEFAULT);
        ArrayList<String> lista = new ArrayList<>();
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buff2));
            lista = (ArrayList<String>) ois.readObject();
            ois.close();
        }catch(Exception e){}
        return lista;
    }

    private ArrayList<GPSCoordinate> parseTrajectoryFromServer(String result){
        Log.d("result",result);
        byte[] buff2 = Base64.decode(result,Base64.NO_WRAP);
        ArrayList<GPSCoordinate> lista = new ArrayList<>();
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buff2));
            lista = (ArrayList<GPSCoordinate>) ois.readObject();
            ois.close();
        }catch(Exception e){}
        Log.d("Number of coordinates",Integer.toString(lista.size()));
        return lista;
    }

    private void showTrajectoryError() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Error");
        alertDialogBuilder.setMessage("Could not get previous trajectories");

        alertDialogBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                // User clicked OK
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

}
