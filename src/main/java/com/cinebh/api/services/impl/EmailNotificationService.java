package com.cinebh.api.services.impl;

import com.cinebh.api.services.NotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
public class EmailNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final String VERIFICATION_TEMPLATE = "verification-email";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.notification.from-address}")
    private String fromAddress;

    @Value("${app.notification.from-name}")
    private String fromName;

    @Async
    @Override
    public void sendAccountVerificationCode(final String toEmail, final String toName, final String code) {
        final Context context = buildContext(toName, code, "Verify your Cinebh Account");
        sendHtmlEmail(toEmail, "Welcome to Cinebh - Verify your account", context);
    }

    @Async
    @Override
    public void sendPasswordResetCode(final String toEmail, final String toName, final String code) {
        final Context context = buildContext(toName, code, "Reset your Cinebh Password");
        sendHtmlEmail(toEmail, "Cinebh - Password Reset", context);
    }

    private Context buildContext(final String name, final String code, final String title) {
        final Context context = new Context();
        context.setVariable("name", name != null ? name : "User");
        context.setVariable("code", code);
        context.setVariable("title", title);
        return context;
    }

    private void sendHtmlEmail(final String toEmail, final String subject, final Context context) {
        try {
            final MimeMessage mimeMessage = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            final String htmlContent = templateEngine.process(VERIFICATION_TEMPLATE, context);

            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Successfully sent email to {}", toEmail);

        } catch (MessagingException | UnsupportedEncodingException exception) {
            log.error("Failed to send email to {}", toEmail, exception);
        }
    }
}
