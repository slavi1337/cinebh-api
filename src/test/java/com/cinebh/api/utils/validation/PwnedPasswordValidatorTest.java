package com.cinebh.api.utils.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PwnedPasswordValidatorTest {

    @InjectMocks
    private PwnedPasswordValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Test
    void shouldReturnTrueForNullOrEmptyPassword() {
        assertThat(validator.isValid(null, context)).isTrue();
        assertThat(validator.isValid("", context)).isTrue();
    }

    @Test
    void shouldReturnFalseForCommonPwnedPassword() {
        final boolean result = validator.isValid("Password123", context);
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnTrueForSecureUniquePassword() {
        final boolean result = validator.isValid("Cinebh_Secret_2026_Unique_!#", context);
        assertThat(result).isTrue();
    }
}
