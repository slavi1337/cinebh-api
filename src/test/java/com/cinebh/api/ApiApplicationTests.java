package com.cinebh.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.payment.stripe.secret-key=sk_test_context",
        "app.payment.stripe.webhook-secret=whsec_context"
})
class ApiApplicationTests {

    @Test
    void contextLoads() {
    }

}
