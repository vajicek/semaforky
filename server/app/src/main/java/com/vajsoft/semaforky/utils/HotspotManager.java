package com.vajsoft.semaforky.utils;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;

import com.vajsoft.semaforky.R;
import com.vajsoft.semaforky.activities.SettingsActivity;
import com.vajsoft.semaforky.data.Settings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class HotspotManager {
    private static final Logger LOGGER = Logger.getLogger(SettingsActivity.class.getName());

    private Settings settings;
    private Context context;
    private WifiManager wifiManager;
    private WifiManager.LocalOnlyHotspotReservation reservation;

    public class HotspotManagerException extends Exception {
        public HotspotManagerException(String message) {
            super(message);
        }
    }

    public HotspotManager(Context context, Settings settings) {
        this.context = context;
        this.settings = settings;
        this.wifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
    }

    /**
     * Enable or disable wifi AP.
     */
    public void setWifiState(boolean state, Consumer<String> onFail) throws HotspotManagerException {
        if (isApOn() == state) {
            return;
        }
        configApState(state, onFail);
    }

    /**
     * Check whether wifi hotspot on or off.
     */
    public boolean isApOn() {
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
            if (!wifiConfiguration.SSID.equals(settings.getEssid()) || !wifiConfiguration.preSharedKey.equals(settings.getEssid())) {
                return false;
            }
            return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private WifiConfiguration setupWifiConfiguration(WifiConfiguration wifiConfiguration) {
        wifiConfiguration.SSID = settings.getEssid();
        wifiConfiguration.preSharedKey = settings.getPassword();
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        return wifiConfiguration;
    }

    private void configApState(boolean enable, Consumer<String> onFail) throws HotspotManagerException {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                onFail.accept(String.format(this.context.getResources().getString(R.string.wifiHotspotPostOreoWarning),
                        settings.getEssid(), settings.getPassword()));
                //setupWifiOreo(enable, onFail);
            } else {
                setupWifi(enable);
            }
        } catch (NoSuchMethodException e) {
            throw new HotspotManagerException("Unable to set Wifi AP");
        } catch (Exception e) {
            e.printStackTrace();
            throw new HotspotManagerException(e.getMessage());
        }
    }

    private void setupWifi(boolean enable) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        WifiConfiguration wifiConfiguration = enable ? setupWifiConfiguration(new WifiConfiguration()) : null;
        wifiManager.getClass()
                .getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class)
                .invoke(wifiManager, wifiConfiguration, enable);
    }

    // TODO(vajicek): experimntal code, remove if not working
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setupWifiOreo(boolean enable, Consumer<String> onFail) {
        if (enable) {
            wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
                @Override
                public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation_) {
                    reservation = reservation_;
                    setupWifiConfiguration(reservation.getWifiConfiguration());
                    super.onStarted(reservation);
                }

                @Override
                public void onFailed(int reason) {
                    super.onFailed(reason);
                    onFail.accept(String.format("Failed to start local only hotspot. reason=%d", reason));
                }
            }, new Handler());
        } else {
            if (reservation != null) {
                reservation.close();
                reservation = null;
            }
        }
    }
}
