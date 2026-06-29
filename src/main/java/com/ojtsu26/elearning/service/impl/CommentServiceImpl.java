package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.Comment;
import com.ojtsu26.elearning.dto.request.CommentRequestDTO;
import com.ojtsu26.elearning.dto.response.CommentResponseDTO;
import com.ojtsu26.elearning.mapper.CommentMapper;
import com.ojtsu26.elearning.repository.CommentRepository;
import com.ojtsu26.elearning.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;

    @Override
    public List<CommentResponseDTO> findAll() {
        return commentRepository.findAll().stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CommentResponseDTO findById(Integer id) {
        Comment entity = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        return commentMapper.toDto(entity);
    }

    @Override
    public CommentResponseDTO create(CommentRequestDTO requestDTO) {
        Comment entity = commentMapper.toEntity(requestDTO);
        Comment saved = commentRepository.save(entity);
        return commentMapper.toDto(saved);
    }

    @Override
    public CommentResponseDTO update(Integer id, CommentRequestDTO requestDTO) {
        if (!commentRepository.existsById(id)) {
            throw new RuntimeException("Comment not found");
        }
        Comment entity = commentMapper.toEntity(requestDTO);
        entity.setId(id);
        Comment updated = commentRepository.save(entity);
        return commentMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        commentRepository.deleteById(id);
    }
}
