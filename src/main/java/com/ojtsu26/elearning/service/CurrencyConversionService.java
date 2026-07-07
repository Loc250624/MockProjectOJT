package com.ojtsu26.elearning.service;

import java.math.BigDecimal;

/**
 * Centralized currency conversion service.
 * All USD→VND (and future currency) conversions must go through this service.
 * Invariant: Course.price is stored in USD. Payment gateways (e.g. MoMo) receive VND.
 */
public interface CurrencyConversionService {

    /**
     * Convert a USD amount to VND using the configured exchange rate.
     * The result is rounded to the nearest integer (MoMo requires integer VND).
     *
     * @param usdAmount amount in USD
     * @return equivalent integer VND amount
     */
    BigDecimal convertUsdToVnd(BigDecimal usdAmount);

    /**
     * Returns the configured exchange rate (VND per 1 USD).
     */
    BigDecimal getExchangeRate();
}
