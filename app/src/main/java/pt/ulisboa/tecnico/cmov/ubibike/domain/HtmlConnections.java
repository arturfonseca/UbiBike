package pt.ulisboa.tecnico.cmov.ubibike.domain;

import android.os.AsyncTask;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Created by Artur Fonseca on 28/04/2016.
 */
public final class HtmlConnections {


   public static String getResponse(String cmd) {
        String stringText = "";
        String url = "http://10.0.2.2:8000/run?";
        try {
            URL textUrl = new URL(url + cmd);
            BufferedReader bufferReader = new BufferedReader(new InputStreamReader(textUrl.openStream()));
            String StringBuffer;
            while ((StringBuffer = bufferReader.readLine()) != null) {
                stringText += StringBuffer;
            }
            bufferReader.close();
        } catch (Exception e) {
            e.printStackTrace();
            stringText = "ERROR";
        }
        return stringText;
    }
}

