package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.config.PaymentGatewayProperties;
import com.ojtsu26.elearning.common.HmacUtils;
import com.ojtsu26.elearning.dto.request.PaymentRequest;
import com.ojtsu26.elearning.dto.response.PaymentResponse;
import com.ojtsu26.elearning.model.enums.PaymentMethod;
import com.ojtsu26.elearning.service.PaymentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class MoMoProvider implements PaymentProvider {

    private final PaymentGatewayProperties properties;

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.MOMO;
    }

    @Override
    public PaymentResponse initiatePayment(PaymentRequest request) {
        String partnerCode = properties.getMomo().getPartnerCode();
        String accessKey = properties.getMomo().getAccessKey();
        String secretKey = properties.getMomo().getSecretKey();
        String endpoint = properties.getMomo().getEndpoint();

        if (partnerCode == null || accessKey == null || secretKey == null || endpoint == null) {
            log.error("MoMo Sandbox configurations are missing");
            return PaymentResponse.builder()
                    .success(false)
                    .errorMessage("MoMo Sandbox credentials are not configured in properties.")
                    .build();
        }

        // Invariant: Course.price is stored in VND and must not be converted during checkout.
        BigDecimal amount = request.getAmount();
        if (amount == null) {
            log.error("MoMo payment initiation failed: amount is null");
            return PaymentResponse.builder()
                    .success(false)
                    .errorMessage("Payment amount cannot be null.")
                    .build();
        }

        // MoMo Sandbox amount validations
        if (amount.compareTo(new BigDecimal("1000")) < 0 || amount.compareTo(new BigDecimal("50000000")) > 0) {
            log.error("MoMo payment initiation rejected locally: amount {} is out of MoMo Sandbox range (1000 - 50,000,000 VND).", amount);
            return PaymentResponse.builder()
                    .success(false)
                    .errorMessage("Transaction amount must be between 1,000 VND and 50,000,000 VND.")
                    .build();
        }

        // Ensure no fractional part
        if (amount.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
            log.error("MoMo payment initiation rejected locally: amount {} contains a fractional part.", amount);
            return PaymentResponse.builder()
                    .success(false)
                    .errorMessage("Transaction amount must be an integer VND value (no cents/fractions).")
                    .build();
        }

        String requestId = UUID.randomUUID().toString();
        long amountLong = amount.longValueExact();
        String orderId = request.getOrderCode();
        String orderInfo = request.getOrderInfo();
        String redirectUrl = request.getReturnUrl();
        String ipnUrl = request.getNotifyUrl();
        String extraData = "";
        String requestType = "captureWallet";

        // Signature format:
        // accessKey=$accessKey&amount=$amount&extraData=$extraData&ipnUrl=$ipnUrl&orderId=$orderId&orderInfo=$orderInfo&partnerCode=$partnerCode&redirectUrl=$redirectUrl&requestId=$requestId&requestType=$requestType
        String rawSignature = String.format(
                "accessKey=%s&amount=%d&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                accessKey, amountLong, extraData, ipnUrl, orderId, orderInfo, partnerCode, redirectUrl, requestId, requestType
        );

        String signature;
        try {
            log.info("MoMo RAW SIGNATURE: {}", rawSignature);
            signature = HmacUtils.hmacSha256(rawSignature, secretKey);
            log.info("MoMo GENERATED SIGNATURE: {}", signature);
        } catch (Exception e) {
            log.error("Failed to sign MoMo request", e);
            return PaymentResponse.builder()
                    .success(false)
                    .errorMessage("Encryption signing failure.")
                    .build();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("partnerCode", partnerCode);
        body.put("partnerName", "LumiNa");
        body.put("storeId", "LumiNa");
        body.put("requestId", requestId);
        body.put("amount", amountLong);
        body.put("orderId", orderId);
        body.put("orderInfo", orderInfo);
        body.put("redirectUrl", redirectUrl);
        body.put("ipnUrl", ipnUrl);
        body.put("extraData", extraData);
        body.put("requestType", requestType);
        body.put("signature", signature);

        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        RestTemplate restTemplate = new RestTemplate(factory);
        try {
            // Log final amount immediately before sending API request
            log.info("Sending request to MoMo: orderId={}, final amount={}", orderId, amountLong);
            
            // Log generated JSON request sent to MoMo
            try {
                String jsonRequest = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
                log.info("MoMo Request JSON payload: {}", jsonRequest);
            } catch (Exception jsonEx) {
                log.warn("Failed to serialize MoMo request body to JSON for logging", jsonEx);
            }

            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(endpoint, body, Map.class);
            Map<String, Object> responseBody = responseEntity.getBody();

            if (responseBody != null) {
                Number resultCodeNum = (Number) responseBody.get("resultCode");
                int resultCode = resultCodeNum != null ? resultCodeNum.intValue() : -1;
                String payUrl = (String) responseBody.get("payUrl");
                String message = (String) responseBody.get("message");

                if (resultCode == 0 && payUrl != null) {
                    log.info("MoMo initiated successfully, payUrl={}", payUrl);
                    return PaymentResponse.builder()
                            .success(true)
                            .paymentUrl(payUrl)
                            .rawResponse(responseBody.toString())
                            .build();
                } else {
                    log.error("MoMo payment initiation failed: resultCode={}, message={}", resultCode, message);
                    return PaymentResponse.builder()
                            .success(false)
                            .errorMessage(message != null ? message : "Failed with resultCode " + resultCode)
                            .rawResponse(responseBody.toString())
                            .build();
                }
            } else {
                return PaymentResponse.builder()
                        .success(false)
                        .errorMessage("Empty payload from payment gateway.")
                        .build();
            }
        } catch (Exception e) {
            log.error("HTTP gateway exception calling MoMo Sandbox API", e);
            return PaymentResponse.builder()
                    .success(false)
                    .errorMessage("Connection to MoMo Sandbox failed: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public boolean verifyWebhookSignature(Map<String, String> params) {
        String signature = params.get("signature");
        if (signature == null) return false;

        String partnerCode = params.get("partnerCode");
        String accessKey = properties.getMomo().getAccessKey();
        String secretKey = properties.getMomo().getSecretKey();
        
        String amount = params.get("amount");
        String extraData = params.get("extraData");
        String message = params.get("message");
        String orderId = params.get("orderId");
        String orderInfo = params.get("orderInfo");
        String payType = params.get("payType");
        String requestId = params.get("requestId");
        String responseTime = params.get("responseTime");
        String resultCode = params.get("resultCode");
        String transId = params.get("transId");

        String rawSignature = String.format(
                "accessKey=%s&amount=%s&extraData=%s&message=%s&orderId=%s&orderInfo=%s&partnerCode=%s&payType=%s&requestId=%s&responseTime=%s&resultCode=%s&transId=%s",
                accessKey,
                amount != null ? amount : "",
                extraData != null ? extraData : "",
                message != null ? message : "",
                orderId != null ? orderId : "",
                orderInfo != null ? orderInfo : "",
                partnerCode != null ? partnerCode : "",
                payType != null ? payType : "",
                requestId != null ? requestId : "",
                responseTime != null ? responseTime : "",
                resultCode != null ? resultCode : "",
                transId != null ? transId : ""
        );

        try {
            String calculated = HmacUtils.hmacSha256(rawSignature, secretKey);
            return java.security.MessageDigest.isEqual(
                    calculated.toLowerCase().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    signature.toLowerCase().getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Failed to verify signature", e);
            return false;
        }
    }

    @Override
    public PaymentResponse refundPayment(String refundCode, long amount, String originalTransId, String reason) {
        String partnerCode = properties.getMomo().getPartnerCode();
        String accessKey = properties.getMomo().getAccessKey();
        String secretKey = properties.getMomo().getSecretKey();
        String endpoint = properties.getMomo().getEndpoint();

        if (partnerCode == null || accessKey == null || secretKey == null || endpoint == null) {
            return PaymentResponse.builder()
                    .success(false)
                    .errorMessage("MoMo credentials are not configured.")
                    .build();
        }

        String refundEndpoint = endpoint.replace("/create", "/refund");
        String requestId = UUID.randomUUID().toString();

        String rawSignature = String.format(
                "accessKey=%s&amount=%d&description=%s&orderId=%s&partnerCode=%s&requestId=%s&transId=%s",
                accessKey, amount, reason != null ? reason : "", refundCode, partnerCode, requestId, originalTransId
        );

        String signature;
        try {
            signature = HmacUtils.hmacSha256(rawSignature, secretKey);
        } catch (Exception e) {
            return PaymentResponse.builder()
                    .success(false)
                    .errorMessage("Signing failed: " + e.getMessage())
                    .build();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("partnerCode", partnerCode);
        body.put("requestId", requestId);
        body.put("orderId", refundCode);
        body.put("amount", amount);
        body.put("transId", Long.parseLong(originalTransId));
        body.put("description", reason != null ? reason : "");
        body.put("signature", signature);

        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        RestTemplate restTemplate = new RestTemplate(factory);

        try {
            log.info("Sending refund request to MoMo: refundCode={}, amount={}", refundCode, amount);
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(refundEndpoint, body, Map.class);
            Map<String, Object> responseBody = responseEntity.getBody();

            if (responseBody != null) {
                Number resultCodeNum = (Number) responseBody.get("resultCode");
                int resultCode = resultCodeNum != null ? resultCodeNum.intValue() : -1;
                String message = (String) responseBody.get("message");
                
                Number momoTransIdNum = (Number) responseBody.get("transId");
                String providerTransId = momoTransIdNum != null ? momoTransIdNum.toString() : "";

                if (resultCode == 0) {
                    log.info("MoMo refund success: refundCode={}, providerTransId={}", refundCode, providerTransId);
                    return PaymentResponse.builder()
                            .success(true)
                            .errorMessage(providerTransId)
                            .rawResponse(responseBody.toString())
                            .build();
                } else {
                    log.error("MoMo refund failed: resultCode={}, message={}", resultCode, message);
                    return PaymentResponse.builder()
                            .success(false)
                            .errorMessage(message != null ? message : "Failed with code " + resultCode)
                            .rawResponse(responseBody.toString())
                            .build();
                }
            } else {
                return PaymentResponse.builder()
                        .success(false)
                        .errorMessage("Empty response from payment gateway.")
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to perform refund with MoMo", e);
            return PaymentResponse.builder()
                    .success(false)
                    .errorMessage("Connection to MoMo Sandbox failed: " + e.getMessage())
                    .build();
        }
    }
}
