package com.epam.healenium.service;

import com.epam.healenium.FieldName;
import com.epam.healenium.model.Context;
import com.epam.healenium.treecomparing.Node;
import com.epam.healenium.treecomparing.NodeBuilder;
import com.epam.healenium.utils.ResourceReader;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j(topic = "healenium")
public class NodeService {

    /**
     * A JavaScript source to extract an HTML item with its attributes
     */
    private static final String SCRIPT = ResourceReader.readResource(
            "itemsWithAttributes.js", s -> s.collect(Collectors.joining()));

    /**
     * build list nodes by source webElement
     *
     * @param driver     - web driver
     * @param webElement - source element
     * @param context    - find element context dto
     * @return - list path nodes
     */
    public List<Node> getNodePath(WebDriver driver, WebElement webElement, Context context) {
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        String data = (String) executor.executeScript(SCRIPT, webElement);
        List<Node> path = new LinkedList<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode treeNode = mapper.readTree(data);
            context.setCurrentUrl(treeNode.get("url").textValue());
            JsonNode items = treeNode.get("items");
            if (items.isArray()) {
                for (final JsonNode jsonNode : items) {
                    Node node = toNode(mapper.treeAsTokens(jsonNode));
                    path.add(node);
                }
            }
        } catch (Exception ex) {
            log.error("Failed to get element node path!", ex);
        }
        return path;
    }

    /**
     * Convert raw data to {@code Node}
     *
     * @param parser - JSON reader
     * @return path node
     * @throws IOException
     */
    private Node toNode(JsonParser parser) throws IOException {
        ObjectCodec codec = parser.getCodec();
        TreeNode tree = parser.readValueAsTree();
        String tag = codec.treeToValue(tree.path(FieldName.TAG), String.class);
        Integer index = codec.treeToValue(tree.path(FieldName.INDEX), Integer.class);
        String innerText = codec.treeToValue(tree.path(FieldName.INNER_TEXT), String.class);
        String id = codec.treeToValue(tree.path(FieldName.ID), String.class);
        //noinspection unchecked
        Set<String> classes = codec.treeToValue(tree.path(FieldName.CLASSES), Set.class);
        //noinspection unchecked
        Map<String, String> attributes = codec.treeToValue(tree.path(FieldName.OTHER), Map.class);
        return new NodeBuilder()
                //TODO: fix attribute setting, because they override 'id' and 'classes' property
                .setAttributes(attributes)
                .setTag(tag)
                .setIndex(index)
                .setId(id)
                .addContent(innerText)
                .setClasses(classes)
                .build();
    }
}
