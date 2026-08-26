package com.agromall.payment.application;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class PaymentSignature {

    private final byte[] secret;
    private final long timestampWindowSeconds;

    @Autowired
    public PaymentSignature(@Value("${agromall.payment.sandbox-secret}") String secret) {
        this(secret, 300);
    }

    public PaymentSignature(String secret, long timestampWindowSeconds) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.timestampWindowSeconds = timestampWindowSeconds;
    }

    public String canonical(String paymentNo, String orderNo, BigDecimal amount, String result, long timestamp) {
        return String.join("|", paymentNo, orderNo, amount.toPlainString(), result, Long.toString(timestamp));
    }

    public String sign(String paymentNo, String orderNo, BigDecimal amount, String result, long timestamp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] digest = mac.doFinal(canonical(paymentNo, orderNo, amount, result, timestamp)
                    .getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) hex.append("%02x".formatted(value));
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign sandbox payment callback", exception);
        }
    }

    public boolean verify(String paymentNo, String orderNo, BigDecimal amount, String result, long timestamp,
                          String providedSignature, long now) {
        if (providedSignature == null || Math.abs(now - timestamp) > timestampWindowSeconds) return false;
        return MessageDigest.isEqual(sign(paymentNo, orderNo, amount, result, timestamp)
                .getBytes(StandardCharsets.US_ASCII), providedSignature.getBytes(StandardCharsets.US_ASCII));
    }
}
