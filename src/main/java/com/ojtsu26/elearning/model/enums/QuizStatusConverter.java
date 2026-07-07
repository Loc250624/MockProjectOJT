package com.ojtsu26.elearning.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class QuizStatusConverter implements AttributeConverter<QuizStatus, String> {
    @Override
    public String convertToDatabaseColumn(QuizStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public QuizStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : QuizStatus.valueOf(dbData.toUpperCase());
    }
}
