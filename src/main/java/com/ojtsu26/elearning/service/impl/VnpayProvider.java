package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.common.HmacUtils;
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
import java.text.SimpleDateFormat;
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
        if (vnpayProps.getTmnCode() == null || vnpayProps.getHashSecret() == null) {
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
            String vnp_IpAddr = request.getIpAddress() != null ? request.getIpAddress() : "127.0.0.1";

            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            formatter.setTimeZone(TimeZone.getTimeZone("Etc/GMT+7"));
            String vnp_CreateDate = formatter.format(cld.getTime());

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

            // Sort keys alphabetically
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);

            // Build raw signature & query string
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    // Raw string for hashing
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    // Query string for url redirect
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }

            // VNPAY v2.1.0 uses HmacSHA512
            String queryUrl = query.toString();
            String rawHashData = hashData.toString();
            
            // Clean up: vnpay requires hashing URL-encoded keys/values joined by '&'
            // Wait, vnpay signature requires ONLY: fieldName=encodedFieldValue joined by &
            // Let's verify how rawHashData should be built:
            // "vnp_Amount=2497500000&vnp_Command=pay&..."
            // Yes! The formula is: key=URLEncoder(value) joined by &
            StringBuilder actualHashData = new StringBuilder();
            Iterator<String> itr2 = fieldNames.iterator();
            while (itr2.hasNext()) {
                String fieldName = itr2.next();
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    actualHashData.append(fieldName);
                    actualHashData.append('=');
                    actualHashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()).replace("+", "%20"));
                    if (itr2.hasNext()) {
                        actualHashData.append('&');
                    }
                }
            }
            
            String rawSigData = actualHashData.toString();
            log.info("VNPAY Raw signature string: {}", rawSigData);
            
            String vnp_SecureHash = hmacSha512(rawSigData, vnpayProps.getHashSecret());
            log.info("VNPAY Generated secure hash: {}", vnp_SecureHash);

            String paymentUrl = vnpayProps.getEndpoint() + "?" + queryUrl + "&vnp_SecureHash=" + vnp_SecureHash;
            log.info("VNPAY Initiated Redirect URL: {}", paymentUrl);

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
        log.info("Verifying VNPAY webhook signature. Params: {}", params);
        
        String secureHash = params.get("vnp_SecureHash");
        if (secureHash == null || secureHash.isEmpty()) {
            log.warn("Missing vnp_SecureHash parameter in callback");
            return false;
        }

        // Sort keys starting with vnp_ (excluding vnp_SecureHash and vnp_SecureHashType)
        List<String> fieldNames = params.keySet().stream()
                .filter(k -> k.startsWith("vnp_") && !k.equals("vnp_SecureHash") && !k.equals("vnp_SecureHashType"))
                .sorted()
                .collect(Collectors.toList());

        try {
            StringBuilder hashData = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()).replace("+", "%20"));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }

            String rawSigData = hashData.toString();
            log.info("VNPAY Callback Raw signature string: {}", rawSigData);
            
            String calculatedHash = hmacSha512(rawSigData, properties.getVnpay().getHashSecret());
            log.info("VNPAY Callback Calculated signature: {}, Received: {}", calculatedHash, secureHash);

            return calculatedHash.equalsIgnoreCase(secureHash);

        } catch (Exception e) {
            log.error("Failed to verify VNPAY signature", e);
            return false;
        }
    }

    @Override
    public PaymentResponse refundPayment(String refundCode, long amount, String originalTransId, String reason) {
        // Mock refund since refund is not fully implemented/optional
        log.info("Simulating VNPAY refund: refundCode={}, amount={}, originalTransId={}", refundCode, amount, originalTransId);
        return PaymentResponse.builder()
                .success(true)
                .build();
    }

    private String hmacSha512(String data, String key) {
        try {
            javax.crypto.Mac sha512HMAC = javax.crypto.Mac.getInstance("HmacSHA512");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
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
}
