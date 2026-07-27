package com.ojtsu26.elearning.feedback.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeedbackEmailListener {

    private final FeedbackThankYouEmailService emailService;
    private final FeedbackEmailProperties properties;

    @Async("feedbackEmailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FeedbackSubmittedEvent event) {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            emailService.send(event);
        } catch (RuntimeException ex) {
            log.error("Feedback receipt email failed; feedbackId={}, errorType={}",
                    event.feedbackId(),
                    ex.getClass().getSimpleName());
        }
    }
}
