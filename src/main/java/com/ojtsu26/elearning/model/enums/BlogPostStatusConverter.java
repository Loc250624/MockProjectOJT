package com.ojtsu26.elearning.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BlogPostStatusConverter implements AttributeConverter<BlogPostStatus, String> {

    @Override
    public String convertToDatabaseColumn(BlogPostStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public BlogPostStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BlogPostStatus.valueOf(dbData.toUpperCase());
    }
}
