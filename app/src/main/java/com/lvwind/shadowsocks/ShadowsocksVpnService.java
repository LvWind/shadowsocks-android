package com.lvwind.shadowsocks;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.lvwind.shadowsocks.database.SsConfig;
import com.lvwind.shadowsocks.process.PdnsdProcess;
import com.lvwind.shadowsocks.process.SsLocalProcess;
import com.lvwind.shadowsocks.process.SsTunnelProcess;
import com.lvwind.shadowsocks.process.Tun2socksProcess;
import com.lvwind.shadowsocks.ui.activity.Disconnect;
import com.lvwind.shadowsocks.ui.activity.MainActivity;
import com.lvwind.shadowsocks.utils.ConfigUtils;
import com.lvwind.shadowsocks.utils.NetworkUtil;
import org.xbill.DNS.*;

import java.io.*;
import java.net.InetAddress;
import java.util.*;

/**
 * Created by LvWind on 16/6/27.
 */
public class ShadowsocksVpnService extends BaseService {
    private static final int SHADOWSOCKS_STATUS = 10001;
    private static final String TAG = ShadowsocksVpnService.class.getSimpleName();

    int VPN_MTU = 1500;
    String PRIVATE_VLAN = "26.26.26.%s";
    String PRIVATE_VLAN6 = "fdfe:dcba:9876::%s";
    ParcelFileDescriptor conn = null;
    BroadcastReceiver receiver = null;
    ShadowsocksVpnThread vpnThread;
    SsConfig config = null;

    SsLocalProcess sslocalProcess;
    SsTunnelProcess sstunnelProcess;
    PdnsdProcess pdnsdProcess;
    Tun2socksProcess tun2socksProcess;

    public void startShadowsocksDaemon() {
        //ACL(Access Control List) bypass
        if (config.getRoute() != Constants.Route.ALL) {
            String[] acl = new String[0];
            if (config.getRoute() == Constants.Route.BYPASS_LAN)
                acl = getResources().getStringArray(R.array.private_route);
            if (config.getRoute() == Constants.Route.BYPASS_CHN)
                acl = getResources().getStringArray(R.array.chn_route);
            if (config.getRoute() == Constants.Route.BYPASS_LAN_CHN)
                acl = getResources().getStringArray(R.array.chn_route);
            PrintWriter printWriter = ConfigUtils.printToFile(new File(Constants.Path.BASE + "/acl.list"));
            for (int i = 0; i < acl.length; i++)
                printWriter.println(acl[i]);
            printWriter.close();
        }


        //read config and write to file
        String conf = String.format(Locale.ENGLISH, ConfigUtils.SHADOWSOCKS, config.host, config.remotePort, config.localPort,
                config.password, config.method, 10);
        PrintWriter printWriter = ConfigUtils.printToFile(new File(Constants.Path.BASE + "/ss-local-vpn.conf"));
        printWriter.println(conf);
        printWriter.close();

        sslocalProcess = SsLocalProcess.createSsLocal(getContext(), config);
        if (sslocalProcess != null) {
            sslocalProcess.start();
        }
    }

    public void startDnsTunnel() {
        //读取配置 并写入文件
        String conf = String.format(Locale.ENGLISH, ConfigUtils
                        .SHADOWSOCKS, config.host, config.remotePort, 8163,
                config.password, config.method, 10);

        PrintWriter printWriter = ConfigUtils.printToFile(new File(Constants.Path.BASE + "/ss-tunnel-vpn.conf"));
        printWriter.println(conf);
        printWriter.close();

        sstunnelProcess = SsTunnelProcess.createSsTunnel(getContext());
        if (sstunnelProcess != null) {
            sstunnelProcess.start();
        }
        Log.d(TAG, "start DnsTun");
    }

    public void startDnsDaemon() {
        pdnsdProcess = PdnsdProcess.createPdnsd(getContext(), config);
        if (pdnsdProcess != null) {
            pdnsdProcess.start();
        }
        Log.d(TAG, "start DnsDaemon");
    }

