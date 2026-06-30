package com.ojtsu26.elearning.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BlogStatusConverter implements AttributeConverter<BlogStatus, String> {
    @Override
    public String convertToDatabaseColumn(BlogStatus attribute) {
        if (attribute == null) return null;
        return attribute.name().toLowerCase();
    }
    @Override
    public BlogStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return BlogStatus.valueOf(dbData.toUpperCase());
    }
}
