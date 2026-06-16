package com.cinebh.api.services;

import com.cinebh.api.services.impl.EmailNotificationService;
import com.cinebh.api.services.impl.TicketQrCodeService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private TicketQrCodeService ticketQrCodeService;

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

    @Test
    void shouldSendTicketPurchaseConfirmationSuccessfully() throws Exception {
        final MimeMessage mimeMessage = mock(MimeMessage.class);
        final UUID ticketCode = UUID.fromString("00000000-0000-0000-0000-000000000222");

        setupMocking(mimeMessage);
        when(ticketQrCodeService.createTicketQrCode(ticketCode)).thenReturn(new byte[]{1, 2, 3});
        when(templateEngine.process(eq("ticket-purchase-confirmation-email"), any(Context.class)))
                .thenReturn("<html>Ticket Confirmation</html>");

        emailNotificationService.sendTicketPurchaseConfirmation(
                "user@example.com",
                "John Doe",
                UUID.fromString("00000000-0000-0000-0000-000000000111"),
                ticketCode,
                "Mandalorian",
                "Banja Luka",
                "Cinebh Arena",
                "Hall 1",
                OffsetDateTime.parse("2026-06-15T18:00:00Z"),
                List.of("A1", "A2"),
                BigDecimal.valueOf(14),
                "BAM"
        );

        verify(mailSender).send(mimeMessage);
        verify(templateEngine).process(eq("ticket-purchase-confirmation-email"), any(Context.class));
    }

    private void setupMocking(final MimeMessage mimeMessage) {
        ReflectionTestUtils.setField(emailNotificationService, "fromAddress", "slavisa.covakusic@student.etf.unibl.org");
        ReflectionTestUtils.setField(emailNotificationService, "fromName", "Cinebh");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }
}
