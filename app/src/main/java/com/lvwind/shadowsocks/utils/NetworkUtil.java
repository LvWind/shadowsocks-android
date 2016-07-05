package com.lvwind.shadowsocks.utils;

import java.lang.reflect.Method;
import java.net.InetAddress;

/**
 * Created by LvWind on 16/6/27.
 */
public class NetworkUtil {

    public static boolean isLiteralIpAddress(String address) {
        try {
            Method isNumericMethod = InetAddress.class.getMethod("isNumeric", String.class);
            return (Boolean) isNumericMethod.invoke(null, address);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
