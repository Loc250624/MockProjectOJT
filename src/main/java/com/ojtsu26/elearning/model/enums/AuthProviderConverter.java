package com.ojtsu26.elearning.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AuthProviderConverter implements AttributeConverter<AuthProvider, String> {

    @Override
    public String convertToDatabaseColumn(AuthProvider attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name().toLowerCase();
    }

    @Override
    public AuthProvider convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return AuthProvider.valueOf(dbData.toUpperCase());
    }
}
