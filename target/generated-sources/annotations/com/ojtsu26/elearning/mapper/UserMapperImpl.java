package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.dto.request.UserRequestDTO;
import com.ojtsu26.elearning.dto.response.UserResponseDTO;
import com.ojtsu26.elearning.model.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-30T09:33:44+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponseDTO toDto(User entity) {
        if ( entity == null ) {
            return null;
        }

        UserResponseDTO userResponseDTO = new UserResponseDTO();

        userResponseDTO.setId( entity.getId() );
        userResponseDTO.setFullName( entity.getFullName() );
        userResponseDTO.setEmail( entity.getEmail() );
        userResponseDTO.setPasswordHash( entity.getPasswordHash() );
        userResponseDTO.setAvatarUrl( entity.getAvatarUrl() );
        userResponseDTO.setRole( entity.getRole() );
        userResponseDTO.setAuthProvider( entity.getAuthProvider() );
        userResponseDTO.setStatus( entity.getStatus() );
        userResponseDTO.setCreatedAt( entity.getCreatedAt() );
        userResponseDTO.setUpdatedAt( entity.getUpdatedAt() );

        return userResponseDTO;
    }

    @Override
    public User toEntity(UserRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.fullName( dto.getFullName() );
        user.email( dto.getEmail() );
        user.passwordHash( dto.getPasswordHash() );
        user.avatarUrl( dto.getAvatarUrl() );
        user.role( dto.getRole() );
        user.authProvider( dto.getAuthProvider() );
        user.status( dto.getStatus() );

        return user.build();
    }
}
