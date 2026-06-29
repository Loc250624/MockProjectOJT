package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.Certificate;
import com.ojtsu26.elearning.dto.request.CertificateRequestDTO;
import com.ojtsu26.elearning.dto.response.CertificateResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CertificateMapper {

    @Mapping(source = "student.id", target = "studentId")
    @Mapping(source = "course.id", target = "courseId")
    CertificateResponseDTO toDto(Certificate entity);

    @Mapping(source = "studentId", target = "student.id")
    @Mapping(source = "courseId", target = "course.id")
    Certificate toEntity(CertificateRequestDTO dto);
}
