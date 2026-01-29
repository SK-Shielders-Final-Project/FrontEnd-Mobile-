package com.mobility.hack.ride;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobility.hack.R;
import com.mobility.hack.network.ApiService;
import com.mobility.hack.network.PaymentRequest;
import com.mobility.hack.network.PaymentResponse;
import com.mobility.hack.network.RetrofitClient;
import com.mobility.hack.security.TokenManager;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentActivity extends AppCompatActivity {

    private static final String TAG = "PaymentActivity";
    private WebView webView;
    private ApiService apiService;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        webView = findViewById(R.id.webView);

        tokenManager = new TokenManager(this);
        apiService = RetrofitClient.getApiService(tokenManager);

        // 1. 웹뷰 설정
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        // 2. WebViewClient 설정 (URL 가로채기)
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                if ("localhost".equals(uri.getHost())) {
                    if ("/success".equals(uri.getPath())) {
                        String paymentKey = uri.getQueryParameter("paymentKey");
                        String orderId = uri.getQueryParameter("orderId");
                        String amount = uri.getQueryParameter("amount");
                        requestPaymentConfirmToServer(paymentKey, orderId, Long.parseLong(amount));
                    } else if ("/fail".equals(uri.getPath())) {
                        String message = uri.getQueryParameter("message");
                        Toast.makeText(PaymentActivity.this, "결제 실패: " + message, Toast.LENGTH_SHORT).show();
                    }
                    finish();
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
        });

        // 3. WebChromeClient 설정 (자바스크립트 오류 확인용)
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String logMessage = consoleMessage.message() + " -- From line "
                        + consoleMessage.lineNumber() + " of "
                        + consoleMessage.sourceId();
                Log.e("WebViewConsole", logMessage);

                // 오류 메시지를 화면에 토스트로 보여줌
                Toast.makeText(PaymentActivity.this, "WebConsole: " + consoleMessage.message(), Toast.LENGTH_LONG).show();
                return true;
            }
        });

        // 4. 결제 페이지 로드
        loadPaymentPage();
    }

    private void loadPaymentPage() {
        String clientKey;
        try {
            clientKey = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).metaData.getString("TOSS_CLIENT_KEY");
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this, "클라이언트 키를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        int amount = getIntent().getIntExtra(PurchaseTicketActivity.EXTRA_AMOUNT, 0);
        String orderName = getIntent().getStringExtra(PurchaseTicketActivity.EXTRA_ORDER_NAME);

        if (amount <= 0 || orderName == null) {
            Toast.makeText(this, "결제 정보가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String paymentUrl = "file:///android_asset/payment.html?clientKey=" + clientKey +
                "&amount=" + amount +
                "&orderName=" + Uri.encode(orderName);

        webView.loadUrl(paymentUrl);
    }

    private void requestPaymentConfirmToServer(String paymentKey, String orderId, Long amount) {
        long userId = tokenManager.fetchUserId();
        PaymentRequest paymentRequest = new PaymentRequest(paymentKey, orderId, amount, userId);
        apiService.confirmPayment(paymentRequest).enqueue(new Callback<PaymentResponse>() {
            @Override
            public void onResponse(Call<PaymentResponse> call, Response<PaymentResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PaymentActivity.this, "포인트가 충전되었습니다.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    try {
                        Log.e(TAG, "Payment failed: " + response.errorBody().string());
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Toast.makeText(PaymentActivity.this, "포인트 충전에 실패했습니다.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<PaymentResponse> call, Throwable t) {
                Log.e(TAG, "Payment API call failed", t);
                Toast.makeText(PaymentActivity.this, "서버 통신에 실패했습니다.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
