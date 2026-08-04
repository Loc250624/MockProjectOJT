package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.config.PaymentGatewayProperties;
import com.ojtsu26.elearning.dto.request.PaymentRequest;
import com.ojtsu26.elearning.dto.response.PaymentResponse;
import com.ojtsu26.elearning.model.enums.PaymentMethod;
import com.ojtsu26.elearning.service.PaymentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VnpayProvider implements PaymentProvider {

    private final PaymentGatewayProperties properties;

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.VNPAY;
    }

    @Override
    public PaymentResponse initiatePayment(PaymentRequest request) {
        log.info("Initiating VNPAY payment for order: {}, amount: {}", request.getOrderCode(), request.getAmount());

        PaymentGatewayProperties.VnpayProperties vnpayProps = properties.getVnpay();
        if (isBlank(vnpayProps.getTmnCode())
                || isBlank(vnpayProps.getHashSecret())
                || isBlank(vnpayProps.getEndpoint())
                || isBlank(request.getReturnUrl())) {
            log.error("VNPAY Sandbox configurations are missing");
            return PaymentResponse.builder()
                    .success(false)
                    .errorMessage("VNPAY Sandbox configuration missing.")
                    .build();
        }

        try {
            String vnp_Version = "2.1.0";
            String vnp_Command = "pay";
            String vnp_TmnCode = vnpayProps.getTmnCode();

            // VNPAY amount is multiplied by 100 (in minor unit/cents)
            long amountCent = request.getAmount().multiply(new BigDecimal("100")).longValue();
            String vnp_Amount = String.valueOf(amountCent);

            String vnp_CurrCode = "VND";
            String vnp_TxnRef = request.getOrderCode();
            String vnp_OrderInfo = request.getOrderInfo();
            String vnp_OrderType = "other";
            String vnp_Locale = "vn";
            String vnp_ReturnUrl = request.getReturnUrl();
            String vnp_IpAddr = normalizeIpAddress(request.getIpAddress());

            ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            LocalDateTime createTime = LocalDateTime.now(vietnamZone);
            String vnp_CreateDate = createTime.format(formatter);
            String vnp_ExpireDate = createTime.plusMinutes(15).format(formatter);

            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount", vnp_Amount);
            vnp_Params.put("vnp_CurrCode", vnp_CurrCode);
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
            vnp_Params.put("vnp_OrderType", vnp_OrderType);
            vnp_Params.put("vnp_Locale", vnp_Locale);
            vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

            // Sort keys alphabetically
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);

            // Build the query string and signature input from the exact same
            // UTF-8 encoded values. VNPAY expects spaces encoded as '+'.
            StringJoiner hashData = new StringJoiner("&");
            StringJoiner query = new StringJoiner("&");
            for (String fieldName : fieldNames) {
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    String encodedValue = urlEncode(fieldValue);
                    hashData.add(fieldName + "=" + encodedValue);
                    query.add(urlEncode(fieldName) + "=" + encodedValue);
                }
            }

            // VNPAY v2.1.0 uses HmacSHA512
            String queryUrl = query.toString();
            String rawSigData = hashData.toString();

            String vnp_SecureHash = hmacSha512(rawSigData, vnpayProps.getHashSecret());
            log.debug("VNPAY request signed: orderCode={}, signatureLength={}",
                    request.getOrderCode(), vnp_SecureHash.length());

            String separator = vnpayProps.getEndpoint().contains("?") ? "&" : "?";
            String paymentUrl = vnpayProps.getEndpoint() + separator + queryUrl
                    + "&vnp_SecureHash=" + vnp_SecureHash;
            log.info("VNPAY initiated redirect for order: {}", request.getOrderCode());

            return PaymentResponse.builder()
                    .success(true)
                    .paymentUrl(paymentUrl)
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate VNPAY redirect URL", e);
            return PaymentResponse.builder()
                    .success(false)
                    .errorMessage("Error initiating VNPAY payment gateway.")
                    .build();
        }
    }

    @Override
    public boolean verifyWebhookSignature(Map<String, String> params) {
        log.info("Verifying VNPAY webhook signature for order: {}", params.get("vnp_TxnRef"));

        String secureHash = params.get("vnp_SecureHash");
        if (secureHash == null || secureHash.isEmpty()) {
            log.warn("Missing vnp_SecureHash parameter in callback");
            return false;
        }

        // Sort keys starting with vnp_ (excluding vnp_SecureHash and
        // vnp_SecureHashType)
        List<String> fieldNames = params.keySet().stream()
                .filter(k -> k.startsWith("vnp_") && !k.equals("vnp_SecureHash") && !k.equals("vnp_SecureHashType"))
                .sorted()
                .collect(Collectors.toList());

        try {
            StringJoiner hashData = new StringJoiner("&");
            for (String fieldName : fieldNames) {
                String fieldValue = params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.add(fieldName + "=" + urlEncode(fieldValue));
                }
            }

            String rawSigData = hashData.toString();

            String calculatedHash = hmacSha512(rawSigData, properties.getVnpay().getHashSecret());
            log.debug("VNPAY callback signature compared: order={}, providedHashLength={}, calculatedHashLength={}",
                    params.get("vnp_TxnRef"), secureHash.length(), calculatedHash.length());

            return calculatedHash.equalsIgnoreCase(secureHash);

        } catch (Exception e) {
            log.error("Failed to verify VNPAY signature", e);
            return false;
        }
    }

    @Override
    public PaymentResponse refundPayment(String refundCode, long amount, String originalTransId, String reason) {
        // Mock refund since refund is not fully implemented/optional
        log.info("Simulating VNPAY refund: refundCode={}, amount={}, originalTransId={}", refundCode, amount,
                originalTransId);
        return PaymentResponse.builder()
                .success(true)
                .build();
    }

    private String hmacSha512(String data, String key) {
        try {
            javax.crypto.Mac sha512HMAC = javax.crypto.Mac.getInstance("HmacSHA512");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512HMAC.init(secretKey);
            byte[] rawHmac = sha512HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : rawHmac) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC-SHA512", e);
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String normalizeIpAddress(String ipAddress) {
        if (isBlank(ipAddress)
                || "::1".equals(ipAddress)
                || "0:0:0:0:0:0:0:1".equals(ipAddress)) {
            return "127.0.0.1";
        }

        String firstAddress = ipAddress.contains(",")
                ? ipAddress.substring(0, ipAddress.indexOf(',')).trim()
                : ipAddress.trim();
        return firstAddress.contains(":") ? "127.0.0.1" : firstAddress;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
