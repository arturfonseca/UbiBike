package pt.ulisboa.tecnico.cmov.ubibike;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.cmov.ubibike.domain.HtmlConnections;

public class UserInfoActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "UserAccount";
    private String userName;
    private TextView points;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        this.userName = settings.getString("userName", "");

        new GetPoints().execute("getpoints:" + userName);

        points = (TextView) findViewById(R.id.textView7);
        points.setText(getString(R.string.upd_points_tag));

        TextView user = (TextView) findViewById(R.id.textViewUsername);
        user.setText(this.userName);

    }

    private class GetPoints extends AsyncTask<String, String, String> {

        protected String doInBackground(String... url) {
            return HtmlConnections.getResponse(url[0]);
        }

        protected void onPostExecute(String result) {
            if (result.equals("ERROR")) {
                points.setText("Error");
            } else {
                points.setText(result + " points");
            }

        }
    }
}
