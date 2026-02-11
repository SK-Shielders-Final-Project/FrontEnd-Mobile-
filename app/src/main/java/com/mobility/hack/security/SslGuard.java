package com.mobility.hack.security;

import android.util.Log;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

public class SslGuard implements X509TrustManager {

    private static final String TAG = "SslGuard";
    private static volatile boolean isLibraryLoaded = false;

    static {
        try {
            System.loadLibrary("mobile");
            isLibraryLoaded = true;
            Log.i(TAG, "✅ Native Library loaded successfully.");
        } catch (UnsatisfiedLinkError e) {
            isLibraryLoaded = false;
            Log.e(TAG, "🚨 [FATAL] Failed to load native-lib! SSL Pinning cannot work.", e);
        }
    }

    // Native 함수 선언
    public native boolean verifyCert(byte[] certEncoded, boolean checkEnabled);

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain == null || chain.length == 0) {
            throw new CertificateException("X509Certificate chain is empty");
        }

        // ✅ 테스트에서는 “라이브러리 로드 실패 = 실패”가 훨씬 안전함
        if (!isLibraryLoaded) {
            throw new CertificateException("Native library not loaded - cannot verify pinning");
        }

        try {
            // ✅ 네가 말한대로 테스트용: 항상 검사
            boolean isCheckNeeded = true;

            // Leaf 인증서
            byte[] certBytes = chain[0].getEncoded();

            Log.w(TAG, "verifyCert() call / authType=" + authType
                    + " / leafSubject=" + chain[0].getSubjectDN());

            boolean isSafe = verifyCert(certBytes, isCheckNeeded);

            if (!isSafe) {
                throw new CertificateException("SSL Pinning Failed! Certificate mismatch.");
            }

        } catch (CertificateException ce) {
            Log.e(TAG, "🚨 Pinning blocked: " + ce.getMessage());
            throw ce;
        } catch (Exception e) {
            Log.e(TAG, "Error during cert verification", e);
            throw new CertificateException(e);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        // Client 인증서 기반 mTLS 안 쓰면 보통 여기 호출 안 됨
        // 그래도 명시적으로 “허용”하려면 빈 구현 가능
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
