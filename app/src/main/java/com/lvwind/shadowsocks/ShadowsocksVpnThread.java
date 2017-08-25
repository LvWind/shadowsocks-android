package com.lvwind.shadowsocks;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.*;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by LvWind on 16/6/27.
 */
public class ShadowsocksVpnThread extends Thread {
    String TAG = ShadowsocksVpnThread.class.getSimpleName();
    String PATH = "";

    volatile boolean isRunning = true;
    LocalServerSocket serverSocket = null;
    ShadowsocksVpnService vpnService;

    ShadowsocksVpnThread(ShadowsocksVpnService vpnService) {
        this.vpnService = vpnService;
        PATH = Constants.Path.BASE + "/protect_path";
    }

    @Override
    public void run() {
        try {
            new File(PATH).delete();
        } catch (Exception e) {
            // ignore
        }

        try {
            LocalSocket localSocket = new LocalSocket();
            localSocket.bind(new LocalSocketAddress(PATH, LocalSocketAddress.Namespace.FILESYSTEM));
            serverSocket = new LocalServerSocket(localSocket.getFileDescriptor());
        } catch (IOException e) {
            Log.e(TAG, "unable to bind", e);
            return;
        }

        ExecutorService pool = Executors.newFixedThreadPool(4);

        while (isRunning) {
            try {
                final LocalSocket socket = serverSocket.accept();
                //Log.d(TAG, "accept");
                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            InputStream input = socket.getInputStream();
                            OutputStream output = socket.getOutputStream();

                            input.read();

                            FileDescriptor[] fds = socket.getAncillaryFileDescriptors();

                            if (fds != null && fds.length != 0) {
                                Method getInt = FileDescriptor.class.getDeclaredMethod("getInt$");
                                int fd = (int) getInt.invoke(fds[0]);
                                boolean ret = vpnService.protect(fd);
                                //Log.d(TAG, "protect");

                                // Trick to close file descriptor
                                System.jniclose(fd);

                                if (ret) {
                                    output.write(0);
                                } else {
                                    output.write(1);
                                }
                            }

                            input.close();
                            output.close();

                        } catch (Exception e) {
                            Log.e(TAG, "Error when protect socket" + e.getMessage());
                        }

                        // close socket
                        try {
                            socket.close();
                        } catch (Exception e) {
                            // ignore
                        }

                    }
                });
            } catch (IOException | NullPointerException e) {
                Log.e(TAG, "Error when accept socket", e);
                return;
            }
        }
    }

    private void closeServerSocket() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (Exception e) {
                // ignore
            }
            serverSocket = null;
        }
    }

    void stopThread() {
        isRunning = false;
        closeServerSocket();
    }
}