    int startVpn() throws PackageManager.NameNotFoundException {
        changeState(Constants.State.CONNECTING);
        showNotification(Constants.State.CONNECTING);

        Builder builder = new Builder();
        String str = String.format(Locale.ENGLISH, PRIVATE_VLAN, "1");
        builder
                .setSession(config.name)
                .setMtu(VPN_MTU)
                .addAddress(str, 24)
                .addDnsServer("8.8.8.8");

        if (config.isIpv6()) {
            builder.addAddress(String.format(Locale.ENGLISH, PRIVATE_VLAN6, "1"), 126);
            builder.addRoute("::", 0);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        }

        if (config.route == Constants.Route.ALL || config.route == Constants.Route.BYPASS_CHN) {
            builder.addRoute("0.0.0.0", 0);
        } else {
            String list[] = getResources().getStringArray(R.array.bypass_private_route);
            for (int i = 0; i < list.length; i++) {
                String[] addr = list[i].split("/");
                builder.addRoute(addr[0], Integer.valueOf(addr[1]));
            }
        }
        builder.addRoute("8.8.0.0", 16);


        try {
            conn = builder.establish();
        } catch (IllegalStateException e) {
            changeState(Constants.State.STOPPED, e.getMessage());
            showNotification(Constants.State.STOPPED);
            conn = null;

        }

        if (conn == null) {
            stopRunner();
            return -1;
        }

        int fd = conn.getFd();

        String cmd = String.format(Locale.ENGLISH,
                Constants.Path.BASE +
                        "/tun2socks --netif-ipaddr %s "
                        + "--netif-netmask 255.255.255.0 "
                        + "--socks-server-addr 127.0.0.1:%d "
                        + "--tunfd %d "
                        + "--tunmtu %d "
                        + "--loglevel 5 "
                        + "--sock-path %s "
                        + "--logger stdout",
                String.format(Locale.ENGLISH, PRIVATE_VLAN, "2"), config.localPort, fd, VPN_MTU, Constants.Path.BASE + "/sock_path");
        if (config.isIpv6())
            cmd += " --netif-ip6addr " + String.format(Locale.ENGLISH, PRIVATE_VLAN6 , "2");

        if (config.isUdpdns())
            cmd += " --enable-udprelay";
        else
            cmd += String.format(Locale.ENGLISH, " --dnsgw %s:8153", String.format(Locale.ENGLISH, PRIVATE_VLAN, "1"));

        tun2socksProcess = Tun2socksProcess.createTun2socks(getContext());
        if (tun2socksProcess != null) {
            tun2socksProcess.start(cmd);
        }
        return fd;
    }

