package com.lvwind.shadowsocks;

import android.content.Context;
import android.net.VpnService;
import android.os.Handler;
import android.os.RemoteException;
import com.lvwind.shadowsocks.database.SsConfig;

/**
 * Created by LvWind on 16/6/27.
 */
public abstract class BaseService extends VpnService {
    volatile private Constants.State state = Constants.State.INIT;
    private IShadowsocksCallback callback = null;
    IShadowsocksService.Stub binder = new IShadowsocksService.Stub() {

        public int getState() {
            return state.ordinal();
        }

        @Override
        public void registerCallback(IShadowsocksCallback cb) throws RemoteException {
            callback = cb;

        }

        @Override
        public void unregisterCallback() throws RemoteException {
            callback = null;
        }

        public void stop() {
            if (state != Constants.State.CONNECTING && state != Constants.State.STOPPING) {
                stopRunner();
            }
        }

        public void start(SsConfig config) {
            if (state != Constants.State.CONNECTING && state != Constants.State.STOPPING) {
                startRunner(config);
            }
        }
    };

    public abstract void stopBackgroundService();

    public abstract void startRunner(SsConfig config);

    public abstract void stopRunner();

    public abstract String getTag();

    public abstract Context getContext();

    public int getCallbackCount() {
        //        return callbackCount;
        return -1;
    }

    public Constants.State getState() {
        return state;
    }

    public void changeState(Constants.State s) {
        changeState(s, null);
    }

    protected void changeState(final Constants.State s, final String msg) {
        Handler handler = new Handler(getContext().getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                if (state != s) {
                    if (callback != null) {
                        try {
                            callback.onStatusChanged(s.ordinal());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    state = s;
                }
            }
        });

    }
}
