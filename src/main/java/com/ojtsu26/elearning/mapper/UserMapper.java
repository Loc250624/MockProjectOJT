package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.dto.request.UserRequestDTO;
import com.ojtsu26.elearning.dto.response.UserResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {


    UserResponseDTO toDto(User entity);


    User toEntity(UserRequestDTO dto);
}
