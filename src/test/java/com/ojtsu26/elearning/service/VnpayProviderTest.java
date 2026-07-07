package com.ojtsu26.elearning.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ojtsu26.elearning.config.PaymentGatewayProperties;
import com.ojtsu26.elearning.dto.request.PaymentRequest;
import com.ojtsu26.elearning.dto.response.PaymentResponse;
import com.ojtsu26.elearning.model.enums.PaymentMethod;
import com.ojtsu26.elearning.service.impl.VnpayProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class VnpayProviderTest {

    @Mock
    private PaymentGatewayProperties properties;

    @InjectMocks
    private VnpayProvider vnpayProvider;

    private PaymentGatewayProperties.VnpayProperties vnpayProperties;

    @BeforeEach
    void setUp() {
        vnpayProperties = new PaymentGatewayProperties.VnpayProperties();
        vnpayProperties.setTmnCode("2QXUIB0A");
        vnpayProperties.setHashSecret("MSBUNHXVQPHFLNNEUJWTLKJZPEXXWJCT");
        vnpayProperties.setEndpoint("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        vnpayProperties.setReturnUrl("http://localhost:8080/student/payment-result");
        vnpayProperties.setIpnUrl("http://localhost:8080/api/payment/vnpay-ipn");

        lenient().when(properties.getVnpay()).thenReturn(vnpayProperties);
    }

    @Test
    void getMethod_ReturnsVnpay() {
        assertEquals(PaymentMethod.VNPAY, vnpayProvider.getMethod());
    }

    @Test
    void initiatePayment_MissingConfigs_ReturnsError() {
        // Arrange
        vnpayProperties.setTmnCode(null);

        PaymentRequest request = PaymentRequest.builder()
                .orderCode("ORD123")
                .amount(new BigDecimal("250000"))
                .orderInfo("Test course")
                .returnUrl("http://return")
                .notifyUrl("http://notify")
                .build();

        // Act
        PaymentResponse response = vnpayProvider.initiatePayment(request);

        // Assert
        assertFalse(response.isSuccess());
        assertTrue(response.getErrorMessage().contains("configuration missing"));
    }

    @Test
    void initiatePayment_Success_GeneratesRedirectUrl() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
                .orderCode("ORD12345")
                .amount(new BigDecimal("100000")) // 100,000 VND
                .orderInfo("Purchase React Advanced")
                .returnUrl("http://localhost:8080/student/payment-result")
                .notifyUrl("http://localhost:8080/api/payment/vnpay-ipn")
                .ipAddress("127.0.0.1")
                .build();

        // Act
        PaymentResponse response = vnpayProvider.initiatePayment(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getPaymentUrl());
        assertTrue(response.getPaymentUrl().startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"));
        assertTrue(response.getPaymentUrl().contains("vnp_TmnCode=2QXUIB0A"));
        assertTrue(response.getPaymentUrl().contains("vnp_TxnRef=ORD12345"));
        assertTrue(response.getPaymentUrl().contains("vnp_Amount=10000000")); // 100,000 VND * 100
        assertTrue(response.getPaymentUrl().contains("vnp_SecureHash="));
    }

    @Test
    void verifyWebhookSignature_ValidSignature_ReturnsTrue() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", "2QXUIB0A");
        params.put("vnp_Amount", "10000000");
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", "ORD12345");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "12345678");
        // A computed signature for the above parameters using key "MSBUNHXVQPHFLNNEUJWTLKJZPEXXWJCT"
        // Raw string to sign: vnp_Amount=10000000&vnp_CurrCode=VND&vnp_ResponseCode=00&vnp_TmnCode=2QXUIB0A&vnp_TransactionNo=12345678&vnp_TxnRef=ORD12345
        String rawSig = "vnp_Amount=10000000&vnp_CurrCode=VND&vnp_ResponseCode=00&vnp_TmnCode=2QXUIB0A&vnp_TransactionNo=12345678&vnp_TxnRef=ORD12345";
        String secureHash = calculateHmacSha512(rawSig, "MSBUNHXVQPHFLNNEUJWTLKJZPEXXWJCT");
        params.put("vnp_SecureHash", secureHash);

        // Act
        boolean result = vnpayProvider.verifyWebhookSignature(params);

        // Assert
        assertTrue(result);
    }

    @Test
    void verifyWebhookSignature_InvalidSignature_ReturnsFalse() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", "2QXUIB0A");
        params.put("vnp_Amount", "10000000");
        params.put("vnp_SecureHash", "invalidhashvalue");

        // Act
        boolean result = vnpayProvider.verifyWebhookSignature(params);

        // Assert
        assertFalse(result);
    }

    private String calculateHmacSha512(String data, String key) {
        try {
            javax.crypto.Mac sha512HMAC = javax.crypto.Mac.getInstance("HmacSHA512");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA512");
            sha512HMAC.init(secretKey);
            byte[] rawHmac = sha512HMAC.doFinal(data.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : rawHmac) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
