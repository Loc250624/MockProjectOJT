package com.ojtsu26.elearning.scheduler;

import com.ojtsu26.elearning.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpirationScheduler {

    private final OrderService orderService;

    // Run every 5 minutes
    @Scheduled(cron = "0 */5 * * * *")
    public void runExpirationJob() {
        log.info("Starting scheduled job to expire pending orders...");
        try {
            orderService.expirePendingOrders();
        } catch (Exception e) {
            log.error("Failed to run order expiration job", e);
        }
        log.info("Finished scheduled job to expire pending orders.");
    }
}
