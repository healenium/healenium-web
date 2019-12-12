/**
 * Healenium-web Copyright (C) 2019 EPAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.healenium.data;

import com.epam.healenium.FieldName;
import com.epam.healenium.treecomparing.Node;
import com.epam.healenium.treecomparing.NodeBuilder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.typesafe.config.Config;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.openqa.selenium.By;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Log4j2
public class FileSystemPathStorage implements PathStorage {

    private static final int MAX_FILE_LENGTH = 128;
    private static final String FILENAME_REGEX = "[\\w\\-]+";
    private static final String REPORT_FILE = "index.html";
    private final Path basePath;
    private final Path reportsPath;
    private final ObjectMapper objectMapper;

    /**
     * Creates a file system bound storage.
     *
     * @param config a file system path to the place the storage will be physically allocated. Supports relative paths; in
     *               this case the path starts from a working directory.
     */
    public FileSystemPathStorage(Config config) {
        this.objectMapper = initMapper();
        this.basePath = Paths.get(config.getString("basePath"));
        this.reportsPath = Paths.get(config.getString("reportPath"));
        basePath.toFile().mkdirs();
        reportsPath.toFile().mkdirs();
    }

    private ObjectMapper initMapper() {
        SimpleModule module = new SimpleModule("node");
        module.addSerializer(Node.class, new NodeSerializer());
        module.addDeserializer(Node.class, new NodeDeserializer());
        ObjectMapper objectMapper = new ObjectMapper().registerModule(module);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return objectMapper;
    }

    @Override
    public synchronized void persistLastValidPath(By locator, String context, List<Node> nodes) {
        log.debug("* persistLastValidPath start: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
        Path persistedNodePath = getPersistedNodePath(locator, context);
        byte[] newContent;
        try {
            newContent = objectMapper.writeValueAsBytes(nodes);
            Files.write(persistedNodePath, newContent);
        } catch (JsonProcessingException e) {
            log.error("Could not map the contents to JSON!", e);
        } catch (IOException e) {
            log.error("Failed to persist last valid path", e);
        }
        log.debug("* persistLastValidPath finish: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
    }

    @Override
    public synchronized List<Node> getLastValidPath(By locator, String context) {
        Path path = getPersistedNodePath(locator, context);
        if (Files.exists(path)) {
            try {
                byte[] bytes = Files.readAllBytes(path);
                //noinspection unchecked
                return objectMapper.readValue(bytes, List.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return Collections.emptyList();
    }

    public synchronized void saveLocatorInfo(LocatorInfo info) throws IOException {
        new ObjectMapper().writeValue(reportsPath.resolve("data.json").toFile(), info);
        Path target = reportsPath.resolve(REPORT_FILE);
        if (!Files.exists(target)) {
            log.debug(String.format("File '%s' does not exist.", target));
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream source = classLoader.getResourceAsStream(REPORT_FILE);
            if (source == null) {
                log.debug(String.format("Input stream for reading the '%s' resource is null. Copy to '%s' was skipped.", REPORT_FILE, target));
            } else {
                log.debug(String.format("Copy input stream of reading the '%s' resource to '%s' file.", REPORT_FILE, target));
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            log.debug(String.format("File '%s' exists.", target));
        }
    }

    private Path getPersistedNodePath(By locator, String context) {
        return basePath.resolve(getFileName(context) + "_" + locator.hashCode());
    }

    private String getFileName(String context) {
        if (context.matches(FILENAME_REGEX) && context.length() < MAX_FILE_LENGTH) {
            return context;
        }
        return DigestUtils.md5Hex(context);
    }

    private static class NodeSerializer extends JsonSerializer<Node> {

        @Override
        public void serializeWithType(Node value, JsonGenerator gen, SerializerProvider serializers,
                                      TypeSerializer typeSer) throws IOException {
            WritableTypeId typeId = typeSer.typeId(value, Node.class, JsonToken.START_OBJECT);
            typeSer.writeTypePrefix(gen, typeId);
            serialize(value, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeId);
        }

        @Override
        public void serialize(Node value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStringField(FieldName.TAG, value.getTag());
            gen.writeNumberField(FieldName.INDEX, value.getIndex());
            gen.writeStringField(FieldName.INNER_TEXT, value.getInnerText());
            gen.writeStringField(FieldName.ID, value.getId());
            gen.writeStringField(FieldName.CLASSES, String.join(" ", value.getClasses()));
            gen.writeObjectField(FieldName.OTHER, value.getOtherAttributes());
            gen.flush();
        }
    }

    private static class NodeDeserializer extends JsonDeserializer<Node> {

        @Override
        public Node deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
            ObjectCodec codec = parser.getCodec();
            TreeNode tree = parser.readValueAsTree();
            String tag = codec.treeToValue(tree.path(FieldName.TAG), String.class);
            Integer index = codec.treeToValue(tree.path(FieldName.INDEX), Integer.class);
            String innerText = codec.treeToValue(tree.path(FieldName.INNER_TEXT), String.class);
            String id = codec.treeToValue(tree.path(FieldName.ID), String.class);
            String classes = codec.treeToValue(tree.path(FieldName.CLASSES), String.class);
            //noinspection unchecked
            Map<String, String> attributes = codec.treeToValue(tree.path(FieldName.OTHER), Map.class);
            attributes.put(FieldName.ID, id);
            attributes.put(FieldName.CLASS, classes);
            return new NodeBuilder()
                    .setTag(tag)
                    .setIndex(index)
                    .addContent(innerText)
                    .setAttributes(attributes)
                    .build();
        }
    }
}
