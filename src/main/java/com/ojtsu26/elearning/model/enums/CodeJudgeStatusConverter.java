package com.ojtsu26.elearning.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CodeJudgeStatusConverter implements AttributeConverter<CodeJudgeStatus, String> {
    @Override
    public String convertToDatabaseColumn(CodeJudgeStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public CodeJudgeStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CodeJudgeStatus.valueOf(dbData.toUpperCase());
    }
}
