package com.vajsoft.semaforky.utils;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import java.lang.reflect.Method;

public class HotspotManager {

    private Context context;

    public HotspotManager(Context context) {
        this.context = context;
    }

    /** Check whether wifi hotspot on or off. */
    public boolean isApOn(String ssid, String password) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        try {
            // test enabled
            Method isApEnabledMethod = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
            isApEnabledMethod.setAccessible(true);
            if (!(Boolean) isApEnabledMethod.invoke(wifiManager)) {
                return false;
            }

            // test configured
            Method getWifiApConfigurationMethod = wifiManager.getClass().getDeclaredMethod("getWifiApConfiguration");
            getWifiApConfigurationMethod.setAccessible(true);
            WifiConfiguration wifiConfiguration = (WifiConfiguration) getWifiApConfigurationMethod.invoke(wifiManager);
            if (!wifiConfiguration.SSID.equals(ssid) || !wifiConfiguration.preSharedKey.equals(ssid)) {
                return false;
            }

            return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    public void disableApState() throws HotspotManagerException {
        configApState(false, null, null);
    }

    /** Configure wifi hotspot. */
    public void configApState(boolean enable, String ssid, String password) throws HotspotManagerException {
        WifiManager wifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        WifiConfiguration wifiConfiguration = null;
        try {
            if (enable) {
                wifiConfiguration = new WifiConfiguration();
                wifiConfiguration.SSID = ssid;
                wifiConfiguration.preSharedKey = password;
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifiManager.setWifiEnabled(false);
            }
            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifiManager, wifiConfiguration, enable);
        } catch (Exception e) {
            e.printStackTrace();
            throw new HotspotManagerException();
        }
    }

    public class HotspotManagerException extends Exception {
    }
}
