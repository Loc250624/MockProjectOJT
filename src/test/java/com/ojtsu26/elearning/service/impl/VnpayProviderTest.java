package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.config.PaymentGatewayProperties;
import com.ojtsu26.elearning.dto.request.PaymentRequest;
import com.ojtsu26.elearning.dto.response.PaymentResponse;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VnpayProviderTest {

    private static final String HASH_SECRET = "local-unit-test-hash-secret";

    @Test
    void createsCanonicalSignedSandboxUrlForLocalhost() throws Exception {
        PaymentGatewayProperties properties = new PaymentGatewayProperties();
        properties.getVnpay().setTmnCode("TESTCODE");
        properties.getVnpay().setHashSecret(HASH_SECRET);
        properties.getVnpay().setEndpoint("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");

        VnpayProvider provider = new VnpayProvider(properties);
        PaymentResponse response = provider.initiatePayment(PaymentRequest.builder()
                .orderCode("ORD-LOCAL-1")
                .amount(new BigDecimal("10000"))
                .orderInfo("Purchase course: Java Basics")
                .returnUrl("http://localhost:8080/student/payment-result")
                .ipAddress("0:0:0:0:0:0:0:1")
                .build());

        assertTrue(response.isSuccess());

        String rawQuery = URI.create(response.getPaymentUrl()).getRawQuery();
        int secureHashSeparator = rawQuery.lastIndexOf("&vnp_SecureHash=");
        String signedData = rawQuery.substring(0, secureHashSeparator);
        String suppliedHash = rawQuery.substring(secureHashSeparator + "&vnp_SecureHash=".length());

        assertTrue(signedData.contains(
                "vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A8080%2Fstudent%2Fpayment-result"));
        assertTrue(signedData.contains("vnp_IpAddr=127.0.0.1"));
        assertTrue(signedData.contains("vnp_ExpireDate="));
        assertTrue(signedData.contains("vnp_OrderInfo=Purchase+course%3A+Java+Basics"));
        assertEquals(hmacSha512(HASH_SECRET, signedData), suppliedHash);
    }

    private String hmacSha512(String key, String data) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA512");
        hmac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder result = new StringBuilder();
        for (byte value : bytes) {
            result.append(String.format("%02x", value));
        }
        return result.toString();
    }
}
