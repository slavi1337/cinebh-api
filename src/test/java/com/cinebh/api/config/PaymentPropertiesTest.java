package com.cinebh.api.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPropertiesTest {

    @Test
    void shouldAcceptValidStripeConfiguration() {
        final PaymentProperties paymentProperties = new PaymentProperties(
                new PaymentProperties.Stripe("sk_test_secret", "whsec_test", null)
        );

        assertThat(validate(paymentProperties)).isEmpty();
    }

    @Test
    void shouldRejectMissingStripeConfiguration() {
        final PaymentProperties paymentProperties = new PaymentProperties(null);

        assertThat(validate(paymentProperties)).anySatisfy(violation ->
                assertThat(violation.getPropertyPath().toString()).isEqualTo("stripe")
        );
    }

    @Test
    void shouldRejectBlankStripeSecrets() {
        final PaymentProperties paymentProperties = new PaymentProperties(
                new PaymentProperties.Stripe(" ", "", "bam")
        );

        assertThat(validate(paymentProperties))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("stripe.secretKey", "stripe.webhookSecret");
    }

    private Set<ConstraintViolation<PaymentProperties>> validate(
            final PaymentProperties paymentProperties
    ) {
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            final Validator validator = validatorFactory.getValidator();
            return validator.validate(paymentProperties);
        }
    }
}
