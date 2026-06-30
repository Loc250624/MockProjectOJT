package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.dto.request.BlogRequestDTO;
import com.ojtsu26.elearning.dto.response.BlogResponseDTO;
import com.ojtsu26.elearning.model.entity.Blog;
import com.ojtsu26.elearning.model.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-30T21:10:47+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Oracle Corporation)"
)
@Component
public class BlogMapperImpl implements BlogMapper {

    @Override
    public BlogResponseDTO toDto(Blog entity) {
        if ( entity == null ) {
            return null;
        }

        BlogResponseDTO blogResponseDTO = new BlogResponseDTO();

        blogResponseDTO.setAuthorId( entityAuthorId( entity ) );
        blogResponseDTO.setId( entity.getId() );
        blogResponseDTO.setTitle( entity.getTitle() );
        blogResponseDTO.setContent( entity.getContent() );
        blogResponseDTO.setStatus( entity.getStatus() );
        blogResponseDTO.setRejectReason( entity.getRejectReason() );
        blogResponseDTO.setCreatedAt( entity.getCreatedAt() );
        blogResponseDTO.setUpdatedAt( entity.getUpdatedAt() );

        return blogResponseDTO;
    }

    @Override
    public Blog toEntity(BlogRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Blog.BlogBuilder blog = Blog.builder();

        blog.author( blogRequestDTOToUser( dto ) );
        blog.title( dto.getTitle() );
        blog.content( dto.getContent() );
        blog.status( dto.getStatus() );
        blog.rejectReason( dto.getRejectReason() );

        return blog.build();
    }

    private Integer entityAuthorId(Blog blog) {
        if ( blog == null ) {
            return null;
        }
        User author = blog.getAuthor();
        if ( author == null ) {
            return null;
        }
        Integer id = author.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected User blogRequestDTOToUser(BlogRequestDTO blogRequestDTO) {
        if ( blogRequestDTO == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.id( blogRequestDTO.getAuthorId() );

        return user.build();
    }
}
