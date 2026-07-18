package com.ojtsu26.elearning.controller.api;

import com.ojtsu26.elearning.model.enums.PaymentMethod;
import com.ojtsu26.elearning.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/webhook")
    public ResponseEntity<?> handleMomoWebhook(@RequestBody Map<String, String> payload) {
        log.info("Received MoMo webhook callback: {}", callbackSummary(payload, "orderId", "transId", "signature"));
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

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<?> handleVnpayIpn(@RequestParam Map<String, String> params) {
        log.info("Received VNPAY IPN callback: {}", callbackSummary(params, "vnp_TxnRef", "vnp_TransactionNo", "vnp_SecureHash"));
        
        Map<String, String> normalizedParams = new java.util.HashMap<>();
        normalizedParams.put("orderId", params.get("vnp_TxnRef"));
        normalizedParams.put("transId", params.get("vnp_TransactionNo"));
        
        String responseCode = params.get("vnp_ResponseCode");
        String resultCode = "00".equals(responseCode) ? "0" : (responseCode != null ? responseCode : "99");
        normalizedParams.put("resultCode", resultCode);
        
        // Include everything for signature check
        normalizedParams.putAll(params);

        try {
            paymentService.processWebhook(PaymentMethod.VNPAY, normalizedParams);
            return ResponseEntity.ok(Map.of(
                    "RspCode", "00",
                    "Message", "Confirm Success"
            ));
        } catch (IllegalArgumentException e) {
            log.error("Invalid VNPAY signature or params", e);
            String message = e.getMessage() != null ? e.getMessage() : "";
            if (message.contains("Order not found")) {
                return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order not Found"));
            }
            return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid Checksum"));
        } catch (Exception e) {
            log.error("Error processing VNPAY IPN", e);
            return ResponseEntity.ok(Map.of(
                    "RspCode", "99",
                    "Message", "Input Required"
            ));
        }
    }

    private Map<String, Object> callbackSummary(
            Map<String, String> params,
            String orderKey,
            String transactionKey,
            String signatureKey) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("orderId", params.get(orderKey));
        summary.put("transactionPresent", params.containsKey(transactionKey));
        summary.put("signaturePresent", params.containsKey(signatureKey));
        summary.put("fieldCount", params.size());
        return summary;
    }
}
