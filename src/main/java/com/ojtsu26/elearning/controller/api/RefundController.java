package com.ojtsu26.elearning.controller.api;

import com.ojtsu26.elearning.common.UserFacingErrorMessage;
import com.ojtsu26.elearning.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/refunds")
@RequiredArgsConstructor
@Slf4j
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    public ResponseEntity<?> triggerRefund(@RequestParam("orderId") Integer orderId,
                                           @RequestParam(value = "reason", defaultValue = "Admin request") String reason) {
        log.info("Requesting refund for orderId: {}, reason: {}", orderId, reason);
        try {
            refundService.processRefund(orderId, reason);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Refund processed successfully."
            ));
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.error("Refund validation failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", UserFacingErrorMessage.from(e)
            ));
        } catch (Exception e) {
            log.error("Internal error processing refund", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Internal server error occurred."
            ));
        }
    }
}
