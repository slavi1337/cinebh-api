package com.cinebh.api.services.impl;

import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.services.AdvancedValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdvancedValidationServiceImpl implements AdvancedValidationService {

    private static final Logger log = LoggerFactory.getLogger(AdvancedValidationServiceImpl.class);

    private static final String IANA_TLD_URL = "https://data.iana.org/TLD/tlds-alpha-by-domain.txt";
    private static final String DISPOSABLE_DOMAINS_URL = "https://raw.githubusercontent.com/disposable-email-domains/disposable-email-domains/master/disposable_email_blocklist.conf";
    private static final String PWNED_API_URL = "https://api.pwnedpasswords.com/range/";

    private Set<String> validTlds = new HashSet<>();
    private Set<String> disposableDomains = new HashSet<>();
    private boolean isInitialized = false;

    @Override
    public void validateEmailDomain(final String email) {
        initializeLists();

        final String domain = email.substring(email.lastIndexOf("@") + 1).toLowerCase();
        final String tld = domain.contains(".") ? domain.substring(domain.lastIndexOf(".") + 1).toUpperCase() : "";

        if (!validTlds.isEmpty() && !validTlds.contains(tld)) {
            throw new ApiException("Invalid email domain TLD.", HttpStatus.BAD_REQUEST);
        }

        if (disposableDomains.contains(domain)) {
            throw new ApiException("Temporary or disposable emails are not allowed.", HttpStatus.BAD_REQUEST);
        }

        if (!hasMxRecord(domain)) {
            throw new ApiException("Email domain cannot receive messages (No MX records).", HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public void validatePasswordPwned(final String password) {
        try {
            final String sha1Hash = getSha1Hash(password);
            final String prefix = sha1Hash.substring(0, 5);
            final String suffix = sha1Hash.substring(5);

            final RestTemplate restTemplate = new RestTemplate();
            final String response = restTemplate.getForObject(PWNED_API_URL + prefix, String.class);

            if (response != null && response.contains(suffix)) {
                throw new ApiException("Password has been compromised in a data breach. Please choose a different one.", HttpStatus.BAD_REQUEST);
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception exception) {
            log.warn("Failed to check Pwned Passwords API. Assuming valid to avoid blocking users.", exception);
        }
    }

    private synchronized void initializeLists() {
        if (isInitialized) {
            return;
        }

        try {
            final RestTemplate restTemplate = new RestTemplate();

            final String tldData = restTemplate.getForObject(IANA_TLD_URL, String.class);
            if (tldData != null) {
                validTlds = Arrays.stream(tldData.split("\n"))
                        .filter(line -> !line.startsWith("#") && !line.isBlank())
                        .map(String::trim)
                        .collect(Collectors.toSet());
            }

            final String disposableData = restTemplate.getForObject(DISPOSABLE_DOMAINS_URL, String.class);
            if (disposableData != null) {
                disposableDomains = Arrays.stream(disposableData.split("\n"))
                        .filter(line -> !line.isBlank())
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());
            }

            isInitialized = true;
            log.info("Successfully loaded {} TLDs and {} disposable domains.", validTlds.size(), disposableDomains.size());
        } catch (Exception exception) {
            log.warn("Failed to fetch external validation lists.", exception);
            isInitialized = true;
        }
    }

    private boolean hasMxRecord(final String domain) {
        try {
            final Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");

            final DirContext dirContext = new InitialDirContext(env);
            final Attributes attributes = dirContext.getAttributes(domain, new String[]{"MX"});
            return attributes.get("MX") != null && attributes.get("MX").size() > 0;
        } catch (Exception exception) {
            log.warn("MX record check failed for domain: {}. Reason: {}", domain, exception.getMessage());
            return false;
        }
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
