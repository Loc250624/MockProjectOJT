package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.CertificateRequestDTO;
import com.ojtsu26.elearning.dto.response.CertificateResponseDTO;
import java.util.List;

public interface CertificateService {
    List<CertificateResponseDTO> findAll();
    CertificateResponseDTO findById(Integer id);
    CertificateResponseDTO create(CertificateRequestDTO requestDTO);
    CertificateResponseDTO update(Integer id, CertificateRequestDTO requestDTO);
    void delete(Integer id);
}
