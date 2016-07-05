package com.lvwind.shadowsocks.ui.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import com.lvwind.shadowsocks.*;
import com.lvwind.shadowsocks.database.SsConfig;
import com.lvwind.shadowsocks.utils.AssetsUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    Button button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(this);

        AssetsUtil.prepareAbi();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);

        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService();
    }

    @Override
    protected void onStop() {
        unbindService();
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SS_REQUEST_CODE_CONNECT:
                if (resultCode == Activity.RESULT_OK) {
                    startService(new Intent(this, ShadowsocksVpnService.class));
                    try {
                        SsConfig cp = new SsConfig("name", "addr", "passw", "aes-128-cfb", 12345);
                        sss.start(cp);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }

    }
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button:
                startSS();
                break;
        }
    }

    private IShadowsocksService sss = null;
    private SSVPNServiceCallBack ssCallback = null;
    public final int SS_REQUEST_CODE_CONNECT = 0;



    class SSVPNServiceCallBack extends IShadowsocksCallback.Stub {
        @Override
        public void onStatusChanged(final int status) throws RemoteException {
            try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (status == Constants.State.CONNECTED.ordinal()) {
                            }
                            if (status == Constants.State.STOPPED.ordinal()) {
                            }
                            if (status == Constants.State.STOPPING.ordinal()) {
                            }
                            if (status == Constants.State.CONNECTING.ordinal()) {
                            }
                            if (status == Constants.State.INIT.ordinal()) {
                            }
                        }
                    });
            } catch (NullPointerException e) {
                //ignore
            }
        }
    }

    private void bindService() {
        Intent intent = new Intent(this, ShadowsocksVpnService.class);
        intent.setAction(Constants.Action.SERVICE);
        bindService(intent, mSSConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindService() {
        unbindService(mSSConnection);
    }

    private void startSS() {
        Intent vpn = ShadowsocksVpnService.prepare(this);
        if (vpn != null) {
            startActivityForResult(vpn, SS_REQUEST_CODE_CONNECT);
        } else {
            onActivityResult(SS_REQUEST_CODE_CONNECT, Activity.RESULT_OK, null);
        }
    }

    private void stopSS() {
        try {
            if (sss.getState() == Constants.State.CONNECTED.ordinal() ||
                    sss.getState() == Constants.State.CONNECTING.ordinal()) {
                sss.stop();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private ServiceConnection mSSConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            sss = IShadowsocksService.Stub.asInterface(service);
            ssCallback = new SSVPNServiceCallBack();
            try {
                sss.registerCallback(ssCallback);
                ssCallback.onStatusChanged(sss.getState());
            } catch (RemoteException ignored) {
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (sss != null) {
                try {
                    sss.unregisterCallback();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            try {
                ssCallback.onStatusChanged(Constants.State.STOPPED.ordinal());
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            sss = null;
            ssCallback = null;
        }
    };


}
