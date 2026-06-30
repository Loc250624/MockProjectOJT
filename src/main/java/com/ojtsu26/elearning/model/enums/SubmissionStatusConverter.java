package com.ojtsu26.elearning.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SubmissionStatusConverter implements AttributeConverter<SubmissionStatus, String> {
    @Override
    public String convertToDatabaseColumn(SubmissionStatus attribute) {
        if (attribute == null) return null;
        return attribute.name().toLowerCase();
    }
    @Override
    public SubmissionStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return SubmissionStatus.valueOf(dbData.toUpperCase());
    }
}
