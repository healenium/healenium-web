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
import com.epam.healenium.mapper.HealeniumMapper;
import com.epam.healenium.mapper.HealeniumMapperImpl;
import com.epam.healenium.model.LastHealingDataDto;
import com.epam.healenium.model.MetricsDto;
import com.epam.healenium.model.RequestDto;
import com.epam.healenium.treecomparing.Node;
import com.epam.healenium.treecomparing.Scored;
import com.epam.healenium.utils.SystemUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.openqa.selenium.By;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


/**
 * Wrapper for {@code RestTemplate} class.
 * Main purpose - encapsulate consumer from really used client and invocation complexity
 */

@Slf4j
public class RestClient {

    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final String baseUrl;
    private final String sessionKey;
    private final ObjectMapper objectMapper;
    private final HealeniumMapper mapper;

    public RestClient(Config config) {
        objectMapper = initMapper();
        baseUrl = "http://" + config.getString("serverHost") + ":" + config.getInt("serverPort") + "/healenium";
        sessionKey = config.hasPath("sessionKey") ? config.getString("sessionKey") : "";
        mapper = new HealeniumMapperImpl();
    }

    private OkHttpClient okHttpClient() {
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
     * @param by element By locator
     * @param nodePath List of nodes
     * @param element StackTraceElement
     */
    public void selectorsRequest(By by, StackTraceElement element, List<List<Node>> nodePath) {
        RequestDto requestDto = mapper.buildDto(by, element, nodePath);
        try {
            RequestBody body = RequestBody.create(JSON, objectMapper.writeValueAsString(requestDto));
            Request request = new Request.Builder()
                    .url(baseUrl)
                    .post(body)
                    .build();
            okHttpClient().newCall(request).execute();
        } catch (Exception e) {
            log.warn("Failed to make response");
        }
    }

    /**
     * Collect results from previous healing
     * @param locator By locator
     * @param element StackTraceElement
     * @param page pageObject name
     * @param choices scored By locators
     * @param healed newly healed locator
     * @param screenshot image with healed element
     * @param healingTime healing time
     * @param metricsDto
     */
    public void healRequest(By locator, StackTraceElement element, String page, List<Scored<By>> choices,
                            Scored<By> healed, byte[] screenshot, String healingTime, MetricsDto metricsDto) {
        RequestDto requestDto = mapper.buildDto(locator, element, page, choices, healed, screenshot);
        try {
            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("screenshot", buildScreenshotName(), RequestBody.create(MediaType.parse("image/png"), screenshot))
                    .addFormDataPart("dto", objectMapper.writeValueAsString(requestDto))
                    .addFormDataPart("metrics", objectMapper.writeValueAsString(metricsDto))
                    .build();

            Request request = new Request.Builder()
                    .addHeader("sessionKey", sessionKey)
                    .addHeader("hostProject", SystemUtils.getHostProjectName())
                    .addHeader("healingTime", healingTime)
                    .url(baseUrl + "/healing")
                    .post(requestBody)
                    .build();
            okHttpClient().newCall(request).execute();
        } catch (Exception e) {
            log.warn("Failed to make response", e);
        }
    }

    /**
     * Get node path for given selector
     * @param locator element By locator
     * @param element StackTraceElement
     * @return nodes
     */
    public Optional<LastHealingDataDto> getLastHealingData(By locator, StackTraceElement element) {
        LastHealingDataDto lastHealingDataDto = null;
        RequestDto requestDto = mapper.buildDto(locator, element);
        try {
            HttpUrl.Builder httpBuilder = HttpUrl.parse(baseUrl).newBuilder()
                    .addQueryParameter("locator", requestDto.getLocator())
                    .addQueryParameter("className", requestDto.getClassName())
                    .addQueryParameter("methodName", requestDto.getMethodName());

            Request request = new Request.Builder()
                    .addHeader("sessionKey", sessionKey)
                    .url(httpBuilder.build())
                    .get()
                    .build();
            Response response = okHttpClient().newCall(request).execute();
            if (response.code() == 200) {
                String result = response.body().string();
                lastHealingDataDto = objectMapper.readValue(result, new TypeReference<LastHealingDataDto>() {});
            }
        } catch (Exception ex) {
            log.warn("Failed to make response", ex);
        }
        return Optional.ofNullable(lastHealingDataDto);
    }

    /**
     *
     * @return
     */
    private String buildScreenshotName() {
        return "screenshot_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy-hh-mm-ss").withLocale(Locale.US)) + ".png";
    }

    public HealeniumMapper getMapper() {
        return mapper;
    }
}
