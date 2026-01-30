package com.mobility.hack;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class PaymentHistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        RecyclerView rvPaymentHistory = findViewById(R.id.rv_payment_history);
        rvPaymentHistory.setLayoutManager(new LinearLayoutManager(this));

        // Create dummy data
        List<Payment> payments = new ArrayList<>();
        payments.add(new Payment("1,000원", "ORDER_12345", "DONE"));
        payments.add(new Payment("2,500원", "ORDER_67890", "CANCELLED"));

        PaymentHistoryAdapter adapter = new PaymentHistoryAdapter(payments);
        rvPaymentHistory.setAdapter(adapter);
    }
}
