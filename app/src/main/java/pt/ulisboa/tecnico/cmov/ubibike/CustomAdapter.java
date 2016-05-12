package pt.ulisboa.tecnico.cmov.ubibike;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.places.personalized.PlaceAliasResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class CustomAdapter extends BaseAdapter {

    private final String TAG = "CustomAdapter";

    private LinkedHashMap<String, Integer> peersList = new LinkedHashMap<>();
    private LinkedHashMap<String, String> peersIP = new LinkedHashMap<>();

    private final Context mActivity;
    private final LayoutInflater inflater;
    private final boolean isMessengerActivity;
    private final boolean isWifiPointsActivity;

    public CustomAdapter(Context listActivity) {
        this.mActivity = listActivity;
        this.inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.isMessengerActivity = (listActivity instanceof MessengerActivity);
        this.isWifiPointsActivity = (listActivity instanceof WifiPointsActivity);
    }

    @Override
    public int getCount() {
        return peersList.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setPeersList(String[] name) {
        this.peersList.clear(); // first clear

        for (String temp : name) {
            String[] parsed = temp.split("|");

            this.peersList.put(parsed[0], R.drawable.off_state);
            this.peersIP.put(parsed[0], parsed[1]);
        }
    }

    public void setPeersList(List<String> name) {
        this.peersList.clear(); // first clear
        this.peersIP.clear(); // first clear

        for (String temp : name) {
            String[] parsed = temp.split("\\|\\?");

            this.peersList.put(parsed[0], R.drawable.off_state);
            this.peersIP.put(parsed[0], parsed[1]);
        }
    }

    public void updateGroupImage(String name) {
        this.peersList.put(name, R.drawable.on_state);
    }

    public class Holder {
        TextView tv;
        ImageView img;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        Holder holder = new Holder();
        View rowView;
        rowView = inflater.inflate(R.layout.list_item, null);

        holder.tv = (TextView) rowView.findViewById(R.id.textView1);

        final String name = (new ArrayList<>(peersList.keySet())).get(position);
        holder.tv.setText(name);

        holder.img = (ImageView) rowView.findViewById(R.id.imageView1);
        holder.img.setImageResource(peersList.get(name));

        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (peersList.get(name) == R.drawable.on_state) {

                    // very bad approach
                    if (isMessengerActivity) {
                        Toast.makeText(mActivity, "Clicked " + name, Toast.LENGTH_LONG).show();
                        ((MessengerActivity) mActivity).connectTo(name, peersIP.get(name));
                    } else if (isWifiPointsActivity) {
                        Toast.makeText(mActivity, "Clicked " + name, Toast.LENGTH_LONG).show();
                        ((WifiPointsActivity) mActivity).connectTo(name, peersIP.get(name));
                    } else {
                        Toast.makeText(mActivity, "Try again", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "ERROR Clicked_" + name + " getting class");
                    }

                } else {
                    // alert the user
                    new AlertDialog.Builder(mActivity)
                            .setTitle("Error")
                            .setMessage("User " + name + " not in group!")
                            .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // clicked OK
                                }
                            })
                            .show();
                }
            }
        });

        rowView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                // Show user more information about peer
                new AlertDialog.Builder(mActivity)
                        .setTitle("User")
                        .setMessage("User: " + name + "\nIP: " + peersIP.get(name))
                        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // clicked OK
                            }
                        })
                        .show();

                return true;
            }
        });

        return rowView;
    }

}
