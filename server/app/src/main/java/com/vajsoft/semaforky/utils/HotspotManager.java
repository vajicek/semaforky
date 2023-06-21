package com.vajsoft.semaforky.utils;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;

import com.android.dx.stock.ProxyBuilder;
import com.vajsoft.semaforky.data.Settings;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class HotspotManager {

    private final Settings settings;
    private final Context context;
    private final WifiManager wifiManager;

    public static class HotspotManagerException extends Exception {
        public HotspotManagerException(final String message) {
            super(message);
        }

        public HotspotManagerException(final String message, Throwable cause) {
            super(message, cause);
        }
    }

    public interface OnHotspotControlCallbacks {
        void failed(final String message);

        void started();
    }

    public HotspotManager(final Context context, final Settings settings) {
        this.context = context;
        this.settings = settings;
        this.wifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
    }

    /**
     * Enable or disable wifi AP.
     */
    public void setWifiState(final boolean state, final OnHotspotControlCallbacks controlCallbacks) throws HotspotManagerException {
        if (isApOn() == state) {
            return;
        }
        configApState(state, controlCallbacks);
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
            return wifiConfiguration.SSID.equals(settings.getEssid()) && wifiConfiguration.preSharedKey.equals(settings.getEssid());
        } catch (Throwable ignored) {
        }
        return false;
    }

    private WifiConfiguration setupWifiConfiguration(final WifiConfiguration wifiConfiguration) {
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

    private void configApState(final boolean enable, final OnHotspotControlCallbacks controlCallbacks) throws HotspotManagerException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupWifiOreo(enable, controlCallbacks);
        } else {
            setupWifi(enable);
        }
    }

    private void setupWifi(final boolean enable) throws HotspotManagerException {
        WifiConfiguration wifiConfiguration = enable ? setupWifiConfiguration(new WifiConfiguration()) : null;
        try {
            wifiManager.getClass()
                    .getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class)
                    .invoke(wifiManager, wifiConfiguration, enable);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new HotspotManagerException("Failed while getting access to Wifi AP configuration interface.");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setupWifiOreo(final boolean enable, final OnHotspotControlCallbacks controlCallbacks) throws HotspotManagerException {
        OreoTethering oreoTethering = new OreoTethering(context);
        if (enable) {
            oreoTethering.startTethering(settings.getEssid(), settings.getPassword(), controlCallbacks);
        } else {
            oreoTethering.stopTethering();
        }
    }

    /**
     * This is alternative way to control Wifi AP on Android >= 8.0.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    class OreoTethering {
        ConnectivityManager connectivityManager;
        Method startTetheringMethod;
        Method stopTetheringMethod;

        public OreoTethering(final Context context) throws HotspotManagerException {
            connectivityManager = context.getSystemService(ConnectivityManager.class);
            try {
                startTetheringMethod = connectivityManager.getClass().getDeclaredMethod("startTethering", int.class, boolean.class, OnStartTetheringCallbackClass(), Handler.class);
                stopTetheringMethod = connectivityManager.getClass().getDeclaredMethod("stopTethering", int.class);
            } catch (NoSuchMethodException e) {
                throw new HotspotManagerException("Failed while obtaining tethering control interface.", e);
            }
        }

        public void startTethering(final String essid, final String password, final OnHotspotControlCallbacks controlCallbacks) throws HotspotManagerException {
            configureHotspot(essid, password);
            try {
                startTetheringMethod.invoke(connectivityManager, ConnectivityManager.TYPE_MOBILE, false, getOnStartTetheringCallbackProxy(controlCallbacks), null);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new HotspotManagerException("Failed while starting tethering.", e);
            }
        }

        public void stopTethering() throws HotspotManagerException {
            try {
                stopTetheringMethod.invoke(connectivityManager, ConnectivityManager.TYPE_MOBILE);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new HotspotManagerException("Failed while stopping tethering.", e);
            }
        }

        private Object getOnStartTetheringCallbackProxy(final OnHotspotControlCallbacks controlCallbacks) throws HotspotManagerException {
            File outputDir = context.getCodeCacheDir();
            try {
                return ProxyBuilder.forClass(OnStartTetheringCallbackClass()).dexCache(outputDir).handler(new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        switch (method.getName()) {
                            case "onTetheringStarted":
                                controlCallbacks.started();
                                break;
                            case "onTetheringFailed":
                                controlCallbacks.failed("Failed while trying to enable tethering.");
                                break;
                            default:
                                ProxyBuilder.callSuper(proxy, method, args);
                        }
                        return null;
                    }
                }).build();
            } catch (IOException e) {
                throw new HotspotManagerException(e.getMessage());
            }
        }

        private void configureHotspot(final String name, final String password) throws HotspotManagerException {
            WifiConfiguration apConfig = new WifiConfiguration();
            apConfig.SSID = name;
            apConfig.preSharedKey = password;
            apConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            try {
                Method setConfigMethod = wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
                if (!(boolean) setConfigMethod.invoke(wifiManager, apConfig)) {
                    throw new HotspotManagerException("Failed while setting Wifi AP configuration.");
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new HotspotManagerException("Failed while getting access to Wifi AP configuration interface.", e);
            }
        }

        private Class<?> OnStartTetheringCallbackClass() throws HotspotManagerException {
            try {
                return Class.forName("android.net.ConnectivityManager$OnStartTetheringCallback");
            } catch (ClassNotFoundException e) {
                throw new HotspotManagerException("Failed while getting ConnectivityManager OnStartTetheringCallback class.", e);
            }
        }
    }
}
