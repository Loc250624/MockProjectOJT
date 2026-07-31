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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class FeedbackThankYouEmailService {

    public static final String CID = "luminaFeedbackBanner";
    static final String SUBJECT = "Thank you for sharing your feedback | Lumina Learning";
    private static final DateTimeFormatter SUBMITTED_AT_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM uuuu, HH:mm", Locale.ENGLISH);

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
            context.setVariable("feedbackSubject", defaultText(event.subject(), "Feedback"));
            context.setVariable("feedbackContent", defaultText(event.content(), "No message provided."));
            context.setVariable("submittedAt", formatSubmittedAt(event.submittedAt()));
            context.setVariable("ratings", ratings(event));

            String html = templateEngine.process("mail/feedback-thank-you", context);
            String plain = plainText(event, name, supportEmail);

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

    private static List<FeedbackRating> ratings(FeedbackSubmittedEvent event) {
        return List.of(
                rating("Course Content", event.courseContentRating(), false),
                rating("Instructor Support", event.instructorSupportRating(), false),
                rating("Learning Experience", event.learningExperienceRating(), false),
                rating("Platform Usability", event.platformUsabilityRating(), false),
                rating("Assessment Experience", event.assessmentExperienceRating(), false),
                rating("Overall Satisfaction", event.overallSatisfactionRating(), true)
        );
    }

    private static FeedbackRating rating(String label, Integer value, boolean overall) {
        int score = value == null ? 0 : Math.max(0, Math.min(5, value));
        return new FeedbackRating(label, score, "★".repeat(score) + "☆".repeat(5 - score), overall);
    }

    private static String formatSubmittedAt(LocalDateTime submittedAt) {
        LocalDateTime timestamp = submittedAt == null ? LocalDateTime.now() : submittedAt;
        return timestamp.format(SUBMITTED_AT_FORMAT);
    }

    private static String plainText(FeedbackSubmittedEvent event, String name, String supportEmail) {
        String help = StringUtils.hasText(supportEmail)
                ? "For assistance, contact " + supportEmail + "."
                : "For assistance, please use the official Lumina Learning support channel.";
        String ratingLines = ratings(event).stream()
                .map(rating -> rating.label() + ": " + rating.score() + "/5")
                .collect(java.util.stream.Collectors.joining("\n"));

        return """
                FEEDBACK RECEIVED

                Dear %s,

                Thank you for sharing your experience with Lumina Learning. Your feedback has been received and forwarded to our administration team for review.

                YOUR FEEDBACK SUMMARY

                Subject: %s
                Submitted: %s

                Ratings
                %s

                Message
                %s

                %s

                Respectfully,
                Lumina Learning Support Team
                Learn with clarity. Teach with confidence.

                This automated email confirms receipt of the feedback submitted from your Lumina Learning account.
                """.formatted(
                name,
                defaultText(event.subject(), "Feedback"),
                formatSubmittedAt(event.submittedAt()),
                ratingLines,
                defaultText(event.content(), "No message provided."),
                help);
    }

    private record FeedbackRating(String label, int score, String stars, boolean overall) {
    }
}
