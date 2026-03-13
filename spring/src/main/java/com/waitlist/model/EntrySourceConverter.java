package com.waitlist.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for {@link EntrySource} so we can store it as a string in the database.
 * This converter is case-insensitive and tolerates legacy lowercase values.
 */
@Converter(autoApply = true)
public class EntrySourceConverter implements AttributeConverter<EntrySource, String> {

    @Override
    public String convertToDatabaseColumn(EntrySource attribute) {
        return attribute == null ? null : attribute.toJson();
    }

    @Override
    public EntrySource convertToEntityAttribute(String dbData) {
        return EntrySource.from(dbData);
    }
}
