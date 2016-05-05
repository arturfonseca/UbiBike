package pt.ulisboa.tecnico.cmov.ubibike;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import pt.ulisboa.tecnico.cmov.ubibike.domain.HtmlConnections;

public class RegisterActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "UserAccount";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


       Button bRegist = (Button) findViewById(R.id.buttonRegister);
       bRegist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    EditText text = (EditText) findViewById(R.id.editTextUsername);
                    String name = text.getText().toString();
                    text = (EditText) findViewById(R.id.editTextPassword);
                    String pw1 = text.getText().toString();
                    text = (EditText) findViewById(R.id.editTextRetypePassword);
                    String pw2 = text.getText().toString();
                    if (!pw1.equals(pw2))
                        showError(0);
                    else
                        new GetResult().execute("regist:" + name + "," + pw1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public void showError(int i) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        if(i == 0)
            alertDialogBuilder.setMessage("Password's don't match.");
        else if (i == 1)
            alertDialogBuilder.setMessage("Error trying to register\nPlease try again");

        alertDialogBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                // User clicked OK
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private class GetResult extends AsyncTask<String, String, String> {
        String output;

        protected String doInBackground(String... url) {
            output = HtmlConnections.getResponse(url[0]);
            return null;
        }

        protected void onPostExecute(String result) {
            if (output.equals("OK")) {

                // save user settings
                String name = ((EditText) findViewById(R.id.editTextUsername)).getText().toString();
                String pw = ((EditText) findViewById(R.id.editTextPassword)).getText().toString();
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("userName", name);
                editor.putString("password", pw);
                editor.commit();

                startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
            }
            else
                showError(1);

        }
    }

}
