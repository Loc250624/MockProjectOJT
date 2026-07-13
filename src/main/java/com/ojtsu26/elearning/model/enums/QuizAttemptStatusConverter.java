package com.ojtsu26.elearning.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class QuizAttemptStatusConverter implements AttributeConverter<QuizAttemptStatus, String> {
    @Override
    public String convertToDatabaseColumn(QuizAttemptStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public QuizAttemptStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : QuizAttemptStatus.valueOf(dbData.toUpperCase());
    }
}
