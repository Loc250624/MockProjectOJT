package com.ojtsu26.elearning.feedback.email;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class FeedbackThankYouEmailService {

    public static final String CID = "luminaFeedbackBanner";
    static final String SUBJECT = "Thank you for sharing your feedback | Lumina Learning";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final ResourceLoader resourceLoader;
    private final FeedbackEmailProperties properties;

    public FeedbackThankYouEmailService(JavaMailSender mailSender,
                                        TemplateEngine templateEngine,
                                        ResourceLoader resourceLoader,
                                        FeedbackEmailProperties properties) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.resourceLoader = resourceLoader;
        this.properties = properties;
    }

    public void send(FeedbackSubmittedEvent event) {
        validateEmail(event.studentEmail());

        try {
            String name = defaultText(event.studentDisplayName(), "Student");
            String supportEmail = defaultText(properties.getSupportEmail(), properties.getReplyTo());

            Context context = new Context(Locale.ENGLISH);
            context.setVariable("studentName", name);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("bannerCid", CID);

            String html = templateEngine.process("mail/feedback-thank-you", context);
            String plain = plainText(name, supportEmail);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(event.studentEmail());
            helper.setSubject(SUBJECT);
            helper.setFrom(new InternetAddress(
                    required(properties.getFrom(), "app.feedback-email.from"),
                    defaultText(properties.getFromName(), "Lumina Learning")
            ));

            if (StringUtils.hasText(properties.getReplyTo())) {
                helper.setReplyTo(properties.getReplyTo());
            }

            helper.setText(plain, html);

            Resource banner = resourceLoader.getResource(properties.getBanner());
            if (!banner.exists() || !banner.isReadable()) {
                throw new IllegalStateException("Feedback email banner is not readable");
            }
            helper.addInline(CID, banner, "image/png");

            mailSender.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to send feedback receipt email", ex);
        }
    }

    private static void validateEmail(String email) {
        if (!StringUtils.hasText(email)
                || !email.contains("@")
                || email.contains("\r")
                || email.contains("\n")) {
            throw new IllegalArgumentException("Student account does not have a usable email");
        }
    }

    private static String required(String value, String property) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(property + " must be configured");
        }
        return value;
    }

    private static String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private static String plainText(String name, String supportEmail) {
        String help = StringUtils.hasText(supportEmail)
                ? "For assistance, please reply to this email when a monitored reply-to address is configured, "
                + "or contact " + supportEmail + "."
                : "For assistance, please use the official Lumina Learning support channel.";

        return """
                FEEDBACK RECEIVED

                Thank you for helping us improve.

                Dear %s,

                Thank you for taking the time to share your feedback with Lumina Learning. We have successfully received your submission and forwarded it to our administration team for review.

                Your comments help us improve the learning experience for students, teachers, and administrators across the platform. While this automated message confirms receipt, our team may contact you through your registered email address if additional information is required.

                %s

                Kind regards,

                Lumina Learning Support Team
                Learn with clarity. Teach with confidence.

                This is an automated confirmation sent to the email address registered with your Lumina Learning account. Please do not share passwords or other sensitive information by email.
                """.formatted(name, help);
    }
}
