package com.lvwind.shadowsocks.process;

import android.content.Context;
import android.util.Log;
import com.lvwind.shadowsocks.Constants;
import com.lvwind.shadowsocks.database.SsConfig;

import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * Created by LvWind on 2016/12/22.
 */

public class KcptunProcess extends Process {

    private static final String LOGTAG = "KcptunProcess";
    private static final Map<Context, KcptunProcess> sInstances = new HashMap<Context, KcptunProcess>();
    Context mContext;
    SsConfig mConfig;

    private Thread thread = null;

    private Process ssProcess = null;

    private Boolean isDestroyed = false;

    private KcptunProcess(Context context, SsConfig config) {
        mContext = context;
        mConfig =config;
    }

    public static synchronized KcptunProcess createKcptun(Context context, SsConfig config) {
        if (context == null) {
            Log.e(LOGTAG, "KcptunProcess.createKcptun got a null context object!");
            return null;
        }
        synchronized (sInstances) {
            final Context appContext = context.getApplicationContext();
            KcptunProcess instance;
            if (!sInstances.containsKey(appContext)) {
                instance = new KcptunProcess(appContext, config);
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
                String[] cmd = {
                        Constants.Path.BASE + "/kcptun"
                        , "-r", mConfig.getHost() + ":" + "24680"
                        , "-l", "127.0.0.1:" + (mConfig.localPort + 90)
                        , "--path", Constants.Path.BASE + "/protect_path"
                        , "-mode"
                        , "fast2"
                };

                try {
                    isDestroyed = false;
                    while (!isDestroyed) {
                        Log.d(LOGTAG, "start process: " + Arrays.toString(cmd));

                        long startTime = java.lang.System.currentTimeMillis();
                        ssProcess = new ProcessBuilder(cmd).redirectErrorStream(true).start();

                        semaphore.release();

                        ssProcess.waitFor();
                        if (java.lang.System.currentTimeMillis() - startTime < 1000) {
                            Log.w(LOGTAG, "process exit too fast, stop guard");
                            isDestroyed = true;
                        }
                    }
                } catch (InterruptedException e) {
                    Log.i(LOGTAG, "thread interrupt, destroy process");
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
    public OutputStream getOutputStream() {
        return null;
    }

    @Override
    public InputStream getInputStream() {
        return null;
    }

    @Override
    public InputStream getErrorStream() {
        return null;
    }

    @Override
    public int waitFor() throws InterruptedException {
        thread.join();
        return 0;
    }

    @Override
    public int exitValue() {
        return 0;
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
}
