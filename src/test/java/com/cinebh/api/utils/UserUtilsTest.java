package com.cinebh.api.utils;

import com.cinebh.api.entities.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class UserUtilsTest {

    @Test
    void shouldReturnFullNameWhenAtLeastOneNamePartExists() {
        final User user = createUser("John", " ");

        assertThat(UserUtils.fullNameOrEmail(user)).isEqualTo("John");
    }

    @Test
    void shouldReturnEmailWhenNamePartsAreBlank() {
        final User user = createUser(null, "");

        assertThat(UserUtils.fullNameOrEmail(user)).isEqualTo("customer@cinebh.test");
    }

    private User createUser(final String firstName, final String lastName) {
        final User user = new User();
        ReflectionTestUtils.setField(user, "email", "customer@cinebh.test");
        ReflectionTestUtils.setField(user, "firstName", firstName);
        ReflectionTestUtils.setField(user, "lastName", lastName);
        return user;
    }
}
