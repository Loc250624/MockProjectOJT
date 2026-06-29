package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.CommentRequestDTO;
import com.ojtsu26.elearning.dto.response.CommentResponseDTO;
import java.util.List;

public interface CommentService {
    List<CommentResponseDTO> findAll();
    CommentResponseDTO findById(Integer id);
    CommentResponseDTO create(CommentRequestDTO requestDTO);
    CommentResponseDTO update(Integer id, CommentRequestDTO requestDTO);
    void delete(Integer id);
}
