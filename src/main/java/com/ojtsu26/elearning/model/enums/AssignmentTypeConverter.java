package com.ojtsu26.elearning.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AssignmentTypeConverter implements AttributeConverter<AssignmentType, String> {
    @Override
    public String convertToDatabaseColumn(AssignmentType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public AssignmentType convertToEntityAttribute(String dbData) {
        return dbData == null || dbData.isBlank() ? null : AssignmentType.valueOf(dbData);
    }
}
