package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.AdminPaymentSummaryDTO;
import com.ojtsu26.elearning.dto.request.TransactionRequestDTO;
import com.ojtsu26.elearning.dto.response.AdminTransactionDTO;
import com.ojtsu26.elearning.dto.response.AdminTransactionDetailDTO;
import com.ojtsu26.elearning.dto.response.TransactionResponseDTO;
import com.ojtsu26.elearning.model.entity.Transaction;
import com.ojtsu26.elearning.model.enums.PaymentMethod;
import com.ojtsu26.elearning.model.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {
    List<TransactionResponseDTO> findAll();
    TransactionResponseDTO findById(Integer id);
    TransactionResponseDTO create(TransactionRequestDTO requestDTO);
    TransactionResponseDTO update(Integer id, TransactionRequestDTO requestDTO);
    void delete(Integer id);
    Page<Transaction> searchTransactions(String keyword, Pageable pageable);
    Page<Transaction> searchTransactions(String keyword, LocalDateTime fromDate,
                                         LocalDateTime toDate, Pageable pageable);
    List<Transaction> findTransactionsForAdminPaymentsExport(String keyword, LocalDateTime fromDate,
                                                             LocalDateTime toDate, Sort sort);
    Page<AdminTransactionDTO> searchAdminTransactions(String keyword, TransactionStatus status,
                                                      PaymentMethod paymentMethod, LocalDateTime fromDate,
                                                      LocalDateTime toDate, Pageable pageable);
    AdminTransactionDetailDTO findAdminTransactionDetail(Integer id);
    AdminPaymentSummaryDTO getAdminPaymentSummary();
    AdminPaymentSummaryDTO getAdminPaymentSummary(LocalDateTime fromDate, LocalDateTime toDate);
}
