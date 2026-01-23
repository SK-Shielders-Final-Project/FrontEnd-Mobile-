package com.mobility.hack.security;

public class SecurityEngine {
    static {
        System.loadLibrary("mobile");
    }

    public native String getApiKey();
    public native boolean checkIntegrity();
    public native boolean isKernelSuDetected();
    public native void initAntiDebug();
}