package com.ojtsu26.elearning.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LessonTypeConverter implements AttributeConverter<LessonType, String> {
    @Override
    public String convertToDatabaseColumn(LessonType attribute) {
        if (attribute == null) return null;
        if (attribute == LessonType.RETIRED) {
            return "coding";
        }
        return attribute.name().toLowerCase();
    }
    @Override
    public LessonType convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        if ("coding".equalsIgnoreCase(dbData)) {
            return LessonType.RETIRED;
        }
        return LessonType.valueOf(dbData.toUpperCase());
    }
}
