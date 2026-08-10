package com.ojtsu26.elearning.service;

import java.math.BigDecimal;

public interface CurrencyDisplayService {
    String BUSINESS_CURRENCY = "USD";
    String DEFAULT_DISPLAY_CURRENCY = "VND";

    String getDisplayCurrency();
    BigDecimal convertUsdToDisplay(BigDecimal usdAmount);
    BigDecimal money(BigDecimal value, String currency);
    String formatUsdForDisplay(BigDecimal usdAmount);
    String formatDisplayMoney(BigDecimal displayAmount);
}