    boolean sendFd(int fd) {
        if (fd != -1) {
            int tries = 1;
            while (tries < 5) {
                try {
                    Thread.sleep(1000 * tries);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (System.sendfd(fd, Constants.Path.BASE + "/sock_path") != -1) {
                    return true;
                }
                tries += 1;
            }
        }
        return false;
    }

    /**
     * Called when the activity is first created.
     */

    @Override
    public IBinder onBind(Intent intent) {
            Log.d(TAG, "onBind");
        String action = intent.getAction();
        if (VpnService.SERVICE_INTERFACE.equals(action)) {
            return super.onBind(intent);
        } else if (Constants.Action.SERVICE.equals(action)) {
            Log.v(TAG, "getBinder");
            return binder;
        }
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public void onRevoke() {
        stopRunner();
    }

    public void killProcesses() {
        if (sslocalProcess != null) {
            sslocalProcess.destroy();
            sslocalProcess = null;
        }
        if (sstunnelProcess != null) {
            sstunnelProcess.destroy();
            sstunnelProcess = null;
        }
        if (tun2socksProcess != null) {
            tun2socksProcess.destroy();
            tun2socksProcess = null;
        }
        if (pdnsdProcess != null) {
            pdnsdProcess.destroy();
            pdnsdProcess = null;
        }
    }


    @Override
    public void stopBackgroundService() {
        stopSelf();
    }

    @Override
    public void startRunner(SsConfig c) {
        config = c;
        vpnThread = new ShadowsocksVpnThread(this);
        vpnThread.start();
        if (VpnService.prepare(this) != null) {

        }
        changeState(Constants.State.CONNECTING);
        showNotification(Constants.State.CONNECTING);
        if (config != null) {
            // reset the context
            killProcesses();
            // Resolve the server address
            boolean resolved = false;
            if (!NetworkUtil.isLiteralIpAddress(config.host)) {
                if (resolve(config.host, Type.A).isEmpty()) {
                    if (!resolve(config.host).isEmpty()) {
                        resolve(config.host);
                        resolved = true;
                    }
                } else {
                    config.host = resolve(config.host, Type.A);
                    resolved = true;
                }
            } else {
                resolved = true;
            }

            Log.d(TAG, "resolved:" + resolved);
            if (resolved && handleConnection()) {
                changeState(Constants.State.CONNECTED);
                showNotification(Constants.State.CONNECTED);
            } else {
                changeState(Constants.State.STOPPED);
                showNotification(Constants.State.STOPPED);
                stopRunner();
            }
        }
    }

    public boolean handleConnection() {
        Log.d(TAG, "handleConnection");
        startShadowsocksDaemon();
        startDnsDaemon();
        startDnsTunnel();
        try {
            int fd = startVpn();
            if (!sendFd(fd)) {
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.getStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public void stopRunner() {
        if (vpnThread != null) {
            vpnThread.stopThread();
            vpnThread = null;
        }
        // change the state
        changeState(Constants.State.STOPPING);
        showNotification(Constants.State.STOPPING);
        // reset VPN
        killProcesses();
        // close connections
        if (conn != null) {
            try {
                conn.close();
                conn = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // stop the service if no callback registered
        if (getCallbackCount() == 0) {
            stopSelf();
        }
        // clean up the context
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        // change the state
        changeState(Constants.State.STOPPED);
        showNotification(Constants.State.STOPPED);
    }


    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public Context getContext() {
        return getBaseContext();
    }

    String resolve(String host, int addrType) {
        try {
            Lookup lookup = new Lookup(host, addrType);
            SimpleResolver resolver = new SimpleResolver("8.8.8.8");
            resolver.setTimeout(5);
            lookup.setResolver(resolver);
            Record[] result = lookup.run();
            if (result == null)
                return null;
            for (Record r : result) {
                switch (addrType) {
                    case Type.A:
                        return ((ARecord) r).getAddress().getHostAddress();
                    case Type.AAAA:
                        return ((AAAARecord) r).getAddress().getHostAddress();
                }
            }
        } catch (java.net.UnknownHostException | TextParseException e) {
            e.getStackTrace();
        }
        return null;
    }

    String resolve(String host) {
        try {
            InetAddress addr = InetAddress.getByName(host);
            return addr.getHostAddress();
        } catch (java.net.UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void showNotification(Constants.State state) {
        Intent intent = new Intent(getBaseContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        android.app.Notification.Builder nBuilder = new Notification.Builder(this);
        nBuilder.setContentTitle(getString(R.string.app_name));
        nBuilder.setOnlyAlertOnce(true);
        nBuilder.setOngoing(true);
        nBuilder.setContentIntent(pendingIntent);
        nBuilder.setSmallIcon(R.drawable.ic_stat);

        if (state == Constants.State.CONNECTED) {
            nBuilder.setContentText(getString(R.string.state_connected));
        }
        if (state == Constants.State.CONNECTING) {
            nBuilder.setContentText(getString(R.string.state_connecting));
        }
        if (state == Constants.State.STOPPED) {
            nBuilder.setContentText(getString(R.string.state_disconnected));
            mNotificationManager.cancel(SHADOWSOCKS_STATUS);
            return;
        }
        if (state == Constants.State.STOPPING) {
            nBuilder.setContentText(getString(R.string.state_connecting));
        }
        if (state == Constants.State.INIT) {
            nBuilder.setContentText(getString(R.string.state_noprocess));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Intent disconnectVPN = new Intent(this, Disconnect.class);
            disconnectVPN.setAction(Constants.Action.CLOSE);
            PendingIntent disconnectPendingIntent = PendingIntent.getActivity(this, 0, disconnectVPN, 0);

            nBuilder.addAction(android.R.drawable.ic_menu_close_clear_cancel,
                    getString(R.string.cancel_connection), disconnectPendingIntent);
        }


        @SuppressWarnings("deprecation")
        Notification notification = nBuilder.getNotification();
        mNotificationManager.notify(SHADOWSOCKS_STATUS, notification);
    }

}

