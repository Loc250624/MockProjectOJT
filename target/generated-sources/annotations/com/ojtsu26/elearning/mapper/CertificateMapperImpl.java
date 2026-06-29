package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.dto.request.CertificateRequestDTO;
import com.ojtsu26.elearning.dto.response.CertificateResponseDTO;
import com.ojtsu26.elearning.model.entity.Certificate;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-29T22:09:51+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Oracle Corporation)"
)
@Component
public class CertificateMapperImpl implements CertificateMapper {

    @Override
    public CertificateResponseDTO toDto(Certificate entity) {
        if ( entity == null ) {
            return null;
        }

        CertificateResponseDTO certificateResponseDTO = new CertificateResponseDTO();

        certificateResponseDTO.setStudentId( entityStudentId( entity ) );
        certificateResponseDTO.setCourseId( entityCourseId( entity ) );
        certificateResponseDTO.setId( entity.getId() );
        certificateResponseDTO.setIssueDate( entity.getIssueDate() );
        certificateResponseDTO.setCertificateUrl( entity.getCertificateUrl() );

        return certificateResponseDTO;
    }

    @Override
    public Certificate toEntity(CertificateRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Certificate.CertificateBuilder certificate = Certificate.builder();

        certificate.student( certificateRequestDTOToUser( dto ) );
        certificate.course( certificateRequestDTOToCourse( dto ) );
        certificate.certificateUrl( dto.getCertificateUrl() );

        return certificate.build();
    }

    private Integer entityStudentId(Certificate certificate) {
        if ( certificate == null ) {
            return null;
        }
        User student = certificate.getStudent();
        if ( student == null ) {
            return null;
        }
        Integer id = student.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Integer entityCourseId(Certificate certificate) {
        if ( certificate == null ) {
            return null;
        }
        Course course = certificate.getCourse();
        if ( course == null ) {
            return null;
        }
        Integer id = course.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected User certificateRequestDTOToUser(CertificateRequestDTO certificateRequestDTO) {
        if ( certificateRequestDTO == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.id( certificateRequestDTO.getStudentId() );

        return user.build();
    }

    protected Course certificateRequestDTOToCourse(CertificateRequestDTO certificateRequestDTO) {
        if ( certificateRequestDTO == null ) {
            return null;
        }

        Course.CourseBuilder course = Course.builder();

        course.id( certificateRequestDTO.getCourseId() );

        return course.build();
    }
}
