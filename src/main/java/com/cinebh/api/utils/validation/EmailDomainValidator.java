package com.cinebh.api.utils.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.stream.Collectors;

public class EmailDomainValidator implements ConstraintValidator<ValidEmailDomain, String> {

    private static final Logger log = LoggerFactory.getLogger(EmailDomainValidator.class);

    private static final String IANA_TLD_URL = "https://data.iana.org/TLD/tlds-alpha-by-domain.txt";
    private static final String DISPOSABLE_DOMAINS_URL = "https://raw.githubusercontent.com/disposable-email-domains/disposable-email-domains/master/disposable_email_blocklist.conf";

    private static Set<String> validTlds = new HashSet<>();
    private static Set<String> disposableDomains = new HashSet<>();
    private static boolean isInitialized = false;

    @Override
    public boolean isValid(final String email, final ConstraintValidatorContext context) {
        if (email == null || !email.contains("@")) {
            return false;
        }

        initializeLists();

        final String domain = email.substring(email.lastIndexOf("@") + 1).toLowerCase();
        final String tld = domain.contains(".") ? domain.substring(domain.lastIndexOf(".") + 1).toUpperCase() : "";

        if (!validTlds.isEmpty() && !validTlds.contains(tld)) {
            log.debug("Invalid TLD detected: {}", tld);
            return false;
        }

        if (disposableDomains.contains(domain)) {
            log.debug("Disposable email domain detected: {}", domain);
            return false;
        }

        return hasMxRecord(domain);
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
            log.warn("Failed to fetch external validation lists. Falling back to MX validation only.", exception);
            isInitialized = true;
        }
    }

    private boolean hasMxRecord(final String domain) {
        try {
            final Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");

            final DirContext dirContext = new InitialDirContext(env);
            final Attributes attributes = dirContext.getAttributes(domain, new String[]{"MX"});
            final Attribute mx = attributes.get("MX");

            return mx != null && mx.size() > 0;
        } catch (Exception exception) {
            log.warn("MX record check failed for domain: {}. Reason: {}", domain, exception.getMessage());
            return false;
        }
    }
}
