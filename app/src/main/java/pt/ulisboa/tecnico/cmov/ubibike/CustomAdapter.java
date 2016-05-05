package pt.ulisboa.tecnico.cmov.ubibike;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class CustomAdapter extends BaseAdapter {

    private LinkedHashMap<String, Integer> peersList = new LinkedHashMap<>();
    private MessengerActivity mActivity;
    private LayoutInflater inflater;

    public CustomAdapter(MessengerActivity listMessengerActivity) {
        mActivity = listMessengerActivity;
        inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return peersList.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    public void setPeersList(String[] name) {
        if(!peersList.isEmpty())
            peersList.clear();

        for (String temp : name) {
            peersList.put(temp, R.drawable.off_state);
        }
    }

    public void updatePeerImg(String name, int img) {
        peersList.put(name, img);
    }

    public class Holder {
        TextView tv;
        ImageView img;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        Holder holder = new Holder();
        View rowView;
        rowView = inflater.inflate(R.layout.list_messenger, null);

        holder.tv = (TextView) rowView.findViewById(R.id.textView1);
        final String name = (new ArrayList<>(peersList.keySet())).get(position);
        holder.tv.setText(name);

        holder.img = (ImageView) rowView.findViewById(R.id.imageView1);
        holder.img.setImageResource(peersList.get(name));

        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = (new ArrayList<>(peersList.keySet())).get(position);

                if (peersList.get(name) == R.drawable.on_state) {
                    Toast.makeText(mActivity, "You Clicked " + name, Toast.LENGTH_LONG).show();
                    mActivity.connectTo((new ArrayList<>(peersList.keySet())).get(position));
                }
                else {
                    // alert the user
                    new AlertDialog.Builder(mActivity)
                            .setTitle("Error")
                            .setMessage("User " + name + " not in group!")
                            .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .show();
                }
            }
        });
        return rowView;
    }

}
