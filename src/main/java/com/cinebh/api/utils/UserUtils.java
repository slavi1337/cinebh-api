package com.cinebh.api.utils;

import com.cinebh.api.entities.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserUtils {

    public static String fullNameOrEmail(final User user) {
        final String fullName = Stream.of(user.getFirstName(), user.getLastName())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));

        return fullName.isBlank() ? user.getEmail() : fullName;
    }
}
