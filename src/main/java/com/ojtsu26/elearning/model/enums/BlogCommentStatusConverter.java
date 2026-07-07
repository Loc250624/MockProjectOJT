package com.ojtsu26.elearning.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BlogCommentStatusConverter implements AttributeConverter<BlogCommentStatus, String> {

    @Override
    public String convertToDatabaseColumn(BlogCommentStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public BlogCommentStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BlogCommentStatus.valueOf(dbData.toUpperCase());
    }
}
