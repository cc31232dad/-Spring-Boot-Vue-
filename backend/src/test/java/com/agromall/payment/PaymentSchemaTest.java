package com.agromall.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PaymentSchemaTest {

    @Autowired JdbcTemplate jdbc;

    @Test
    void migrationCreatesPaymentRecordContract() {
        List<Map<String, Object>> columns = jdbc.queryForList("SHOW COLUMNS FROM payment_records");
        assertThat(columns).extracting(row -> row.get("Field"))
                .contains("payment_no", "order_id", "buyer_id", "amount", "status", "expires_at", "callback_count");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'payment_records' AND column_name = 'payment_no' AND non_unique = 0", Integer.class))
                .isEqualTo(1);
    }
}
