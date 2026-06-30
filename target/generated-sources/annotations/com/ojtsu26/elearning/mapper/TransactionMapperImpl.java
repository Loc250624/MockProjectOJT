package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.dto.request.TransactionRequestDTO;
import com.ojtsu26.elearning.dto.response.TransactionResponseDTO;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.Transaction;
import com.ojtsu26.elearning.model.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-30T09:33:45+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Oracle Corporation)"
)
@Component
public class TransactionMapperImpl implements TransactionMapper {

    @Override
    public TransactionResponseDTO toDto(Transaction entity) {
        if ( entity == null ) {
            return null;
        }

        TransactionResponseDTO transactionResponseDTO = new TransactionResponseDTO();

        transactionResponseDTO.setStudentId( entityStudentId( entity ) );
        transactionResponseDTO.setCourseId( entityCourseId( entity ) );
        transactionResponseDTO.setId( entity.getId() );
        transactionResponseDTO.setAmount( entity.getAmount() );
        transactionResponseDTO.setPaymentMethod( entity.getPaymentMethod() );
        transactionResponseDTO.setTransactionRef( entity.getTransactionRef() );
        transactionResponseDTO.setStatus( entity.getStatus() );
        transactionResponseDTO.setWebhookResponse( entity.getWebhookResponse() );
        transactionResponseDTO.setCreatedAt( entity.getCreatedAt() );
        transactionResponseDTO.setUpdatedAt( entity.getUpdatedAt() );

        return transactionResponseDTO;
    }

    @Override
    public Transaction toEntity(TransactionRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Transaction.TransactionBuilder transaction = Transaction.builder();

        transaction.student( transactionRequestDTOToUser( dto ) );
        transaction.course( transactionRequestDTOToCourse( dto ) );
        transaction.amount( dto.getAmount() );
        transaction.paymentMethod( dto.getPaymentMethod() );
        transaction.transactionRef( dto.getTransactionRef() );
        transaction.status( dto.getStatus() );
        transaction.webhookResponse( dto.getWebhookResponse() );

        return transaction.build();
    }

    private Integer entityStudentId(Transaction transaction) {
        if ( transaction == null ) {
            return null;
        }
        User student = transaction.getStudent();
        if ( student == null ) {
            return null;
        }
        Integer id = student.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Integer entityCourseId(Transaction transaction) {
        if ( transaction == null ) {
            return null;
        }
        Course course = transaction.getCourse();
        if ( course == null ) {
            return null;
        }
        Integer id = course.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected User transactionRequestDTOToUser(TransactionRequestDTO transactionRequestDTO) {
        if ( transactionRequestDTO == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.id( transactionRequestDTO.getStudentId() );

        return user.build();
    }

    protected Course transactionRequestDTOToCourse(TransactionRequestDTO transactionRequestDTO) {
        if ( transactionRequestDTO == null ) {
            return null;
        }

        Course.CourseBuilder course = Course.builder();

        course.id( transactionRequestDTO.getCourseId() );

        return course.build();
    }
}
