package com.ojtsu26.elearning.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ojtsu26.elearning.config.PaymentGatewayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
public class CurrencyConversionServiceImplTest {

    @Mock
    private PaymentGatewayProperties paymentGatewayProperties;

    @InjectMocks
    private CurrencyConversionServiceImpl currencyConversionService;

    @BeforeEach
    void setUp() {
        // Default exchange rate mock: 1 USD = 25000 VND
        lenient().when(paymentGatewayProperties.getExchangeRate()).thenReturn(new BigDecimal("25000"));
    }

    @Test
    void convertUsdToVnd_IntegerPrice_Success() {
        // Arrange
        BigDecimal usdAmount = new BigDecimal("100");

        // Act
        BigDecimal result = currencyConversionService.convertUsdToVnd(usdAmount);

        // Assert
        assertEquals(new BigDecimal("2500000"), result);
    }

    @Test
    void convertUsdToVnd_DecimalPrice_RoundUp() {
        // Arrange: 99.99 USD * 25000 = 2499750 VND
        BigDecimal usdAmount = new BigDecimal("99.99");

        // Act
        BigDecimal result = currencyConversionService.convertUsdToVnd(usdAmount);

        // Assert
        assertEquals(new BigDecimal("2499750"), result);
    }

    @Test
    void convertUsdToVnd_DecimalPrice_RoundDown() {
        // Arrange: 99.994 USD * 25000 = 2499850 VND
        // Wait, let's test rounding behavior with specific fractional values
        // E.g., at exchange rate 25000:
        // 0.00001 USD * 25000 = 0.25 -> rounds to 0
        // 0.00002 USD * 25000 = 0.50 -> rounds to 1 (HALF_UP)
        // Let's use exchange rate 25000 and different inputs:
        // 0.000018 USD -> 0.45 -> rounds to 0
        // 0.000022 USD -> 0.55 -> rounds to 1
        BigDecimal lowAmount = new BigDecimal("0.000018");
        BigDecimal highAmount = new BigDecimal("0.000022");

        // Act
        BigDecimal lowResult = currencyConversionService.convertUsdToVnd(lowAmount);
        BigDecimal highResult = currencyConversionService.convertUsdToVnd(highAmount);

        // Assert
        assertEquals(BigDecimal.ZERO, lowResult);
        assertEquals(BigDecimal.ONE, highResult);
    }

    @Test
    void convertUsdToVnd_NullAmount_ReturnsZero() {
        // Act
        BigDecimal result = currencyConversionService.convertUsdToVnd(null);

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void convertUsdToVnd_ZeroAmount_ReturnsZero() {
        // Act
        BigDecimal result = currencyConversionService.convertUsdToVnd(BigDecimal.ZERO);

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void getExchangeRate_ReturnsConfiguredRate() {
        // Act
        BigDecimal rate = currencyConversionService.getExchangeRate();

        // Assert
        assertEquals(new BigDecimal("25000"), rate);
    }

    @Test
    void convertUsdToVnd_NegativeExchangeRate_HandlesCorrectly() {
        // Arrange
        when(paymentGatewayProperties.getExchangeRate()).thenReturn(new BigDecimal("-25000"));
        BigDecimal usdAmount = new BigDecimal("10");

        // Act
        BigDecimal result = currencyConversionService.convertUsdToVnd(usdAmount);

        // Assert
        assertEquals(new BigDecimal("-250000"), result);
    }

    @Test
    void convertUsdToVnd_ZeroExchangeRate_ReturnsZero() {
        // Arrange
        when(paymentGatewayProperties.getExchangeRate()).thenReturn(BigDecimal.ZERO);
        BigDecimal usdAmount = new BigDecimal("100");

        // Act
        BigDecimal result = currencyConversionService.convertUsdToVnd(usdAmount);

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }
}
