package com.lvwind.shadowsocks.process;

import android.content.Context;
import android.util.Log;
import com.lvwind.shadowsocks.Constants;
import com.lvwind.shadowsocks.R;
import com.lvwind.shadowsocks.database.SsConfig;
import com.lvwind.shadowsocks.utils.ConfigUtils;

import java.io.*;
import java.lang.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * Created by LvWind on 16/6/27.
 */
public class PdnsdProcess extends Process {

    private static final String LOGTAG = "PdnsdProcess";
    private static final Map<Context, PdnsdProcess> sInstances = new HashMap<Context, PdnsdProcess>();
    Context mContext;
    SsConfig mConfig;

    private Thread thread = null;

    private Process ssProcess = null;

    private Boolean isDestroyed = false;

    private PdnsdProcess(Context context, SsConfig config) {
        mContext = context;
        mConfig =config;
    }

    public static synchronized PdnsdProcess createPdnsd(Context context, SsConfig config) {
        if (context == null) {
            Log.e(LOGTAG, "PdnsdProcess.createPdnsd got a null context object!");
            return null;
        }
        synchronized (sInstances) {
            final Context appContext = context.getApplicationContext();
            PdnsdProcess instance;
            if (!sInstances.containsKey(appContext)) {
                instance = new PdnsdProcess(appContext, config);
                sInstances.put(appContext, instance);
            } else {
                instance = sInstances.get(appContext);
            }
            return instance;
        }
    }

    public void start() {
        final Semaphore semaphore = new Semaphore(1);
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        thread = new Thread() {
            @Override
            public void run() {
                String ipv6;
                if (mConfig.isIpv6()) {
                    ipv6 = "";
                } else {
                    ipv6 = "reject = ::/0;";
                }
                String conf;
                if (mConfig.route == Constants.Route.BYPASS_CHN || mConfig.route == Constants.Route.BYPASS_LAN_CHN) {
                    String reject = mContext.getResources().getString(R.string.reject);
                    String blackList = mContext.getResources().getString(R.string.black_list);

                    conf = String.format(Locale.ENGLISH, ConfigUtils.PDNSD_DIRECT, "0.0.0.0", 8153
                            , reject, blackList, 8163 ,ipv6);
                } else {
                    conf = String.format(Locale.ENGLISH, ConfigUtils.PDNSD_LOCAL, "0.0.0.0", 8153, 8163, ipv6);
                }
                PrintWriter printWriter = ConfigUtils.printToFile(new File(Constants.Path.BASE + "/pdnsd-vpn.conf"));
                printWriter.println(conf);
                printWriter.close();

                String cmd = Constants.Path.BASE + "/pdnsd -c " + Constants.Path.BASE + "/pdnsd-vpn.conf";

                try {
                    isDestroyed = false;
                    while (!isDestroyed) {
                        Log.d(LOGTAG, "start process: " + cmd);

                        long startTime = java.lang.System.currentTimeMillis();
                        ssProcess = new ProcessBuilder(cmd.split(" ")).redirectErrorStream(true).start();

                        semaphore.release();

                        ssProcess.waitFor();
                        if (java.lang.System.currentTimeMillis() - startTime < 1000) {
                            Log.w(LOGTAG, "process exit too fast, stop guard ");
                            isDestroyed = true;
                        }
                    }
                } catch (InterruptedException e) {
                    Log.i(LOGTAG, "thread interrupt, destroy process: ");
                    ssProcess.destroy();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    semaphore.release();
                }
            }
        };

        thread.start();
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        isDestroyed = true;
        thread.interrupt();
        if (ssProcess != null) {
            ssProcess.destroy();
        }
        try {
            thread.join();
        } catch (InterruptedException e) {
            //Ignore
        }
    }

    @Override
    public int exitValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getErrorStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getInputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream getOutputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int waitFor() throws InterruptedException {
        thread.join();
        return 0;
    }
}
