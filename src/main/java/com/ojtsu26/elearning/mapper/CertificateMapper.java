package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.Certificate;
import com.ojtsu26.elearning.dto.request.CertificateRequestDTO;
import com.ojtsu26.elearning.dto.response.CertificateResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CertificateMapper {

    @Mapping(source = "enrollment.id", target = "enrollmentId")
    @Mapping(source = "course.id", target = "courseId")
    @Mapping(source = "studentNameSnapshot", target = "studentName")
    @Mapping(source = "courseNameSnapshot", target = "courseName")
    @Mapping(source = "teacherNameSnapshot", target = "teacherName")
    @Mapping(target = "downloadUrl", expression = "java(\"/api/student/certificates/\" + entity.getId() + \"/download\")")
    @Mapping(target = "verifyUrl", expression = "java(\"/certificates/verify/\" + entity.getVerificationCode())")
    CertificateResponseDTO toDto(Certificate entity);

    @Mapping(source = "studentId", target = "student.id")
    @Mapping(source = "courseId", target = "course.id")
    Certificate toEntity(CertificateRequestDTO dto);
}
