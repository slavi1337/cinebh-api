package com.cinebh.api.utils.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EmailDomainValidatorTest {

    @InjectMocks
    private EmailDomainValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Test
    void shouldReturnFalseForInvalidEmailFormat() {
        assertThat(validator.isValid("invalid-email", context)).isFalse();
        assertThat(validator.isValid(null, context)).isFalse();
    }

    @Test
    void shouldReturnFalseForDisposableEmail() {
        final boolean result = validator.isValid("test@mailinator.com", context);
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseForInvalidTld() {
        final boolean result = validator.isValid("user@test.something_fake", context);
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnTrueForValidEmail() {
        final boolean result = validator.isValid("cinebh.internship@gmail.com", context);
        assertThat(result).isTrue();
    }
}
