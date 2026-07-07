package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.config.PaymentGatewayProperties;
import com.ojtsu26.elearning.service.CurrencyConversionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Centralized USD→VND conversion service.
 * Reads the exchange rate from application.properties via PaymentGatewayProperties.
 * All payment-related currency conversions should go through this service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyConversionServiceImpl implements CurrencyConversionService {

    private final PaymentGatewayProperties paymentGatewayProperties;

    /**
     * Convert a USD amount to VND.
     * Result is rounded to the nearest integer because MoMo (and most VND gateways)
     * only accept whole-number VND amounts.
     *
     * Example: 99.00 USD × 25000 = 2,475,000 VND
     *
     * @param usdAmount amount in USD (Course.price)
     * @return integer VND amount for the payment gateway
     */
    @Override
    public BigDecimal convertUsdToVnd(BigDecimal usdAmount) {
        if (usdAmount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal exchangeRate = paymentGatewayProperties.getExchangeRate();
        BigDecimal vndAmount = usdAmount.multiply(exchangeRate).setScale(0, RoundingMode.HALF_UP);
        log.debug("Currency conversion: {} USD × {} = {} VND", usdAmount, exchangeRate, vndAmount);
        return vndAmount;
    }

    @Override
    public BigDecimal getExchangeRate() {
        return paymentGatewayProperties.getExchangeRate();
    }
}
