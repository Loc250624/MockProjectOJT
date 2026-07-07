package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.dto.response.BlogCommentResponseDTO;
import com.ojtsu26.elearning.model.entity.BlogComment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BlogCommentMapper {

    @Mapping(source = "blogPost.id", target = "blogPostId")
    @Mapping(source = "blogPost.title", target = "blogPostTitle")
    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "author.fullName", target = "authorName")
    BlogCommentResponseDTO toDto(BlogComment entity);
}
