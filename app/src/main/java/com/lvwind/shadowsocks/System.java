package com.lvwind.shadowsocks;

/**
 * Created by LvWind on 16/6/27.
 */
public class System {
    static {
        java.lang.System.loadLibrary("system");
    }

    public static native int exec(String cmd);
    public static native String getABI();

    public static native int sendfd(int fd, String path);

    public static native void jniclose(int fd);
}
