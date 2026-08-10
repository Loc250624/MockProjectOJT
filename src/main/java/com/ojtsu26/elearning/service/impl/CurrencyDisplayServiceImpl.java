package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.repository.SystemSettingRepository;
import com.ojtsu26.elearning.service.CurrencyConversionService;
import com.ojtsu26.elearning.service.CurrencyDisplayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CurrencyDisplayServiceImpl implements CurrencyDisplayService {

    private static final String SETTING_KEY = "commerce.currency";

    private final SystemSettingRepository systemSettingRepository;
    private final CurrencyConversionService currencyConversionService;

    @Override
    @Transactional(readOnly = true)
    public String getDisplayCurrency() {
        return systemSettingRepository.findByKey(SETTING_KEY)
                .map(setting -> normalizeCurrency(setting.getValue()))
                .orElse(DEFAULT_DISPLAY_CURRENCY);
    }

    @Override
    public BigDecimal convertUsdToDisplay(BigDecimal usdAmount) {
        BigDecimal amount = usdAmount == null ? BigDecimal.ZERO : usdAmount;
        String currency = getDisplayCurrency();
        if ("VND".equals(currency)) {
            return money(currencyConversionService.convertUsdToVnd(amount), currency);
        }
        return money(amount, currency);
    }

    @Override
    public BigDecimal money(BigDecimal value, String currency) {
        int scale = "VND".equals(normalizeCurrency(currency)) ? 0 : 2;
        return (value == null ? BigDecimal.ZERO : value).setScale(scale, RoundingMode.HALF_UP);
    }

    @Override
    public String formatUsdForDisplay(BigDecimal usdAmount) {
        String currency = getDisplayCurrency();
        BigDecimal displayAmount = convertUsdToDisplay(usdAmount);
        return format(displayAmount, currency);
    }

    @Override
    public String formatDisplayMoney(BigDecimal displayAmount) {
        String currency = getDisplayCurrency();
        return format(displayAmount, currency);
    }

    private String format(BigDecimal amount, String currency) {
        BigDecimal displayAmount = money(amount, currency);
        if ("VND".equals(currency)) {
            NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
            formatter.setMinimumFractionDigits(0);
            formatter.setMaximumFractionDigits(0);
            return formatter.format(displayAmount) + " VND";
        }
        NumberFormat formatter = NumberFormat.getCurrencyInstance("VND".equals(currency)
                ? Locale.forLanguageTag("vi-VN")
                : Locale.US);
        formatter.setCurrency(java.util.Currency.getInstance(currency));
        formatter.setMinimumFractionDigits("VND".equals(currency) ? 0 : 2);
        formatter.setMaximumFractionDigits("VND".equals(currency) ? 0 : 2);
        return formatter.format(displayAmount);
    }

    private String normalizeCurrency(String currency) {
        if (currency == null) {
            return DEFAULT_DISPLAY_CURRENCY;
        }
        String clean = currency.trim().toUpperCase(Locale.ROOT);
        return "USD".equals(clean) || "VND".equals(clean) ? clean : DEFAULT_DISPLAY_CURRENCY;
    }
}
