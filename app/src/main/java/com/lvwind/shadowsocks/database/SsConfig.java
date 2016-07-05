package com.lvwind.shadowsocks.database;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

import java.util.Date;
import java.util.Locale;

/**
 * Created by LvWind on 16/6/27.
 */
public class SsConfig implements Parcelable {
    @DatabaseField(id = true)
    private int profileId;
    @DatabaseField
    public String name = "default";
    @DatabaseField
    public String host = "";
    @DatabaseField
    public String password = "";
    @DatabaseField
    public String method = "aes-256-cfb";
    @DatabaseField
    public int remotePort = 12345;
    @DatabaseField
    public int localPort = 1080;
    @DatabaseField
    public String route = "all";
    @DatabaseField
    public Boolean proxyApps = false;
    @DatabaseField
    public Boolean bypass = false;
    @DatabaseField
    public Boolean udpdns = false;
    @DatabaseField
    public Boolean auth = false;
    @DatabaseField
    public Boolean ipv6 = false;
    @DatabaseField(dataType = DataType.LONG_STRING)
    public String individual = "";
    @DatabaseField
    public Long tx = 0L;
    @DatabaseField
    public Long rx = 0L;
    @DatabaseField
    public Date date = new Date();
    @DatabaseField
    public Long userOrder = null;

    public SsConfig() {

    }

    public SsConfig(String profileName, String address, String password, String encMethod, int remotePort) {
        this.name = profileName;
        this.host = address;
        this.password = password;
        this.method = encMethod;
        this.remotePort = remotePort;
    }

    public SsConfig(int profileId, String name, String host, String password, String method, int remotePort,
                    int localPort, String route, Boolean proxyApps, Boolean bypass, Boolean udpdns,
                    Boolean auth, Boolean ipv6, String individual, Long tx, Long rx, Date date, Long userOrder) {
        this.profileId = profileId;
        this.name = name;
        this.host = host;
        this.password = password;
        this.method = method;
        this.remotePort = remotePort;
        this.localPort = localPort;
        this.route = route;
        this.proxyApps = proxyApps;
        this.bypass = bypass;
        this.udpdns = udpdns;
        this.auth = auth;
        this.ipv6 = ipv6;
        this.individual = individual;
        this.tx = tx;
        this.rx = rx;
        this.date = date;
        this.userOrder = userOrder;
    }

    @Override
    public String toString() {
        return "ss://" + Base64.encodeToString("%s%s:%s@%s:%d".format(Locale.ENGLISH, method, "", password, host, remotePort).getBytes(), Base64.NO_PADDING | Base64.NO_WRAP);
    }

    public int getProfileId() {
        return profileId;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public String getPassword() {
        return password;
    }

    public String getMethod() {
        return method;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public int getLocalPort() {
        return localPort;
    }

    public String getRoute() {
        return route;
    }

    public Boolean isProxyApps() {
        return proxyApps;
    }

    public Boolean isBypass() {
        return bypass;
    }

    public Boolean isUdpdns() {
        return udpdns;
    }

    public Boolean isAuth() {
        return auth;
    }

    public Boolean isIpv6() {
        return ipv6;
    }

    public String getIndividual() {
        return individual;
    }

    public Long getTx() {
        return tx;
    }

    public Long getRx() {
        return rx;
    }

    public Date getDate() {
        return date;
    }

    public Long getUserOrder() {
        return userOrder;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.profileId);
        dest.writeString(this.name);
        dest.writeString(this.host);
        dest.writeString(this.password);
        dest.writeString(this.method);
        dest.writeInt(this.remotePort);
        dest.writeInt(this.localPort);
        dest.writeString(this.route);
        dest.writeValue(this.proxyApps);
        dest.writeValue(this.bypass);
        dest.writeValue(this.udpdns);
        dest.writeValue(this.auth);
        dest.writeValue(this.ipv6);
        dest.writeString(this.individual);
        dest.writeValue(this.tx);
        dest.writeValue(this.rx);
        dest.writeLong(this.date != null ? this.date.getTime() : -1);
        dest.writeValue(this.userOrder);
    }

    protected SsConfig(Parcel in) {
        this.profileId = in.readInt();
        this.name = in.readString();
        this.host = in.readString();
        this.password = in.readString();
        this.method = in.readString();
        this.remotePort = in.readInt();
        this.localPort = in.readInt();
        this.route = in.readString();
        this.proxyApps = (Boolean) in.readValue(Boolean.class.getClassLoader());
        this.bypass = (Boolean) in.readValue(Boolean.class.getClassLoader());
        this.udpdns = (Boolean) in.readValue(Boolean.class.getClassLoader());
        this.auth = (Boolean) in.readValue(Boolean.class.getClassLoader());
        this.ipv6 = (Boolean) in.readValue(Boolean.class.getClassLoader());
        this.individual = in.readString();
        this.tx = (Long) in.readValue(Long.class.getClassLoader());
        this.rx = (Long) in.readValue(Long.class.getClassLoader());
        long tmpDate = in.readLong();
        this.date = tmpDate == -1 ? null : new Date(tmpDate);
        this.userOrder = (Long) in.readValue(Long.class.getClassLoader());
    }

    public static final Creator<SsConfig> CREATOR = new Creator<SsConfig>() {
        @Override
        public SsConfig createFromParcel(Parcel source) {
            return new SsConfig(source);
        }

        @Override
        public SsConfig[] newArray(int size) {
            return new SsConfig[size];
        }
    };
}