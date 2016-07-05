package com.lvwind.shadowsocks.process;

import android.content.Context;
import android.util.Log;
import com.lvwind.shadowsocks.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * Created by LvWind on 16/6/27.
 */
public class SsTunnelProcess extends Process {

    private static final String LOGTAG = "SsTunnelProcess";
    private static final Map<Context, SsTunnelProcess> sInstances = new HashMap<Context, SsTunnelProcess>();

    private Thread ssThread = null;

    private Process ssProcess = null;

    private Boolean isDestroyed = false;

    private SsTunnelProcess(Context context) {

    }

    public static synchronized SsTunnelProcess createSsTunnel(Context context) {
        if (context == null) {
            Log.e(LOGTAG, "SsTunnelProcess.createSsTunnel got a null context object!");
            return null;
        }
        synchronized (sInstances) {
            final Context appContext = context.getApplicationContext();
            SsTunnelProcess instance;
            if (!sInstances.containsKey(appContext)) {
                instance = new SsTunnelProcess(appContext);
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


        ssThread = new Thread() {
            @Override
            public void run() {
                String[] cmd = {
                        Constants.Path.BASE + "/ss-tunnel"
                        , "-V"
                        , "-u"
                        , "-t", "10"
                        , "-b", "127.0.0.1"
                        , "-l", "8163"
                        , "-L", "8.8.8.8:53"
                        , "-P", Constants.Path.BASE
                        , "-c", Constants.Path.BASE + "/ss-tunnel-vpn.conf"};

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

        ssThread.start();
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        isDestroyed = true;
        ssThread.interrupt();
        if (ssProcess != null) {
            ssProcess.destroy();
        }
        try {
            ssThread.join();
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
        ssThread.join();
        return 0;
    }
}

