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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView userName = (TextView) findViewById(R.id.textView5);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        userName.setText(settings.getString("userName", ""));

        String pointsText = getIntent().getStringExtra("points");

        TextView points = (TextView) findViewById(R.id.textView7);
        points.setText(pointsText);
    }

}
