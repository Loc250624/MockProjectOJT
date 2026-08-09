package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.response.AdminPaymentSummaryDTO;
import com.ojtsu26.elearning.dto.response.AdminTransactionDTO;
import com.ojtsu26.elearning.dto.response.AdminTransactionDetailDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.Order;
import com.ojtsu26.elearning.model.entity.Transaction;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.dto.request.TransactionRequestDTO;
import com.ojtsu26.elearning.dto.response.TransactionResponseDTO;
import com.ojtsu26.elearning.mapper.TransactionMapper;
import com.ojtsu26.elearning.model.enums.PaymentMethod;
import com.ojtsu26.elearning.model.enums.TransactionStatus;
import com.ojtsu26.elearning.repository.TransactionRepository;
import com.ojtsu26.elearning.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Override
    public List<TransactionResponseDTO> findAll() {
        return transactionRepository.findAll().stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public TransactionResponseDTO findById(Integer id) {
        Transaction entity = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        return transactionMapper.toDto(entity);
    }

    @Override
    public TransactionResponseDTO create(TransactionRequestDTO requestDTO) {
        Transaction entity = transactionMapper.toEntity(requestDTO);
        Transaction saved = transactionRepository.save(entity);
        return transactionMapper.toDto(saved);
    }

    @Override
    public TransactionResponseDTO update(Integer id, TransactionRequestDTO requestDTO) {
        if (!transactionRepository.existsById(id)) {
            throw new RuntimeException("Transaction not found");
        }
        Transaction entity = transactionMapper.toEntity(requestDTO);
        entity.setId(id);
        Transaction updated = transactionRepository.save(entity);
        return transactionMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        transactionRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Transaction> searchTransactions(String keyword, Pageable pageable) {
        String cleanKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return transactionRepository.searchTransactions(cleanKeyword, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminTransactionDTO> searchAdminTransactions(String keyword, TransactionStatus status,
                                                             PaymentMethod paymentMethod, LocalDateTime fromDate,
                                                             LocalDateTime toDate, Pageable pageable) {
        String cleanKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return transactionRepository.searchAdminTransactions(cleanKeyword, status, paymentMethod, fromDate, toDate, pageable)
                .map(this::toAdminTransactionDto);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminTransactionDetailDTO findAdminTransactionDetail(Integer id) {
        Transaction transaction = transactionRepository.findAdminDetailById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Transaction not found"));
        return toAdminTransactionDetailDto(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPaymentSummaryDTO getAdminPaymentSummary() {
        return AdminPaymentSummaryDTO.builder()
                .totalTransactions(transactionRepository.count())
                .successfulTransactions(transactionRepository.countByStatus(TransactionStatus.SUCCESS))
                .pendingTransactions(transactionRepository.countByStatus(TransactionStatus.PENDING))
                .failedTransactions(transactionRepository.countByStatus(TransactionStatus.FAILED))
                .refundedTransactions(transactionRepository.countByStatus(TransactionStatus.REFUNDED))
                .successfulAmount(money(transactionRepository.sumAmountByStatus(TransactionStatus.SUCCESS)))
                .build();
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private AdminTransactionDTO toAdminTransactionDto(Transaction transaction) {
        User student = transaction.getStudent();
        Course course = transaction.getCourse();
        Order order = transaction.getOrder();

        return AdminTransactionDTO.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .currency("VND")
                .paymentMethod(transaction.getPaymentMethod())
                .transactionRef(transaction.getTransactionRef())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .studentId(student == null ? null : student.getId())
                .studentName(student == null ? null : student.getFullName())
                .studentEmail(student == null ? null : student.getEmail())
                .courseId(course == null ? null : course.getId())
                .courseTitle(course == null ? null : course.getTitle())
                .orderId(order == null ? null : order.getId())
                .orderCode(order == null ? null : order.getOrderCode())
                .build();
    }

    private AdminTransactionDetailDTO toAdminTransactionDetailDto(Transaction transaction) {
        User student = transaction.getStudent();
        Course course = transaction.getCourse();
        Order order = transaction.getOrder();

        return AdminTransactionDetailDTO.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .currency("VND")
                .paymentMethod(transaction.getPaymentMethod())
                .transactionRef(transaction.getTransactionRef())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .studentId(student == null ? null : student.getId())
                .studentName(student == null ? null : student.getFullName())
                .studentEmail(student == null ? null : student.getEmail())
                .courseId(course == null ? null : course.getId())
                .courseTitle(course == null ? null : course.getTitle())
                .orderId(order == null ? null : order.getId())
                .orderCode(order == null ? null : order.getOrderCode())
                .orderTotalAmount(order == null ? null : order.getPaidAmount())
                .orderPaidAmount(order == null ? null : order.getPaidAmount())
                .build();
    }
}
