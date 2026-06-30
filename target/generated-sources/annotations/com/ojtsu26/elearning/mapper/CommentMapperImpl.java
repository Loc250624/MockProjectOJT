package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.dto.request.CommentRequestDTO;
import com.ojtsu26.elearning.dto.response.CommentResponseDTO;
import com.ojtsu26.elearning.model.entity.Comment;
import com.ojtsu26.elearning.model.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-30T09:33:45+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Oracle Corporation)"
)
@Component
public class CommentMapperImpl implements CommentMapper {

    @Override
    public CommentResponseDTO toDto(Comment entity) {
        if ( entity == null ) {
            return null;
        }

        CommentResponseDTO commentResponseDTO = new CommentResponseDTO();

        commentResponseDTO.setUserId( entityUserId( entity ) );
        commentResponseDTO.setParentId( entityParentId( entity ) );
        commentResponseDTO.setId( entity.getId() );
        commentResponseDTO.setTargetType( entity.getTargetType() );
        commentResponseDTO.setTargetId( entity.getTargetId() );
        commentResponseDTO.setContent( entity.getContent() );
        commentResponseDTO.setCreatedAt( entity.getCreatedAt() );

        return commentResponseDTO;
    }

    @Override
    public Comment toEntity(CommentRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Comment.CommentBuilder comment = Comment.builder();

        comment.user( commentRequestDTOToUser( dto ) );
        comment.parent( commentRequestDTOToComment( dto ) );
        comment.targetType( dto.getTargetType() );
        comment.targetId( dto.getTargetId() );
        comment.content( dto.getContent() );

        return comment.build();
    }

    private Integer entityUserId(Comment comment) {
        if ( comment == null ) {
            return null;
        }
        User user = comment.getUser();
        if ( user == null ) {
            return null;
        }
        Integer id = user.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Integer entityParentId(Comment comment) {
        if ( comment == null ) {
            return null;
        }
        Comment parent = comment.getParent();
        if ( parent == null ) {
            return null;
        }
        Integer id = parent.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected User commentRequestDTOToUser(CommentRequestDTO commentRequestDTO) {
        if ( commentRequestDTO == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.id( commentRequestDTO.getUserId() );

        return user.build();
    }

    protected Comment commentRequestDTOToComment(CommentRequestDTO commentRequestDTO) {
        if ( commentRequestDTO == null ) {
            return null;
        }

        Comment.CommentBuilder comment = Comment.builder();

        comment.id( commentRequestDTO.getParentId() );

        return comment.build();
    }
}
