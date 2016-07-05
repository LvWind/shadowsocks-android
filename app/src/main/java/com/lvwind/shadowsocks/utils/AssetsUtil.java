package com.lvwind.shadowsocks.utils;

import android.content.res.AssetManager;
import android.util.Log;
import com.lvwind.shadowsocks.App;
import com.lvwind.shadowsocks.Constants;
import com.lvwind.shadowsocks.System;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by LvWind on 16/6/27.
 */
public class AssetsUtil {
    public static void prepareAbi() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                copyAssets(System.getABI());
                String ab = "chmod 755 " + Constants.Path.BASE + "/pdnsd" + ";";
                ab += "chmod 755 " + Constants.Path.BASE + "/redsocks" + ";";
                ab += "chmod 755 " + Constants.Path.BASE + "/ss-local" + ";";
                ab += "chmod 755 " + Constants.Path.BASE + "/ss-tunnel" + ";";
                ab += "chmod 755 " + Constants.Path.BASE + "/tun2socks";
                System.exec(ab);

            }
        }.start();
    }

    public static void copyAssets(String path) {
        AssetManager assetManager = App.getAppContext().getAssets();
        String[] files = null;
        try {
            files = assetManager.list(path);
        } catch (IOException e) {
            Log.e("ss-error", e.getMessage());
        }
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                InputStream in = null;
                OutputStream out = null;
                try {
                    if (path.length() > 0) {
                        in = assetManager.open(path + "/" + files[i]);
                    } else {
                        in = assetManager.open(files[i]);
                    }
                    out = new FileOutputStream(Constants.Path.BASE + "/" + files[i]);
                    copyFile(in, out);
                    in.close();
                    in = null;
                    out.flush();
                    out.close();
                    out = null;
                } catch (Exception e) {
                    Log.w("ss-error", e.getMessage());
                }
            }
        }
    }

    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte buffer[] = new byte[1024];
        int read = 0;
        while (true) {
            read = in.read(buffer);
            if (read != -1)
                out.write(buffer, 0, read);
            else
                break;
        }
    }
}
