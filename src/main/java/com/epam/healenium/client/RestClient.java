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
import com.epam.healenium.model.Context;
import com.epam.healenium.model.HealedElement;
import com.epam.healenium.model.HealeniumSelectorImitatorDto;
import com.epam.healenium.model.HealingResult;
import com.epam.healenium.model.HealingResultRequestDto;
import com.epam.healenium.model.LastHealingDataDto;
import com.epam.healenium.model.Locator;
import com.epam.healenium.model.RequestDto;
import com.epam.healenium.treecomparing.Node;
import com.epam.healenium.treecomparing.Scored;
import com.epam.healenium.utils.StackTraceReader;
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
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * Wrapper for {@code RestTemplate} class.
 * Main purpose - encapsulate consumer from really used client and invocation complexity
 */

@Slf4j
@Data
public class RestClient {

    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final String serverUrl;
    private final String imitateUrl;
    private final String sessionKey;
    private ObjectMapper objectMapper;
    private HealeniumMapper mapper;

    public RestClient(Config config) {
        objectMapper = initMapper();
        if (config.hasPath("hlm.server.url")) {
            String hlmServerUrl = config.getString("hlm.server.url");
            serverUrl = (hlmServerUrl.startsWith("http") ? hlmServerUrl : "http://".concat(hlmServerUrl)).concat("/healenium");
            String hlmImitatorUrl = config.getString("hlm.imitator.url");
            imitateUrl = (hlmImitatorUrl.startsWith("http") ? hlmImitatorUrl : "http://".concat(hlmImitatorUrl)).concat("/imitate");
        } else {
            serverUrl = "http://" + config.getString("serverHost") + ":" + config.getInt("serverPort") + "/healenium";
            imitateUrl = "http://" + config.getString("imitateHost") + ":" + config.getInt("imitatePort") + "/imitate";
        }
        sessionKey = config.hasPath("sessionKey") ? config.getString("sessionKey") : StringUtils.EMPTY;
        mapper = new HealeniumMapper(new StackTraceReader());
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
     *
     * @param by         element By locator
     * @param nodePath   List of nodes
     * @param currentUrl url of web page
     */
    public void selectorsRequest(By by, List<List<Node>> nodePath, String currentUrl) {
        RequestDto requestDto = mapper.buildDto(by, nodePath, currentUrl);
        try {
            RequestBody body = RequestBody.create(JSON, objectMapper.writeValueAsString(requestDto));
            Request request = new Request.Builder()
                    .url(serverUrl)
                    .post(body)
                    .build();
            okHttpClient().newCall(request).execute().close();
        } catch (Exception e) {
            log.warn("Failed to make response of 'selectorsRequest' request." + e);
        }
    }

    /**
     * Collect results from previous healing
     *
     * @param context healing context
     */
    public void healRequest(Context context) {
        try {
            List<HealingResultRequestDto> resultRequestDtos = new ArrayList<>();
            for (HealingResult healingResult : context.getHealingResults()) {
                List<Scored<By>> choices = healingResult.getHealedElements().stream()
                        .map(HealedElement::getScored)
                        .collect(Collectors.toList());
                if (choices.isEmpty()) {
                    return;
                }
                String metrics = objectMapper.writeValueAsString(healingResult.getMetricsDto());
                resultRequestDtos.add(mapper.buildMultRequest(context, healingResult, choices, metrics));
            }

            RequestBody requestBody = RequestBody.create(JSON, objectMapper.writeValueAsString(resultRequestDtos));

            Request request = new Request.Builder()
                    .addHeader("sessionKey", sessionKey)
                    .addHeader("hostProject", SystemUtils.getHostProjectName())
                    .url(serverUrl + "/healing")
                    .post(requestBody)
                    .build();
            okHttpClient().newCall(request).execute().close();
        } catch (Exception e) {
            log.warn("Failed to make response of 'healRequest' request. ", e);
        }
    }

    /**
     * Get node path for given selector
     *
     * @param locator    element By locator
     * @param currentUrl url of web page
     * @return lastHealingDataDto   previous success healing data
     */
    public Optional<LastHealingDataDto> getLastHealingData(By locator, String currentUrl) {
        LastHealingDataDto lastHealingDataDto = null;
        RequestDto requestDto = mapper.buildDto(locator, currentUrl);
        try {
            HttpUrl.Builder httpBuilder = HttpUrl.parse(serverUrl).newBuilder()
                    .addQueryParameter("locator", requestDto.getLocator())
                    .addQueryParameter("className", requestDto.getClassName())
                    .addQueryParameter("methodName", requestDto.getMethodName())
                    .addQueryParameter("url", currentUrl);

            Request request = new Request.Builder()
                    .addHeader("sessionKey", sessionKey)
                    .url(httpBuilder.build())
                    .get()
                    .build();
            Response response = okHttpClient().newCall(request).execute();
            if (response.code() == 200) {
                ResponseBody responseBody = response.body();
                String result = responseBody.string();
                lastHealingDataDto = objectMapper.readValue(result, new TypeReference<LastHealingDataDto>() {
                });
                responseBody.close();
            }
        } catch (Exception ex) {
            log.warn("Failed to make response of 'getLastHealingData' request. ", ex);
        }
        return Optional.ofNullable(lastHealingDataDto);
    }

    /**
     * Get imitated locators by target node and user selector from selector-imitator service
     *
     * @param healeniumSelectorImitatorDto element By locator
     * @return locators
     */
    public List<Locator> imitate(HealeniumSelectorImitatorDto healeniumSelectorImitatorDto) {
        List<Locator> locators = new ArrayList<>();
        try {
            RequestBody body = RequestBody.create(JSON, objectMapper.writeValueAsString(healeniumSelectorImitatorDto));
            HttpUrl.Builder httpBuilder = HttpUrl.parse(imitateUrl).newBuilder();
            Request request = new Request.Builder()
                    .url(httpBuilder.build())
                    .post(body)
                    .build();
            try (Response response = okHttpClient().newCall(request).execute()) {
                if (response.code() == 200) {
                    String result = response.body().string();
                    locators = objectMapper.readValue(result, new TypeReference<List<Locator>>() {
                    });
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to make imitate response of 'imitate' request. Message: {}", ex.getMessage());
        }
        return locators;
    }

}
