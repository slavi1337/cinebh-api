package com.cinebh.api.services;

import com.cinebh.api.config.NotificationProperties;
import com.cinebh.api.services.impl.EmailNotificationService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private NotificationProperties notificationProperties;

    @InjectMocks
    private EmailNotificationService emailNotificationService;

    @Test
    void shouldSendAccountVerificationCodeSuccessfully() throws Exception {
        final String toEmail = "user@example.com";
        final String toName = "John Doe";
        final String code = "123456";
        final MimeMessage mimeMessage = mock(MimeMessage.class);

        setupMocking(mimeMessage);
        when(templateEngine.process(eq("verification-email"), any(Context.class))).thenReturn("<html>HTML Content</html>");

        emailNotificationService.sendAccountVerificationCode(toEmail, toName, code);

        verify(mailSender).send(mimeMessage);
        verify(templateEngine).process(eq("verification-email"), any(Context.class));
    }

    @Test
    void shouldSendPasswordResetCodeSuccessfully() throws Exception {
        final String toEmail = "user@example.com";
        final String toName = "John Doe";
        final String code = "654321";
        final MimeMessage mimeMessage = mock(MimeMessage.class);

        setupMocking(mimeMessage);
        when(templateEngine.process(eq("verification-email"), any(Context.class))).thenReturn("<html>HTML Content</html>");

        emailNotificationService.sendPasswordResetCode(toEmail, toName, code);

        verify(mailSender).send(mimeMessage);
        verify(templateEngine).process(eq("verification-email"), any(Context.class));
    }

    private void setupMocking(final MimeMessage mimeMessage) {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(notificationProperties.getFromAddress()).thenReturn("slavisa.covakusic@student.etf.unibl.org");
        when(notificationProperties.getFromName()).thenReturn("Cinebh");
    }
}
