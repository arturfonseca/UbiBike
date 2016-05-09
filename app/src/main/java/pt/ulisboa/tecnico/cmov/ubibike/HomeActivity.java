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
import java.util.HashMap;
import java.util.Hashtable;

import pt.ulisboa.tecnico.cmov.ubibike.domain.HtmlConnections;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        /*
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        */

        Button b1 = (Button) findViewById(R.id.buttonUserMenu);
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(HomeActivity.this, UserActivity.class));
            }
        });

        Button b2 = (Button) findViewById(R.id.buttonPointsMenu);
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(HomeActivity.this, PointsActivity.class));
            }
        });

        Button b3 = (Button) findViewById(R.id.buttonBookMenu);
        b3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new GetResult().execute("getStations");
            }
        });

        Button b4 = (Button) findViewById(R.id.buttonMessenger);
        b4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(HomeActivity.this, MessengerActivity.class));
            }
        });
    }

    private class GetResult extends AsyncTask<String, String, String> {

        protected String doInBackground(String... url) {
            return HtmlConnections.getResponse(url[0]);
        }

        protected void onPostExecute(String result) {
            HashMap<String,String> stations = new HashMap<String,String>();
            if(!result.equals("error")){
                byte[] buff = Base64.decode(result,Base64.DEFAULT);
                try {
                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buff));
                    stations = (HashMap<String,String>) ois.readObject();
                    ois.close();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Intent i = new Intent(HomeActivity.this, StationsMap.class);
            i.putExtra("stations",stations);
            startActivity(i);
        }
    }

}
