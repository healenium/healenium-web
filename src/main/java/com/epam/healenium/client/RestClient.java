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

import com.epam.healenium.client.callback.HttpCallback;
import com.epam.healenium.converter.NodeDeserializer;
import com.epam.healenium.converter.NodeSerializer;
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
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

/**
 * Wrapper for {@code RestTemplate} class.
 * Main purpose - encapsulate consumer from really used client and invocation complexity
 */

@Slf4j(topic = "healenium")
@Data
public class RestClient {

    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final String serverUrl;
    private final String imitateUrl;
    private final String sessionKey;
    private ObjectMapper objectMapper;
    private HealeniumMapper mapper;
    private OkHttpClient okHttpClient;
    private HttpCallback httpCallback;

    public RestClient(Config config) {
        this.okHttpClient = initOkHttpClient();
        this.httpCallback = new HttpCallback();
        this.objectMapper = initMapper();
        this.sessionKey = config.getString("sessionKey");
        this.serverUrl = getHttpUrl(config.getString("hlm.server.url"), "/healenium");
        this.imitateUrl = getHttpUrl(config.getString("hlm.imitator.url"), "/imitate");
        log.debug("[Init] sessionKey: {}, serverUrl: {}, imitateUrl: {}", sessionKey, serverUrl, imitateUrl);
    }

    private String getHttpUrl(String hlmServerUrl, String path) {
        return (hlmServerUrl.startsWith("http") ? hlmServerUrl : "http://".concat(hlmServerUrl)).concat(path);
    }

    private OkHttpClient initOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
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
            String bodyStr = objectMapper.writeValueAsString(requestDto);
            RequestBody body = RequestBody.create(JSON, bodyStr);
            Request request = new Request.Builder()
                    .url(serverUrl)
                    .post(body)
                    .build();
            log.debug("[Save Elements] Request: {}. Request body: {}", request, requestDto);
            okHttpClient.newCall(request).enqueue(httpCallback.asyncCall());
        } catch (Exception e) {
            log.warn("[Save Elements] Error during call. Message: {}, Exception: {}", e.getMessage(), e);
        }
    }

    public ConfigSelectorDto getElements() {
        ConfigSelectorDto configSelectorDto = null;
        try {
            Request request = new Request.Builder()
                    .url(serverUrl + "/elements")
                    .get()
                    .build();
            log.debug("[Get Elements] Request: {}", request);
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (HTTP_NOT_FOUND == response.code()) {
                      throw new RuntimeException("[Get Elements] Compatibility error. Hlm-backend service must be 3.3.0 and height." +
                            "\nActual versions you can find here: https://github.com/healenium/healenium/blob/master/docker-compose-web.yaml");
                }
                String result = Objects.requireNonNull(response.body()).string();
                configSelectorDto = objectMapper.readValue(result, new TypeReference<ConfigSelectorDto>() {
                });
            }
            log.debug("[Get Elements] Response: {}", configSelectorDto);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[Get Elements] Error during call. Message: {}, Exception: {}", e.getMessage(), e);
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

            String bodyStr = objectMapper.writeValueAsString(requestDtos);
            RequestBody requestBody = RequestBody.create(JSON, bodyStr);

            Request request = new Request.Builder()
                    .addHeader("sessionKey", sessionKey)
                    .addHeader("hostProject", SystemUtils.getHostProjectName())
                    .url(serverUrl + "/healing")
                    .post(requestBody)
                    .build();
            log.debug("[Heal Element] Request: {}. Request body: {}", request, requestDtos);
            okHttpClient.newCall(request).enqueue(httpCallback.asyncCall());
        } catch (Exception e) {
            log.warn("[Heal Element] Error during call. Message: {}, Exception: {}", e.getMessage(), e);
        }
    }

    /**
     * Get node path for given selector
     *
     * @param locator    element By locator
     * @param currentUrl url of web page
     * @param command name of command
     * @return lastHealingDataDto   previous success healing data
     */
    public Optional<ReferenceElementsDto> getReferenceElements(By locator, String command, String currentUrl) {
        ReferenceElementsDto referenceElementsDto = null;
        RequestDto requestDto = mapper.buildDto(locator, command, currentUrl);
        try {
            HttpUrl.Builder httpBuilder = HttpUrl.parse(serverUrl).newBuilder()
                    .addQueryParameter("locator", requestDto.getLocator())
                    .addQueryParameter("className", requestDto.getClassName())
                    .addQueryParameter("methodName", requestDto.getMethodName())
                    .addQueryParameter("command", requestDto.getCommand())
                    .addQueryParameter("url", currentUrl);

            Request request = new Request.Builder()
                    .addHeader("sessionKey", sessionKey)
                    .url(httpBuilder.build())
                    .get()
                    .build();

            log.debug("[Get Reference Elements] Request: {}", request);
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.code() == 200) {
                    String result = response.body().string();
                    referenceElementsDto = objectMapper.readValue(result, new TypeReference<ReferenceElementsDto>() {
                    });
                }
            }
            log.debug("[Get Reference Elements] Response: {}", referenceElementsDto);
        } catch (Exception e) {
            log.warn("[Get Reference Elements] Error during call. Message: {}, Exception: {}", e.getMessage(), e);
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
            String bodyStr = objectMapper.writeValueAsString(selectorImitatorDto);
            RequestBody body = RequestBody.create(JSON, bodyStr);
            Request request = new Request.Builder()
                    .url(imitateUrl)
                    .post(body)
                    .build();
            log.debug("[Selector Imitate] Request: {}. Request body: {}", request, bodyStr);
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.code() == 200) {
                    String result = response.body().string();
                    List<Locator> locators = objectMapper.readValue(result, new TypeReference<List<Locator>>() {
                    });
                    log.debug("[Selector Imitate] Response: {}", locators);
                    return locators;
                }
            }
        } catch (Exception e) {
            log.warn("[Selector Imitate] Error during call. Message: {}, Exception: {}", e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public void initReport(String sessionId) {
        try {
            Request request = new Request.Builder()
                    .url(serverUrl + "/report/init/" + sessionId)
                    .post(RequestBody.create(new byte[0]))
                    .build();
            log.debug("[Init Report] Request: {}", request);
            okHttpClient.newCall(request).enqueue(httpCallback.asyncCall());
        } catch (Exception e) {
            log.warn("[Init Report] Error during call. Message: {}, Exception: {}", e.getMessage(), e);
        }
    }
}
