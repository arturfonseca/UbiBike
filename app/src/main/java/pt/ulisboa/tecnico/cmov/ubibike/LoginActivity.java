package pt.ulisboa.tecnico.cmov.ubibike;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import pt.ulisboa.tecnico.cmov.ubibike.domain.HtmlConnections;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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


    private class GetResult extends AsyncTask<String, String, String> {
        String output;

        protected String doInBackground(String... url) {
            output = HtmlConnections.getResponse(url[0]);
            return null;
        }

        protected void onPostExecute(String result) {
            if (output.equals("OK"))
                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            else
                System.out.println("Error");

        }
    }

}
