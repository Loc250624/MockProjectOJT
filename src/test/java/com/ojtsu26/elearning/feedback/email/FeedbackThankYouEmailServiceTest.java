package com.ojtsu26.elearning.feedback.email;

import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackThankYouEmailServiceTest {

    private JavaMailSender mailSender;
    private FeedbackThankYouEmailService service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new JavaMailSenderImpl().createMimeMessage());

        FeedbackEmailProperties properties = new FeedbackEmailProperties();
        properties.setFrom("noreply@lumina.test");
        properties.setFromName("Lumina Learning");
        properties.setReplyTo("support@lumina.test");
        properties.setSupportEmail("support@lumina.test");
        properties.setBanner("classpath:/mail/lumina-feedback-email-banner.png");

        service = new FeedbackThankYouEmailService(
                mailSender,
                templateEngine(),
                new DefaultResourceLoader(),
                properties
        );
    }

    @Test
    void sendBuildsMultipartMessageWithHtmlPlainTextAndInlineBanner() throws Exception {
        FeedbackSubmittedEvent event = new FeedbackSubmittedEvent(
                42,
                "Student One",
                "registered.student@example.com",
                LocalDateTime.of(2026, 7, 22, 9, 30)
        );

        service.send(event);

        var captor = org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage message = captor.getValue();

        assertEquals(FeedbackThankYouEmailService.SUBJECT, message.getSubject());
        assertEquals("registered.student@example.com",
                message.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("support@lumina.test", message.getReplyTo()[0].toString());

        List<String> htmlParts = new ArrayList<>();
        List<String> plainParts = new ArrayList<>();
        List<Part> inlineImages = new ArrayList<>();
        collectParts(message, htmlParts, plainParts, inlineImages);

        assertFalse(plainParts.isEmpty());
        String plain = String.join("\n", plainParts);
        assertTrue(plain.contains("Thank you for helping us improve."), plain);
        assertFalse(plain.contains("Submission details"), plain);
        assertFalse(plain.contains("Reference:"), plain);
        assertFalse(plain.contains("Received:"), plain);
        assertFalse(plain.contains("Status: Received"), plain);

        ByteArrayOutputStream rawMessage = new ByteArrayOutputStream();
        message.writeTo(rawMessage);
        String raw = rawMessage.toString(StandardCharsets.UTF_8);
        assertTrue(raw.contains("FEEDBACK RECEIVED"), raw);
        assertTrue(raw.contains("Thank you for helping us improve."), raw);
        assertTrue(raw.contains("cid:luminaFeedbackBanner"), raw);
        assertTrue(raw.contains("Content-ID: <luminaFeedbackBanner>"), raw);
        assertTrue(raw.contains("Content-Type: image/png"), raw);
        assertFalse(raw.contains("Please improve search."));
        assertFalse(raw.contains("Submission details"), raw);
        assertFalse(raw.contains("Reference"), raw);
        assertFalse(raw.contains("Received"), raw);
        assertFalse(raw.contains("Status"), raw);
    }

    @Test
    void sendRejectsMissingStudentEmailBeforeSending() {
        FeedbackSubmittedEvent event = new FeedbackSubmittedEvent(42, "Student One", " ", LocalDateTime.now());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> service.send(event));

        verify(mailSender, org.mockito.Mockito.never()).send(any(MimeMessage.class));
    }

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode("HTML");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private void collectParts(Part part,
                              List<String> htmlParts,
                              List<String> plainParts,
                              List<Part> inlineImages) throws MessagingException, IOException {
        if (part.isMimeType("image/png") && part.getHeader("Content-ID") != null) {
            inlineImages.add(part);
            return;
        }

        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            for (int index = 0; index < multipart.getCount(); index++) {
                BodyPart bodyPart = multipart.getBodyPart(index);
                collectParts(bodyPart, htmlParts, plainParts, inlineImages);
            }
            return;
        }
        if (part.isMimeType("text/html")) {
            htmlParts.add(content.toString());
            return;
        }
        if (part.isMimeType("text/plain")) {
            plainParts.add(content.toString());
        }
    }
}
