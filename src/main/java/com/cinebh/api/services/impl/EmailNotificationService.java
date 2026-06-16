package com.cinebh.api.services.impl;

import com.cinebh.api.services.NotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final String VERIFICATION_TEMPLATE = "verification-email";
    private static final String TICKET_PURCHASE_CONFIRMATION_TEMPLATE = "ticket-purchase-confirmation-email";
    private static final String TICKET_QR_CONTENT_ID = "bookingQrCode";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final TicketQrCodeService ticketQrCodeService;

    @Value("${app.notification.from-address}")
    private String fromAddress;

    @Value("${app.notification.from-name}")
    private String fromName;

    @Async
    @Override
    public void sendAccountVerificationCode(final String toEmail, final String toName, final String code) {
        final Context context = buildContext(toName, code, "Verify your Cinebh Account");
        sendHtmlEmail(toEmail, "Welcome to Cinebh - Verify your account", context, VERIFICATION_TEMPLATE);
    }

    @Async
    @Override
    public void sendPasswordResetCode(final String toEmail, final String toName, final String code) {
        final Context context = buildContext(toName, code, "Reset your Cinebh Password");
        sendHtmlEmail(toEmail, "Cinebh - Password Reset", context, VERIFICATION_TEMPLATE);
    }

    @Async
    @Override
    public void sendTicketPurchaseConfirmation(
            final String toEmail,
            final String toName,
            final UUID bookingId,
            final UUID ticketCode,
            final String movieTitle,
            final String cityName,
            final String venueName,
            final String hallName,
            final OffsetDateTime projectionStartTime,
            final List<String> seats,
            final BigDecimal totalPrice,
            final String currency
    ) {
        final byte[] ticketQrCode = ticketQrCodeService.createTicketQrCode(ticketCode);
        final Context context = new Context();
        context.setVariable("name", toName != null ? toName : "User");
        context.setVariable("bookingId", bookingId);
        context.setVariable("ticketCode", ticketCode);
        context.setVariable("bookingQrContentId", TICKET_QR_CONTENT_ID);
        context.setVariable("hasBookingQrCode", hasContent(ticketQrCode));
        context.setVariable("movieTitle", movieTitle);
        context.setVariable("cityName", cityName);
        context.setVariable("venueName", venueName);
        context.setVariable("hallName", hallName);
        context.setVariable("projectionStartTime", formatProjectionStartTime(projectionStartTime));
        context.setVariable("seats", seats);
        context.setVariable("totalPrice", totalPrice);
        context.setVariable("currency", currency);

        sendHtmlEmail(
                toEmail,
                "Cinebh - Ticket Purchase Confirmation",
                context,
                TICKET_PURCHASE_CONFIRMATION_TEMPLATE,
                ticketQrCode
        );
    }

    private Context buildContext(final String name, final String code, final String title) {
        final Context context = new Context();
        context.setVariable("name", name != null ? name : "User");
        context.setVariable("code", code);
        context.setVariable("title", title);
        return context;
    }

    private void sendHtmlEmail(
            final String toEmail,
            final String subject,
            final Context context,
            final String templateName
    ) {
        sendHtmlEmail(toEmail, subject, context, templateName, null);
    }

    private void sendHtmlEmail(
            final String toEmail,
            final String subject,
            final Context context,
            final String templateName,
            final byte[] qrCode
    ) {
        try {
            final MimeMessage mimeMessage = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            final String htmlContent = templateEngine.process(templateName, context);

            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            addTicketQrCode(helper, qrCode);

            mailSender.send(mimeMessage);
            log.info("Successfully sent email to {}", toEmail);

        } catch (MessagingException | UnsupportedEncodingException exception) {
            log.error("Failed to send email to {}", toEmail, exception);
        }
    }

    private String formatProjectionStartTime(final OffsetDateTime projectionStartTime) {
        if (projectionStartTime == null) {
            return "";
        }

        return projectionStartTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH));
    }

    private void addTicketQrCode(final MimeMessageHelper helper, final byte[] qrCode) throws MessagingException {
        if (!hasContent(qrCode)) {
            return;
        }

        helper.addInline(TICKET_QR_CONTENT_ID, new ByteArrayResource(qrCode), "image/png");
    }

    private boolean hasContent(final byte[] value) {
        return value != null && value.length > 0;
    }
}
