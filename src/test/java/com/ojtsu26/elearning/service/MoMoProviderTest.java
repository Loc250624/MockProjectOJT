package com.ojtsu26.elearning.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ojtsu26.elearning.config.PaymentGatewayProperties;
import com.ojtsu26.elearning.dto.request.PaymentRequest;
import com.ojtsu26.elearning.dto.response.PaymentResponse;
import com.ojtsu26.elearning.model.enums.PaymentMethod;
import com.ojtsu26.elearning.service.impl.MoMoProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
public class MoMoProviderTest {

    @Mock
    private PaymentGatewayProperties properties;

    @InjectMocks
    private MoMoProvider moMoProvider;

    private PaymentGatewayProperties.MomoProperties momoProperties;

    @BeforeEach
    void setUp() {
        momoProperties = new PaymentGatewayProperties.MomoProperties();
        momoProperties.setPartnerCode("MOMO");
        momoProperties.setAccessKey("accessKey");
        momoProperties.setSecretKey("secretKey");
        momoProperties.setEndpoint("https://test-payment.momo.vn/v2/gateway/api/create");

        lenient().when(properties.getMomo()).thenReturn(momoProperties);
    }

    @Test
    void getMethod_ReturnsMomo() {
        assertEquals(PaymentMethod.MOMO, moMoProvider.getMethod());
    }

    @Test
    void initiatePayment_MissingConfigs_ReturnsError() {
        // Arrange
        momoProperties.setPartnerCode(null);

        PaymentRequest request = PaymentRequest.builder()
                .orderCode("ORD123")
                .amount(new BigDecimal("250000"))
                .orderInfo("Test course")
                .returnUrl("http://return")
                .notifyUrl("http://notify")
                .build();

        // Act
        PaymentResponse response = moMoProvider.initiatePayment(request);

        // Assert
        assertFalse(response.isSuccess());
        assertTrue(response.getErrorMessage().contains("credentials are not configured"));
    }

    @Test
    void initiatePayment_AmountTooLow_ReturnsError() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
                .orderCode("ORD123")
                .amount(new BigDecimal("999")) // Below minimum of 1000 VND
                .orderInfo("Test course")
                .returnUrl("http://return")
                .notifyUrl("http://notify")
                .build();

        // Act
        PaymentResponse response = moMoProvider.initiatePayment(request);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("Transaction amount must be between 1,000 VND and 50,000,000 VND.", response.getErrorMessage());
    }

    @Test
    void initiatePayment_AmountTooHigh_ReturnsError() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
                .orderCode("ORD123")
                .amount(new BigDecimal("50000001")) // Above maximum of 50000000 VND
                .orderInfo("Test course")
                .returnUrl("http://return")
                .notifyUrl("http://notify")
                .build();

        // Act
        PaymentResponse response = moMoProvider.initiatePayment(request);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("Transaction amount must be between 1,000 VND and 50,000,000 VND.", response.getErrorMessage());
    }

    @Test
    void initiatePayment_AmountHasFraction_ReturnsError() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
                .orderCode("ORD123")
                .amount(new BigDecimal("25000.50")) // Contains fractional part
                .orderInfo("Test course")
                .returnUrl("http://return")
                .notifyUrl("http://notify")
                .build();

        // Act
        PaymentResponse response = moMoProvider.initiatePayment(request);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("Transaction amount must be an integer VND value (no cents/fractions).", response.getErrorMessage());
    }
}
