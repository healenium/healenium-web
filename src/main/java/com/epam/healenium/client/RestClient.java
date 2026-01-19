/**
 * Healenium-web Copyright (C) 2019 EPAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.healenium.client;

import com.epam.healenium.converter.NodeDeserializer;
import com.epam.healenium.converter.NodeSerializer;
import com.epam.healenium.exception.HealeniumException;
import com.epam.healenium.mapper.HealeniumMapper;
import com.epam.healenium.model.ConfigSelectorDto;
import com.epam.healenium.model.Context;
import com.epam.healenium.model.HealedElement;
import com.epam.healenium.model.HealingResult;
import com.epam.healenium.model.Locator;
import com.epam.healenium.model.ReferenceElementsDto;
import com.epam.healenium.model.RequestDto;
import com.epam.healenium.model.SelectorImitatorDto;
import com.epam.healenium.treecomparing.Node;
import com.epam.healenium.treecomparing.Scored;
import com.epam.healenium.utils.SystemUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.typesafe.config.Config;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.http.ClientConfig;
import org.openqa.selenium.remote.http.Contents;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.http.HttpMethod;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.openqa.selenium.json.Json.JSON_UTF_8;

/**
 * Wrapper for {@code RestTemplate} class.
 * Main purpose - encapsulate consumer from really used client and invocation complexity
 */

@Slf4j(topic = "healenium")
@Data
public class RestClient {

    private final String serverUrl;
    private final String imitateUrl;
    private final String aiServiceUrl;
    private final String sessionKey;
    private final String selectorType;
    private ObjectMapper objectMapper;
    private HealeniumMapper mapper;
    private HttpClient serverHttpClient;
    private HttpClient imitateHttpClient;
    private HttpClient aiServiceHttpClient;

    public RestClient(Config config) {
        this.objectMapper = initMapper();
        this.sessionKey = config.getString("sessionKey");
        this.serverUrl = getHttpUrl(config.getString("hlm.server.url"), "/healenium");
        this.imitateUrl = getHttpUrl(config.getString("hlm.imitator.url"), "/imitate");
        this.aiServiceUrl = getHttpUrl(config.getString("hlm.ai.url"), "/healenium-ai");
        this.selectorType = config.hasPath("selector-type") ? config.getString("selector-type") : "cssSelector";
        this.serverHttpClient = getHttpClient(serverUrl);
        this.imitateHttpClient = getHttpClient(imitateUrl);
        this.aiServiceHttpClient = getHttpClient(aiServiceUrl);
        log.debug("[Init] sessionKey: {}, serverUrl: {}, imitateUrl: {}, selectorType: {}", 
                sessionKey, serverUrl, imitateUrl, selectorType);
    }

    private String getHttpUrl(String hlmServerUrl, String path) {
        return (hlmServerUrl.startsWith("http") ? hlmServerUrl : "http://".concat(hlmServerUrl)).concat(path);
    }

    @SneakyThrows
    private HttpClient getHttpClient(String url) {
        ClientConfig clientConfig = ClientConfig.defaultConfig()
                .baseUrl(new URL(url));
        return HttpClient.Factory.createDefault().createClient(clientConfig);
    }

    private ObjectMapper initMapper() {
        SimpleModule module = new SimpleModule("node");
        module.addSerializer(Node.class, new NodeSerializer());
        module.addDeserializer(Node.class, new NodeDeserializer());
        ObjectMapper mapper = new ObjectMapper().registerModule(module);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        return mapper;
    }

    /**
     * Store info in backend
     * <p>
     * //     * @param by         element By locator
     * //     * @param nodePath   List of nodes
     * //     * @param currentUrl url of web page
     */
    public void saveElements(RequestDto requestDto) {
        try {
            HttpRequest request = new HttpRequest(HttpMethod.POST, "");
            String content = objectMapper.writeValueAsString(requestDto);
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            request.setHeader("Content-Length", String.valueOf(data.length));
            request.setHeader("Content-Type", JSON_UTF_8);
            request.setContent(Contents.bytes(data));
            log.debug("[Save Elements] By: {}, Locator: {}, Command: {}, URL: {}",
                    requestDto.getType(), requestDto.getLocator(), requestDto.getCommand(), requestDto.getUrl());
            serverExecute(request);
        } catch (Exception e) {
            log.warn("[Save Elements] Error during call. Message: {}, Exception: {}", e.getMessage(), e.toString());
        }
    }

