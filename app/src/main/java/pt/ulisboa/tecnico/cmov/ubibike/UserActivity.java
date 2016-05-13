package pt.ulisboa.tecnico.cmov.ubibike;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import pt.ulisboa.tecnico.cmov.ubibike.domain.GPSCoordinate;
import pt.ulisboa.tecnico.cmov.ubibike.domain.HtmlConnections;

public class UserActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "UserAccount";
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        this.userName = settings.getString("userName", "");

        Button getLastTrajectory = (Button) findViewById(R.id.button4);
        getLastTrajectory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new GetLastTrajectory().execute("getLastTrajectory:"+userName);
            }
        });


        Button pastTrajectories = (Button) findViewById(R.id.button5);
        pastTrajectories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(UserActivity.this,PastActivity.class));
            }
        });
/*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */
    }

    private class GetLastTrajectory extends AsyncTask<String, String, String> {

        protected String doInBackground(String... url) {
            return HtmlConnections.getResponse(url[0]);
        }

        protected void onPostExecute(String result) {
            if (result.equals("ERROR")) {
                showTrajectoryError(); // warn user
            }
            ArrayList<GPSCoordinate> _trajectory = parseTrajectoryFromServer(result);
            Log.d("GPS","Starting the count");
            for(GPSCoordinate g: _trajectory){
                Log.d("GPS",Double.toString(g.getLatitude()));
            }
            Intent i = new Intent(UserActivity.this,TrajectoryMapActivity.class);
            i.putExtra("trajectory",_trajectory);
            startActivity(i);

        }}

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
