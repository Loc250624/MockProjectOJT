package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.Certificate;
import com.ojtsu26.elearning.dto.request.CertificateRequestDTO;
import com.ojtsu26.elearning.dto.response.CertificateResponseDTO;
import com.ojtsu26.elearning.mapper.CertificateMapper;
import com.ojtsu26.elearning.repository.CertificateRepository;
import com.ojtsu26.elearning.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final CertificateMapper certificateMapper;

    @Override
    public List<CertificateResponseDTO> findAll() {
        return certificateRepository.findAll().stream()
                .map(certificateMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CertificateResponseDTO findById(Integer id) {
        Certificate entity = certificateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Certificate not found"));
        return certificateMapper.toDto(entity);
    }

    @Override
    public CertificateResponseDTO create(CertificateRequestDTO requestDTO) {
        Certificate entity = certificateMapper.toEntity(requestDTO);
        Certificate saved = certificateRepository.save(entity);
        return certificateMapper.toDto(saved);
    }

    @Override
    public CertificateResponseDTO update(Integer id, CertificateRequestDTO requestDTO) {
        if (!certificateRepository.existsById(id)) {
            throw new RuntimeException("Certificate not found");
        }
        Certificate entity = certificateMapper.toEntity(requestDTO);
        entity.setId(id);
        Certificate updated = certificateRepository.save(entity);
        return certificateMapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        certificateRepository.deleteById(id);
    }
}
