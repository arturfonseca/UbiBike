package pt.ulisboa.tecnico.cmov.ubibike;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import pt.ulisboa.tecnico.cmov.ubibike.domain.HtmlConnections;

public class RegisterActivity extends AppCompatActivity {

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
                    if (pw1.equals(pw2))
                        new GetResult().execute("regist:" + name + "," + pw1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
                startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
            else
                System.out.println("Error");

        }
    }

}
