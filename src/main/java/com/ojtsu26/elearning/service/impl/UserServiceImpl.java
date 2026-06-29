package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.dto.request.UserRequestDTO;
import com.ojtsu26.elearning.dto.response.UserResponseDTO;
import com.ojtsu26.elearning.mapper.UserMapper;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDTO findById(Integer id) {
        User entity = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userMapper.toDto(entity);
    }

    @Override
    public UserResponseDTO create(UserRequestDTO requestDTO) {
        User entity = userMapper.toEntity(requestDTO);
        User saved = userRepository.save(entity);
        return userMapper.toDto(saved);
    }

    @Override
    public UserResponseDTO update(Integer id, UserRequestDTO requestDTO) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        User entity = userMapper.toEntity(requestDTO);
        entity.setId(id);
        User updated = userRepository.save(entity);
        return userMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        userRepository.deleteById(id);
    }
}
