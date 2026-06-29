package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.Comment;
import com.ojtsu26.elearning.dto.request.CommentRequestDTO;
import com.ojtsu26.elearning.dto.response.CommentResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "parent.id", target = "parentId")
    CommentResponseDTO toDto(Comment entity);

    @Mapping(source = "userId", target = "user.id")
    @Mapping(source = "parentId", target = "parent.id")
    Comment toEntity(CommentRequestDTO dto);
}
