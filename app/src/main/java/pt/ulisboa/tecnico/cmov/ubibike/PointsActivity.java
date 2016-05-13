package pt.ulisboa.tecnico.cmov.ubibike;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.ExpandedMenuView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import pt.ulisboa.tecnico.cmov.ubibike.domain.HtmlConnections;

public class PointsActivity extends AppCompatActivity {

    /* THIS CLASS SEND/RECEIVE PONTS TO SERVER ONLY */

    private String userName;
    private static final String PREFS_NAME = "UserAccount";

    private int points = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_points);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        this.userName = settings.getString("userName", "");
        new GetResult().execute("getpoints:" + userName);

        ((TextView) findViewById(R.id.textViewShowPoints)).setText(getString(R.string.upd_points_tag));

        Button fab = (Button) findViewById(R.id.buttonSendPoints);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pointsSend;
                try {
                    pointsSend = Integer.parseInt(((EditText) findViewById(R.id.extractPoints)).getText().toString());

                } catch (NumberFormatException e) {
                    ((EditText) findViewById(R.id.extractPoints)).setText("");
                    ((EditText) findViewById(R.id.extractUsername)).setText("");
                    return;
                }
                if (pointsSend == 0) {
                    ((EditText) findViewById(R.id.extractPoints)).setText("");
                    ((EditText) findViewById(R.id.extractUsername)).setText("");
                    return;
                }

                if (points != -1) {
                    if (pointsSend > points)
                        pointsSend = points;
                }
                String user = ((EditText) findViewById(R.id.extractUsername)).getText().toString();

                new GetResult().execute("transferpoints:" + userName + "," + pointsSend + "," + user);

                Toast.makeText(view.getContext(), "Sent " + pointsSend + " points",
                        Toast.LENGTH_SHORT).show();


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
            ((EditText) findViewById(R.id.extractPoints)).setText("");
            ((EditText) findViewById(R.id.extractUsername)).setText("");

            TextView t = (TextView) findViewById(R.id.textViewShowPoints);

            if (output.equals("ERROR")) {
                t.setText("ERROR");
                showError();
                points = -1;

            } else {
                //System.out.println(output);
                t.setText(output + " points");
                points = Integer.parseInt(result);
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

