package com.mobility.hack;

public class Payment {
    private final String amount;
    private final String orderId;
    private final String status;

    public Payment(String amount, String orderId, String status) {
        this.amount = amount;
        this.orderId = orderId;
        this.status = status;
    }

    public String getAmount() {
        return amount;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getStatus() {
        return status;
    }
}
