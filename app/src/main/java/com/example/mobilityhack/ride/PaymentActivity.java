package com.example.mobilityhack.ride;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobilityhack.BuildConfig;
import com.example.mobilityhack.R;
import com.example.mobilityhack.network.ApiService;
import com.example.mobilityhack.network.RetrofitClient;
import com.example.mobilityhack.network.dto.PaymentConfirmRequest;
import com.example.mobilityhack.network.dto.PaymentConfirmResponse;
import com.example.mobilityhack.util.Constants;

import java.net.URISyntaxException;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentActivity extends AppCompatActivity {

    private static final String TAG = "PaymentActivity";
    public static final String EXTRA_AMOUNT = "amount";
    public static final String EXTRA_ORDER_NAME = "orderName";

    private WebView webView;
    private ApiService apiService;
    private boolean isPaymentScriptInjected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        WebView.setWebContentsDebuggingEnabled(true);

        webView = findViewById(R.id.web_view);
        apiService = RetrofitClient.getApiService(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        
        // User-Agent를 표준 모바일 안드로이드로 명시적 설정
        String mobileUserAgent = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Mobile Safari/537.36";
        settings.setUserAgentString(mobileUserAgent);

        Intent intent = getIntent();
        int amount = intent.getIntExtra(EXTRA_AMOUNT, 0);
        String orderName = intent.getStringExtra(EXTRA_ORDER_NAME);

        if (amount == 0 || orderName == null) {
            Toast.makeText(this, "결제 정보가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url.startsWith("file:///android_asset/payment.html") && !isPaymentScriptInjected) {
                    isPaymentScriptInjected = true;
                    String orderId = "ORDER_" + UUID.randomUUID().toString().replace("-", "");
                    String clientKey = BuildConfig.TOSS_CLIENT_KEY;
                    String method = "카드";

                    String script = String.format("javascript:requestTossPayment('%s', '%s', '%s', %d, '%s');",
                            clientKey, method, orderId, amount, orderName);
                    view.loadUrl(script);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http://localhost/success")) {
                    Uri uri = Uri.parse(url);
                    String paymentKey = uri.getQueryParameter("paymentKey");
                    String orderId = uri.getQueryParameter("orderId");
                    long returnedAmount = Long.parseLong(uri.getQueryParameter("amount"));
                    requestPaymentConfirmToServer(paymentKey, orderId, (int) returnedAmount);
                    return true;
                } else if (url.startsWith("http://localhost/fail")) {
                    Uri uri = Uri.parse(url);
                    String errorMessage = uri.getQueryParameter("message");
                    Toast.makeText(PaymentActivity.this, "결제 실패: " + errorMessage, Toast.LENGTH_LONG).show();
                    finish();
                    return true;
                }

                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        startActivity(intent);
                        return true;
                    } catch (URISyntaxException | ActivityNotFoundException e) {
                        Toast.makeText(getApplicationContext(), "실행할 앱을 찾을 수 없습니다.", Toast.LENGTH_LONG).show();
                        return true;
                    }
                }

                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                Log.e(TAG, "WebView Error: " + error.getDescription());
            }
        });

        webView.loadUrl("file:///android_asset/payment.html");
    }

    private void requestPaymentConfirmToServer(String paymentKey, String orderId, int amount) {
        int userId = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).getInt(Constants.KEY_USER_ID, -1);
        if (userId == -1) {
            Toast.makeText(this, "사용자 정보를 찾을 수 없습니다. 다시 로그인해주세요.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        PaymentConfirmRequest request = new PaymentConfirmRequest(paymentKey, orderId, amount, userId);
        apiService.confirmPayment(request).enqueue(new Callback<PaymentConfirmResponse>() {
            @Override
            public void onResponse(Call<PaymentConfirmResponse> call, Response<PaymentConfirmResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(PaymentActivity.this, "결제가 성공적으로 완료되었습니다.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    String errorMsg = "서버 승인 실패. 응답 코드: " + response.code();
                    Toast.makeText(PaymentActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<PaymentConfirmResponse> call, Throwable t) {
                Toast.makeText(PaymentActivity.this, "서버 통신 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Network Failure: ", t);
                finish();
            }
        });
    }
}
