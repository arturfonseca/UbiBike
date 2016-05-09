package pt.ulisboa.tecnico.cmov.ubibike;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import pt.ulisboa.tecnico.cmov.ubibike.domain.HtmlConnections;

public class LoginActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "UserAccount";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Try to get user settings
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String userName = settings.getString("userName", "");
        if(!userName.equals("")) {
            EditText text = (EditText) findViewById(R.id.ExtractUsername);
            text.setText(userName, TextView.BufferType.EDITABLE);
            String password = settings.getString("password", "");
            text = (EditText) findViewById(R.id.ExtractPassword);
            text.setText(password, TextView.BufferType.EDITABLE);
        }

        Button bLogin = (Button) findViewById(R.id.buttonLogin);
        bLogin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                try {
                    EditText text = (EditText) findViewById(R.id.ExtractUsername);
                    String name = text.getText().toString();
                    text = (EditText) findViewById(R.id.ExtractPassword);
                    String pw = text.getText().toString();
                    if (name.equals(".") && pw.equals("."))  //FOR TEST ONLY
                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    else
                        new GetResult().execute("login:" + name + "," + pw);
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });

        Button bRegister = (Button) findViewById(R.id.buttonRegister);
        bRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

    }

    public void showError(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Error trying to login\nPlease try again");

        alertDialogBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
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
                String name = ((EditText) findViewById(R.id.ExtractUsername)).getText().toString();
                String pw = ((EditText) findViewById(R.id.ExtractPassword)).getText().toString();
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("userName", name);
                editor.putString("password", pw);
                editor.commit();

                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            }
            else
                showError(); // warn user

        }
    }

}
