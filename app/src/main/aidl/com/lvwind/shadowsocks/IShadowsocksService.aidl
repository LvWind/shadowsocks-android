// IShadowsocksService.aidl
package com.lvwind.shadowsocks;
import com.lvwind.shadowsocks.database.SsConfig;
import com.lvwind.shadowsocks.IShadowsocksCallback;


// Declare any non-default types here with import statements

interface IShadowsocksService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
	int getState();

	oneway void registerCallback(IShadowsocksCallback cb);
	oneway void unregisterCallback();

	oneway void start(in SsConfig config);
	oneway void stop();
}
