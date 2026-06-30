package com.ojtsu26.elearning.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CourseStatusConverter implements AttributeConverter<CourseStatus, String> {
    @Override
    public String convertToDatabaseColumn(CourseStatus attribute) {
        if (attribute == null) return null;
        return attribute.name().toLowerCase();
    }
    @Override
    public CourseStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return CourseStatus.valueOf(dbData.toUpperCase());
    }
}
