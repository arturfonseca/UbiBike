package pt.ulisboa.tecnico.cmov.ubibike;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import pt.ulisboa.tecnico.cmov.ubibike.domain.HtmlConnections;

public class PointsActivity extends AppCompatActivity {

    /* THIS CLASS SEND/RECEIVE PONTS TO SERVER ONLY */

    private String userName;
    private static final String PREFS_NAME = "UserAccount";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_points);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        this.userName = settings.getString("userName", "");
        new GetResult().execute("getpoints:" + userName);

        Button fab = (Button) findViewById(R.id.buttonSendPoints);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int points = Integer.parseInt(((EditText) findViewById(R.id.extractPoints)).getText().toString());
                String user = ((EditText) findViewById(R.id.extractUsername)).getText().toString();
                new GetResult().execute("transferpoints:" + userName + "," + points + "," + user);
            }
        });

    }

    private class GetResult extends AsyncTask<String, Void, String> {
        String output;

        protected String doInBackground(String... url) {
            output = HtmlConnections.getResponse(url[0]);
            //System.out.println(output);
            return output;
        }

        protected void onPostExecute(String result) {
            TextView t = (TextView) findViewById(R.id.textViewShowPoints);

            if (!output.equals("ERROR")) {
                //System.out.println(output);
                t.setText(output);
            } else {
                t.setText("ERROR");
                showError();
            }

        }

    }

    private void showError() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Error");
        alertDialogBuilder.setMessage("Connection to server LOST!");

        alertDialogBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                // User clicked OK
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

}
