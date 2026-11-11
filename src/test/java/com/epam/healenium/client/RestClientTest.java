package com.epam.healenium.client;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.remote.http.HttpRequest;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class RestClientTest {

    @Test
    public void testApiKeyResolvedFromConfig() {
        Map<String, Object> map = new HashMap<>();
        map.put("sessionKey", "test-session-key");
        map.put("hlm.server.url", "http://localhost:8085");
        map.put("hlm.imitator.url", "http://localhost:8000");
        map.put("hlm.api-key", "hlm_live_test_api_key_12345");

        Config config = ConfigFactory.parseMap(map);
        RestClient client = new RestClient(config);

        Assertions.assertEquals("hlm_live_test_api_key_12345", client.getApiKey());
        Assertions.assertTrue(client.hasApiKey());
    }

    @Test
    public void testApiKeyOmittedDefaultsToNull() {
        Map<String, Object> map = new HashMap<>();
        map.put("sessionKey", "test-session-key");
        map.put("hlm.server.url", "http://localhost:7878");
        map.put("hlm.imitator.url", "http://localhost:8000");

        Config config = ConfigFactory.parseMap(map);
        RestClient client = new RestClient(config);

        Assertions.assertNull(client.getApiKey());
        Assertions.assertFalse(client.hasApiKey());
    }

    @Test
    public void testSetAuthHeadersAppliesApiKeyHeader() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("sessionKey", "test-session-key");
        map.put("hlm.server.url", "http://localhost:8085");
        map.put("hlm.imitator.url", "http://localhost:8000");
        map.put("hlm.api-key", "hlm_live_test_api_key_12345");

        Config config = ConfigFactory.parseMap(map);
        RestClient client = new RestClient(config);

        HttpRequest request = new HttpRequest(org.openqa.selenium.remote.http.HttpMethod.GET, "/elements");
        Method method = RestClient.class.getDeclaredMethod("setAuthHeaders", HttpRequest.class);
        method.setAccessible(true);
        method.invoke(client, request);

        Assertions.assertEquals("hlm_live_test_api_key_12345", request.getHeader(RestClient.API_KEY_HEADER));
    }

    @Test
    public void testSetAuthHeadersOmittedWhenNoApiKey() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("sessionKey", "test-session-key");
        map.put("hlm.server.url", "http://localhost:7878");
        map.put("hlm.imitator.url", "http://localhost:8000");

        Config config = ConfigFactory.parseMap(map);
        RestClient client = new RestClient(config);

        HttpRequest request = new HttpRequest(org.openqa.selenium.remote.http.HttpMethod.GET, "/elements");
        Method method = RestClient.class.getDeclaredMethod("setAuthHeaders", HttpRequest.class);
        method.setAccessible(true);
        method.invoke(client, request);

        Assertions.assertNull(request.getHeader(RestClient.API_KEY_HEADER));
    }

    @Test
    public void testHasApiKey() {
        Map<String, Object> mapWithApiKey = new HashMap<>();
        mapWithApiKey.put("sessionKey", "test-session-key");
        mapWithApiKey.put("hlm.server.url", "http://localhost:8085");
        mapWithApiKey.put("hlm.imitator.url", "http://localhost:8000");
        mapWithApiKey.put("hlm.api.key", "some-api-key");
        RestClient clientWithApiKey = new RestClient(ConfigFactory.parseMap(mapWithApiKey));
        Assertions.assertTrue(clientWithApiKey.hasApiKey());

        Map<String, Object> mapOss = new HashMap<>();
        mapOss.put("sessionKey", "test-session-key");
        mapOss.put("hlm.server.url", "http://localhost:7878");
        mapOss.put("hlm.imitator.url", "http://localhost:8000");
        RestClient clientOss = new RestClient(ConfigFactory.parseMap(mapOss));
        Assertions.assertFalse(clientOss.hasApiKey());
    }

    @Test
    public void testGetTenantSettingsGracefulFallbackWhenBackendUnavailable() {
        Map<String, Object> map = new HashMap<>();
        map.put("sessionKey", "test-session-key");
        // Non-existent port to test connection error fallback
        map.put("hlm.server.url", "http://localhost:59999");
        map.put("hlm.imitator.url", "http://localhost:8000");
        map.put("hlm.api.key", "hlm_test_key");

        RestClient client = new RestClient(ConfigFactory.parseMap(map));
        // Should not throw exception, but return Optional.empty()
        Assertions.assertFalse(client.getTenantSettings().isPresent());
    }

    @Test
    public void testAiUrlUsesHlmAiUrlWhenConfigured() {
        Map<String, Object> map = new HashMap<>();
        map.put("sessionKey", "test-session-key");
        map.put("hlm.server.url", "http://localhost:7878");
        map.put("hlm.imitator.url", "http://localhost:8000");
        map.put("hlm.ai.url", "http://localhost:6565");

        RestClient client = new RestClient(ConfigFactory.parseMap(map));
        Assertions.assertEquals("http://localhost:6565/healenium-ai", client.getAiServiceUrl());
    }

    @Test
    public void testAiUrlFallsBackToServerBaseWhenOmitted() {
        Map<String, Object> map = new HashMap<>();
        map.put("sessionKey", "test-session-key");
        map.put("hlm.server.url", "http://localhost:8085");
        map.put("hlm.imitator.url", "http://localhost:8000");

        RestClient client = new RestClient(ConfigFactory.parseMap(map));
        Assertions.assertEquals("http://localhost:8085/healenium-ai", client.getAiServiceUrl());
    }
}
