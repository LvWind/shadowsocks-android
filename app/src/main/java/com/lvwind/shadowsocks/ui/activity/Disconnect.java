package com.lvwind.shadowsocks.ui.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import com.lvwind.shadowsocks.Constants;
import com.lvwind.shadowsocks.IShadowsocksService;
import com.lvwind.shadowsocks.R;
import com.lvwind.shadowsocks.ShadowsocksVpnService;

/**
 * Created by LvWind on 16/6/27.
 */
public class Disconnect extends Activity implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
    private IShadowsocksService mService;
    private ServiceConnection mConnection = new ServiceConnection() {


        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            IShadowsocksService sss = IShadowsocksService.Stub.asInterface(service);
            mService = sss;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, ShadowsocksVpnService.class);
        intent.setAction(Constants.Action.SERVICE);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        showDisconnectDialog();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);
    }

    private void showDisconnectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_cancel);
        builder.setMessage(R.string.cancel_connection_query);
        builder.setNegativeButton(android.R.string.no, this);
        builder.setPositiveButton(android.R.string.yes, this);
        builder.setOnCancelListener(this);

        builder.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (mService != null)
                try {
                    mService.stop();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
        }
        finish();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }

}

