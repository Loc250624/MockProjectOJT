package com.ojtsu26.elearning.controller.api;

import com.ojtsu26.elearning.model.enums.PaymentMethod;
import com.ojtsu26.elearning.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/webhook")
    public ResponseEntity<?> handleMomoWebhook(@RequestBody Map<String, String> payload) {
        log.info("Received MoMo webhook callback: {}", payload);
        try {
            paymentService.processWebhook(PaymentMethod.MOMO, payload);
            return ResponseEntity.ok(Map.of(
                    "resultCode", 0,
                    "message", "Success"
            ));
        } catch (IllegalArgumentException e) {
            log.error("Invalid webhook payload or signature", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "resultCode", 99,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Internal error processing webhook", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "resultCode", 99,
                    "message", "Internal server error"
            ));
        }
    }
}
