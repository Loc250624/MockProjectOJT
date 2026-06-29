package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.TransactionRequestDTO;
import com.ojtsu26.elearning.dto.response.TransactionResponseDTO;
import java.util.List;

public interface TransactionService {
    List<TransactionResponseDTO> findAll();
    TransactionResponseDTO findById(Integer id);
    TransactionResponseDTO create(TransactionRequestDTO requestDTO);
    TransactionResponseDTO update(Integer id, TransactionRequestDTO requestDTO);
    void delete(Integer id);
}
