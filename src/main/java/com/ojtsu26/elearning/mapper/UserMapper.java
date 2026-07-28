package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.dto.request.UserRequestDTO;
import com.ojtsu26.elearning.dto.response.UserResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {


    UserResponseDTO toDto(User entity);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "providerId", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "roadmaps", ignore = true)
    @Mapping(target = "courses", ignore = true)
    @Mapping(target = "courseenrollments", ignore = true)
    @Mapping(target = "submissions", ignore = true)
    @Mapping(target = "certificates", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    User toEntity(UserRequestDTO dto);
}
