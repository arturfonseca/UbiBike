package pt.ulisboa.tecnico.cmov.ubibike;

import pt.inesc.termite.wifidirect.SimWifiP2pBroadcast;
import pt.inesc.termite.wifidirect.SimWifiP2pInfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;

public class WifiP2PBroadcastReceiver extends BroadcastReceiver {

    private final String TAG = "WifiP2PBroadcastReceive";

    private final Context mActivity;
    private final boolean isMessengerActivity;
    private final boolean isWifiPointsActivity;

    public WifiP2PBroadcastReceiver(Context activity) {
        super();
        this.mActivity = activity;
        isMessengerActivity = (activity instanceof MessengerActivity);
        isWifiPointsActivity = (activity instanceof WifiPointsActivity);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (SimWifiP2pBroadcast.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            // This action is triggered when the Termite service changes state:
            // - creating the service generates the WIFI_P2P_STATE_ENABLED event
            // - destroying the service generates the WIFI_P2P_STATE_DISABLED event

            int state = intent.getIntExtra(SimWifiP2pBroadcast.EXTRA_WIFI_STATE, -1);
            if (state == SimWifiP2pBroadcast.WIFI_P2P_STATE_ENABLED) {
                Toast.makeText(mActivity, "WiFi Direct enabled",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mActivity, "WiFi Direct disabled",
                        Toast.LENGTH_SHORT).show();
            }

        } else if (SimWifiP2pBroadcast.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            // Request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()

            Toast.makeText(mActivity, "Peer list changed",
                    Toast.LENGTH_SHORT).show();

            // very bad approach
            if (isMessengerActivity) {
                ((MessengerActivity) mActivity).updatePeersAvailable();
            } else if (isWifiPointsActivity) {
                ((WifiPointsActivity) mActivity).updatePeersAvailable();
            } else {
                Log.d(TAG, "ERROR P2P_PEERS_CHANGED getting class");
            }

        } else if (SimWifiP2pBroadcast.WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION.equals(action)) {

            /*
            SimWifiP2pInfo ginfo = (SimWifiP2pInfo) intent.getSerializableExtra(
                    SimWifiP2pBroadcast.EXTRA_GROUP_INFO);
            ginfo.print();
            */
            Toast.makeText(mActivity, "Network membership changed",
                    Toast.LENGTH_SHORT).show();

            // very bad approach
            if (isMessengerActivity) {
                ((MessengerActivity) mActivity).updateGroupAvailable();
            } else if (isWifiPointsActivity) {
                ((WifiPointsActivity) mActivity).updateGroupAvailable();
            } else {
                Log.d(TAG, "ERROR P2P_NETWORK_MEMBERSHIP_CHANGED getting class");
            }

        }
        /* NOT REALLY NEEDED, but useful for know who's GO and who's client
         -------------------------------------------------------------------------
        else if (SimWifiP2pBroadcast.WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION.equals(action)) {
            SimWifiP2pInfo ginfo = (SimWifiP2pInfo) intent.getSerializableExtra(
                    SimWifiP2pBroadcast.EXTRA_GROUP_INFO);
            ginfo.print();
            Toast.makeText(mActivity, "Group ownership changed",
                    Toast.LENGTH_SHORT).show();
            mActivity.updateGroupAvailable();
        }*/

    }

}