    public ConfigSelectorDto getElements() {
        ConfigSelectorDto configSelectorDto = null;
        try {
            HttpRequest request = new HttpRequest(HttpMethod.GET, "/elements");
            request.setHeader("Cache-Control", "no-cache");
            log.debug("[Get Elements] Request: {}", request);
            HttpResponse response = serverExecute(request);

            if (HTTP_NOT_FOUND == response.getStatus()) {
                throw new RuntimeException("[Get Elements] Compatibility error. Hlm-backend service must be 3.3.0 and height." +
                        "\nActual versions you can find here: https://github.com/healenium/healenium/blob/master/docker-compose-web.yaml");
            }
            Supplier<InputStream> result = response.getContent();
            configSelectorDto = objectMapper.readValue(result.get(), new TypeReference<ConfigSelectorDto>() {
            });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[Get Elements] Error during call. Message: {}, Exception: {}", e.getMessage(), e.toString());
        }
        return configSelectorDto;
    }

    /**
     * Collect results from previous healing
     *
     * @param context healing context
     */
    public void healRequest(Context context) {
        try {
            List<RequestDto> requestDtos = new ArrayList<>();
            for (HealingResult healingResult : context.getHealingResults()) {
                List<Scored<By>> choices = healingResult.getHealedElements().stream()
                        .map(HealedElement::getScored)
                        .collect(Collectors.toList());
                if (choices.isEmpty()) {
                    return;
                }
                String metrics = objectMapper.writeValueAsString(healingResult.getMetricsDto());
                requestDtos.add(mapper.buildMultRequest(context, healingResult, choices, metrics));
            }

            HttpRequest request = new HttpRequest(HttpMethod.POST, "/healing");
            String content = objectMapper.writeValueAsString(requestDtos);
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            request.setHeader("Content-Length", String.valueOf(data.length));
            request.setHeader("Content-Type", JSON_UTF_8);
            request.setHeader("sessionKey", sessionKey);
            request.setHeader("hostProject", SystemUtils.getHostProjectName());
            request.setContent(Contents.bytes(data));
            for (RequestDto requestDto : requestDtos) {
                log.debug("[Save Healed Elements] {}", requestDto.getUsedResult().getLocator());
            }
            serverExecute(request);
        } catch (Exception e) {
            log.warn("[Heal Element] Error during call. Message: {}. Exception: {}", e.getMessage(), e.toString());
        }
    }

    /**
     * Get node path for given selector
     *
     * @param locator    element By locator
     * @param currentUrl url of web page
     * @param command    name of command
     * @return lastHealingDataDto   previous success healing data
     */
    public Optional<ReferenceElementsDto> getReferenceElements(By locator, String command, String currentUrl) {
        ReferenceElementsDto referenceElementsDto = null;
        RequestDto requestDto = mapper.buildDto(locator, command, currentUrl);
        try {
            HttpRequest request = new HttpRequest(HttpMethod.GET, "");
            request.setHeader("Cache-Control", "no-cache")
                    .setHeader("sessionKey", sessionKey)
                    .addQueryParameter("locator", requestDto.getLocator())
                    .addQueryParameter("className", requestDto.getClassName())
                    .addQueryParameter("methodName", requestDto.getMethodName())
                    .addQueryParameter("command", requestDto.getCommand())
                    .addQueryParameter("url", currentUrl);
            log.debug("[Get Reference Elements] Request. Locator: {}, Command: {}, Url: {}",
                    requestDto.getLocator(), requestDto.getCommand(), currentUrl);
            HttpResponse response = serverExecute(request);
            if (response.getStatus() == 200) {
                Supplier<InputStream> result = response.getContent();
                referenceElementsDto = objectMapper.readValue(result.get(), new TypeReference<ReferenceElementsDto>() {
                });
            }
        } catch (Exception e) {
            log.warn("[Get Reference Elements] Error during call. Message: {}. Exception: {}", e.getMessage(), e.toString());
        }
        return Optional.ofNullable(referenceElementsDto);
    }

