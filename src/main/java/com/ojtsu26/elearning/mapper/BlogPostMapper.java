package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.dto.response.BlogPostResponseDTO;
import com.ojtsu26.elearning.model.entity.BlogPost;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BlogPostMapper {

    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "author.fullName", target = "authorName")
    BlogPostResponseDTO toDto(BlogPost entity);
}
