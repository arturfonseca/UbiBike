package pt.ulisboa.tecnico.cmov.ubibike;

import android.content.Intent;
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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.cmov.ubibike.domain.HtmlConnections;

public class BookActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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

        Button buttonListStations = (Button) findViewById(R.id.buttonListStations);
        buttonListStations.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                new GetResult().execute("getStations");

            }
        });
    }


    private class GetResult extends AsyncTask<String, String, String> {

        protected String doInBackground(String... url) {
            return HtmlConnections.getResponse(url[0]);
        }

        protected void onPostExecute(String result) {
            ArrayList<String> stations = new ArrayList<String>();
            if(!result.equals("error")){
                byte[] buff = Base64.decode(result,Base64.DEFAULT);
                try {
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buff));
                    stations = (ArrayList<String>) ois.readObject();
                    ois.close();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Log.d("Stations",result);
            Log.d("Stations", Arrays.toString(stations.toArray()));
            Intent i = new Intent(BookActivity.this, StationsMap.class);
            i.putExtra("stations",(ArrayList<String>)stations);
            startActivity(i);
        }
    }

}
