// IShadowsocksCallback.aidl
package com.lvwind.shadowsocks;

// Declare any non-default types here with import statements

interface IShadowsocksCallback {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
	void onStatusChanged(int status);
}
