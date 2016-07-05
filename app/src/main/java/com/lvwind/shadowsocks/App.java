package com.lvwind.shadowsocks;

import android.app.Application;
import android.content.Context;

/**
 * Created by LvWind on 16/6/27.
 */
public class App extends Application {
    private static Context context;

    public static Context getAppContext() {
        return App.context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }
}
