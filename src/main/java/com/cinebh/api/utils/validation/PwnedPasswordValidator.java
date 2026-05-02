package com.cinebh.api.utils.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PwnedPasswordValidator implements ConstraintValidator<NotPwned, String> {

    private static final Logger log = LoggerFactory.getLogger(PwnedPasswordValidator.class);
    private static final String PWNED_API_URL = "https://api.pwnedpasswords.com/range/";

    @Override
    public boolean isValid(final String password, final ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            return true;
        }

        try {
            final String sha1Hash = getSha1Hash(password);
            final String prefix = sha1Hash.substring(0, 5);
            final String suffix = sha1Hash.substring(5);

            final RestTemplate restTemplate = new RestTemplate();
            final String response = restTemplate.getForObject(PWNED_API_URL + prefix, String.class);

            if (response != null) {
                return !response.contains(suffix);
            }

        } catch (Exception exception) {
            log.warn("Failed to check Pwned Passwords API. Assuming valid to avoid blocking users.", exception);
        }

        return true;
    }

    private String getSha1Hash(final String input) throws NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-1");
        final byte[] bytes = digest.digest(input.getBytes());
        final StringBuilder builder = new StringBuilder();

        for (final byte b : bytes) {
            builder.append(String.format("%02X", b));
        }

        return builder.toString();
    }
}
