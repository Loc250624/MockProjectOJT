package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.response.AdminTeacherPayoutDTO;
import com.ojtsu26.elearning.model.enums.OrderStatus;
import com.ojtsu26.elearning.repository.OrderItemRepository;
import com.ojtsu26.elearning.repository.projection.AdminTeacherPayoutProjection;
import com.ojtsu26.elearning.service.AdminTeacherPayoutService;
import com.ojtsu26.elearning.service.CurrencyDisplayService;
import com.ojtsu26.elearning.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTeacherPayoutServiceImpl implements AdminTeacherPayoutService {

    private static final String UNPAID_STATUS = "Unpaid";

    private final OrderItemRepository orderItemRepository;
    private final CurrencyDisplayService currencyDisplayService;
    private final SystemSettingService systemSettingService;

    @Override
    @Transactional(readOnly = true)
    public List<AdminTeacherPayoutDTO> findPayoutReadiness(LocalDateTime fromDate, LocalDateTime toDate) {
        BigDecimal teacherShareRate = systemSettingService.getTeacherCommissionRate();
        String currency = currencyDisplayService.getDisplayCurrency();
        return orderItemRepository.findAdminTeacherPayoutReadiness(OrderStatus.PAID, fromDate, toDate)
                .stream()
                .map(row -> toDto(row, teacherShareRate, currency))
                .toList();
    }

    private AdminTeacherPayoutDTO toDto(AdminTeacherPayoutProjection row,
                                        BigDecimal teacherShareRate,
                                        String currency) {
        BigDecimal eligibleRevenue = currencyDisplayService.convertUsdToDisplay(row.getEligibleRevenue());
        BigDecimal teacherShare = currencyDisplayService.money(eligibleRevenue.multiply(teacherShareRate), currency);
        return AdminTeacherPayoutDTO.builder()
                .teacherId(row.getTeacherId())
                .teacherName(row.getTeacherName())
                .teacherEmail(row.getTeacherEmail())
                .eligibleRevenue(eligibleRevenue)
                .teacherShare(teacherShare)
                .currency(currency)
                .status(UNPAID_STATUS)
                .paidOrderCount(row.getPaidOrderCount())
                .latestEligiblePaymentAt(row.getLatestEligiblePaymentAt())
                .build();
    }
}