    /**
     * Get imitated locators by target node and user selector from selector-imitator service
     *
     * @param selectorImitatorDto element By locator
     * @return locators
     */
    public List<Locator> imitate(SelectorImitatorDto selectorImitatorDto) {
        try {
            HttpRequest request = new HttpRequest(HttpMethod.POST, "");
            String content = objectMapper.writeValueAsString(selectorImitatorDto);
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            request.setHeader("Content-Length", String.valueOf(data.length));
            request.setHeader("Content-Type", JSON_UTF_8);
            request.setContent(Contents.bytes(data));
            HttpResponse response = imitateExecute(request);

            if (response.getStatus() == 200) {
                Supplier<InputStream> result = response.getContent();
                List<Locator> locators = objectMapper.readValue(result.get(), new TypeReference<List<Locator>>() {
                });
                log.debug("[Selector Imitate] Response: {}", locators);
                return locators;
            }
        } catch (Exception e) {
            log.warn("[Selector Imitate] Error during call. Message: {}, Exception: {}", e.getMessage(), e.toString());
        }
        return Collections.emptyList();
    }

    public void initReport(String sessionId) {
        try {
            HttpRequest request = new HttpRequest(HttpMethod.POST, "/report/init/" + sessionId);
            request.setHeader("Content-Type", JSON_UTF_8);
            log.debug("[Init Report] Request: {}", request);
            serverExecute(request);
        } catch (Exception e) {
            log.warn("[Init Report] Error during call. Message: {}, Exception: {}", e.getMessage(), e.toString());
        }
    }

    public String getXpathSelector(Node node, String sessionId) {
        String xpath = null;
        try {
            if (node == null) {
                log.error("[Get Xpath Selector] Node is null, cannot proceed with request");
                return null;
            }
            
            log.debug("[Get Xpath Selector] Node details - Tag: {}, ID: {}, Classes: {}", 
                node.getTag(), node.getId(), node.getClasses());
                
            HttpRequest request = new HttpRequest(HttpMethod.POST, "/selectors/xpath");
            String content = objectMapper.writeValueAsString(node);
            log.debug("[Get Xpath Selector] Request body: {}", content);
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            request.setHeader("Content-Length", String.valueOf(data.length));
            request.setHeader("Content-Type", JSON_UTF_8);
            request.setHeader("X-Session-Id", sessionId);
            request.setContent(Contents.bytes(data));
            HttpResponse response = aiServiceExecute(request);

            if (HTTP_NOT_FOUND == response.getStatus()) {
                throw new RuntimeException("[Get Xpath Selector] Compatibility error. You must have a paid hlm-ai service.");
            }
            Supplier<InputStream> result = response.getContent();
            Map<String, String> responseMap = objectMapper.readValue(result.get(), new TypeReference<Map<String, String>>() {});
            xpath = responseMap.get("xpath");
            log.debug("[Get Xpath Selector] Received xpath: {}", xpath);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[Get Xpath Selector] Error during call. Message: {}, Exception: {}", e.getMessage(), e.toString());
        }
        return xpath;
    }

    private HttpResponse serverExecute(HttpRequest request) {
        try {
            return serverHttpClient.execute(request);
        } catch (UncheckedIOException e) {
            String errorMessage = "[Execute Error] Unable to connect to the hlm-backend service. " +
                    "Please check if the service is up and running, and verify that the connection URL is correct.";
            log.error(errorMessage);
            throw new HealeniumException(errorMessage, e);
        }
    }

    private HttpResponse aiServiceExecute(HttpRequest request) {
        try {
            return aiServiceHttpClient.execute(request);
        } catch (UncheckedIOException e) {
            String errorMessage = "[Execute Error] Unable to connect to the hlm-ai service. " +
                    "Please check if the service is up and running, and verify that the connection URL is correct.";
            log.error(errorMessage);
            throw new HealeniumException(errorMessage, e);
        }
    }

    private HttpResponse imitateExecute(HttpRequest request) {
        try {
            return imitateHttpClient.execute(request);
        } catch (UncheckedIOException e) {
            String errorMessage = "[Execute Error] Unable to connect to the selector-imitator service. " +
                    "Please check if the service is up and running, and verify that the connection URL is correct.";
            log.error(errorMessage);
            throw new HealeniumException(errorMessage, e);
        }
    }
}
