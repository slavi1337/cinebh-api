package com.cinebh.api.services.impl;

import com.cinebh.api.exceptions.ApiException;
import com.cinebh.api.services.AdvancedValidationService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Service
public class AdvancedValidationServiceImpl implements AdvancedValidationService {

    private static final Logger log = LoggerFactory.getLogger(AdvancedValidationServiceImpl.class);

    private static final String IANA_TLD_URL = "https://data.iana.org/TLD/tlds-alpha-by-domain.txt";
    private static final String DISPOSABLE_DOMAINS_URL = "https://raw.githubusercontent.com/disposable-email-domains/disposable-email-domains/master/disposable_email_blocklist.conf";
    private static final String PWNED_API_URL = "https://api.pwnedpasswords.com/range/";

    private final RestClient restClient;
    private Set<String> validTlds = new HashSet<>();
    private Set<String> disposableDomains = new HashSet<>();
    private boolean isInitialized = false;

    public AdvancedValidationServiceImpl() {
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public void validateEmailDomain(final String email) {
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

            final String response = restClient.get()
                    .uri(PWNED_API_URL + prefix)
                    .retrieve()
                    .body(String.class);

            if (response != null && response.contains(suffix)) {
                throw new ApiException("Password has been compromised in a data breach. Please choose a different one.", HttpStatus.BAD_REQUEST);
            }
        } catch (ApiException e) {
            throw e;
        } catch (RestClientException exception) {
            log.warn("Failed to check Pwned Passwords API. Assuming valid to avoid blocking users.", exception);
        } catch (NoSuchAlgorithmException exception) {
            log.error("SHA-1 algorithm not found.", exception);
            throw new ApiException("Internal security error.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostConstruct
    public void initializeLists() {
        if (isInitialized) {
            return;
        }

        try {
            log.info("Downloading TLD and Disposable email blocklists...");

            final String tldData = restClient.get()
                    .uri(IANA_TLD_URL)
                    .retrieve()
                    .body(String.class);

            if (tldData != null) {
                validTlds = Arrays.stream(tldData.split("\n"))
                        .filter(line -> !line.startsWith("#") && !line.isBlank())
                        .map(String::trim)
                        .collect(Collectors.toSet());
            }

            final String disposableData = restClient.get()
                    .uri(DISPOSABLE_DOMAINS_URL)
                    .retrieve()
                    .body(String.class);

            if (disposableData != null) {
                disposableDomains = Arrays.stream(disposableData.split("\n"))
                        .filter(line -> !line.isBlank())
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());
            }

            isInitialized = true;
            log.info("Successfully loaded {} TLDs and {} disposable domains.", validTlds.size(), disposableDomains.size());
        } catch (RestClientException exception) {
            log.warn("Failed to fetch external validation lists due to network error.", exception);
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
