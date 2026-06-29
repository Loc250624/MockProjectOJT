package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.Blog;
import com.ojtsu26.elearning.dto.request.BlogRequestDTO;
import com.ojtsu26.elearning.dto.response.BlogResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BlogMapper {

    @Mapping(source = "author.id", target = "authorId")
    BlogResponseDTO toDto(Blog entity);

    @Mapping(source = "authorId", target = "author.id")
    Blog toEntity(BlogRequestDTO dto);
}
