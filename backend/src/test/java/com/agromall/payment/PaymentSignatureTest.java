package com.agromall.payment;

import com.agromall.payment.application.PaymentSignature;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentSignatureTest {

    @Test
    void signsCanonicalPayloadAndVerifiesWithinTimestampWindow() {
        PaymentSignature signature = new PaymentSignature("secret", 300);
        long now = 1_700_000_000L;
        String value = signature.sign("PAY-1", "ORD-1", new BigDecimal("12.50"), "SUCCESS", now);

        assertThat(signature.canonical("PAY-1", "ORD-1", new BigDecimal("12.50"), "SUCCESS", now))
                .isEqualTo("PAY-1|ORD-1|12.50|SUCCESS|1700000000");
        assertThat(signature.verify("PAY-1", "ORD-1", new BigDecimal("12.50"), "SUCCESS", now, value, now)).isTrue();
        assertThat(signature.verify("PAY-1", "ORD-1", new BigDecimal("12.50"), "SUCCESS", now, "bad", now)).isFalse();
        assertThat(signature.verify("PAY-1", "ORD-1", new BigDecimal("12.50"), "SUCCESS", now - 301, value, now)).isFalse();
    }
}
