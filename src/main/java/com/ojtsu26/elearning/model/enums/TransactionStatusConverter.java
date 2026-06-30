package com.ojtsu26.elearning.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TransactionStatusConverter implements AttributeConverter<TransactionStatus, String> {
    @Override
    public String convertToDatabaseColumn(TransactionStatus attribute) {
        if (attribute == null) return null;
        return attribute.name().toLowerCase();
    }
    @Override
    public TransactionStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return TransactionStatus.valueOf(dbData.toUpperCase());
    }
}
