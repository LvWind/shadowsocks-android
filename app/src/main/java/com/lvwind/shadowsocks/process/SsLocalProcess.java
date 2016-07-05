package com.lvwind.shadowsocks.process;

import android.content.Context;
import android.util.Log;
import com.lvwind.shadowsocks.Constants;
import com.lvwind.shadowsocks.database.SsConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.*;
import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * Created by LvWind on 16/6/27.
 */
public class SsLocalProcess extends Process {

    private static final String LOGTAG = "SsLocalProcess";
    private static final Map<Context, SsLocalProcess> sInstances = new HashMap<Context, SsLocalProcess>();

    private Thread ssThread = null;

    private Process ssProcess = null;

    private Boolean isDestroyed = false;
    SsConfig mConfig;

    private SsLocalProcess(Context context, SsConfig config) {
        mConfig = config;
    }

    public static synchronized SsLocalProcess createSsLocal(Context context, SsConfig config) {
        if (context == null) {
            Log.e(LOGTAG, "SsLocalProcess.createSsLocal got a null context object!");
            return null;
        }
        synchronized (sInstances) {
            final Context appContext = context.getApplicationContext();
            SsLocalProcess instance;
            if (!sInstances.containsKey(appContext)) {
                instance = new SsLocalProcess(appContext, config);
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
                        Constants.Path.BASE + "/ss-local", "-V", "-u",
                        "-b", "127.0.0.1",
                        "-t", "600",
                        "-c", Constants.Path.BASE + "/ss-local-vpn.conf",
                        "-P", Constants.Path.BASE
                };


                List<String> list = new ArrayList<>(Arrays.asList(cmd));

                if (mConfig.isAuth())
                    list.add("-A");
                //Add acl
                if (mConfig.route != Constants.Route.ALL) {
                    list.add("--acl");
                    list.add(Constants.Path.BASE + "/acl.list");
                }
                cmd = list.toArray(new String[0]);

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
