package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.Transaction;
import com.ojtsu26.elearning.dto.request.TransactionRequestDTO;
import com.ojtsu26.elearning.dto.response.TransactionResponseDTO;
import com.ojtsu26.elearning.mapper.TransactionMapper;
import com.ojtsu26.elearning.repository.TransactionRepository;
import com.ojtsu26.elearning.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
}
