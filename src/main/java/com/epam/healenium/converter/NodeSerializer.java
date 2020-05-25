package com.epam.healenium.converter;

import com.epam.healenium.FieldName;
import com.epam.healenium.treecomparing.Node;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.IOException;

public class NodeSerializer extends JsonSerializer<Node> {

    @Override
    public void serializeWithType(Node value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
        WritableTypeId typeId = typeSer.typeId(value, Node.class, JsonToken.START_OBJECT);
        typeSer.writeTypePrefix(gen, typeId);
        serialize(value, gen, serializers);
        typeSer.writeTypeSuffix(gen, typeId);
    }

    @Override
    public void serialize(Node value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(FieldName.TAG, value.getTag());
        gen.writeNumberField(FieldName.INDEX, value.getIndex());
        gen.writeStringField(FieldName.INNER_TEXT, value.getInnerText());
        gen.writeStringField(FieldName.ID, value.getId());
        gen.writeStringField(FieldName.CLASSES, String.join(" ", value.getClasses()));
        gen.writeObjectField(FieldName.OTHER, value.getOtherAttributes());
        gen.writeEndObject();
        gen.flush();
    }

}
