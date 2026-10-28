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
        map.put("hlm.ai.url", "http://localhost:6565");
        map.put("hlm.api.key", "hlm_live_test_api_key_12345");
        map.put("hlm.tenant.id", "ae26c18d-1ecd-4b80-a291-44316d4384f0");

        Config config = ConfigFactory.parseMap(map);
        RestClient client = new RestClient(config);

        Assertions.assertEquals("hlm_live_test_api_key_12345", client.getApiKey());
        Assertions.assertEquals("ae26c18d-1ecd-4b80-a291-44316d4384f0", client.getTenantId());
    }

    @Test
    public void testApiKeyOmittedDefaultsToNull() {
        Map<String, Object> map = new HashMap<>();
        map.put("sessionKey", "test-session-key");
        map.put("hlm.server.url", "http://localhost:7878");
        map.put("hlm.imitator.url", "http://localhost:8000");
        map.put("hlm.ai.url", "http://localhost:6565");

        Config config = ConfigFactory.parseMap(map);
        RestClient client = new RestClient(config);

        Assertions.assertNull(client.getApiKey());
        Assertions.assertNull(client.getTenantId());
    }

    @Test
    public void testSetAuthHeadersAppliesApiKeyAndTenantHeader() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("sessionKey", "test-session-key");
        map.put("hlm.server.url", "http://localhost:8085");
        map.put("hlm.imitator.url", "http://localhost:8000");
        map.put("hlm.ai.url", "http://localhost:6565");
        map.put("hlm.api.key", "hlm_live_test_api_key_12345");
        map.put("hlm.tenant.id", "ae26c18d-1ecd-4b80-a291-44316d4384f0");

        Config config = ConfigFactory.parseMap(map);
        RestClient client = new RestClient(config);

        HttpRequest request = new HttpRequest(org.openqa.selenium.remote.http.HttpMethod.GET, "/elements");
        Method method = RestClient.class.getDeclaredMethod("setAuthHeaders", HttpRequest.class);
        method.setAccessible(true);
        method.invoke(client, request);

        Assertions.assertEquals("hlm_live_test_api_key_12345", request.getHeader(RestClient.API_KEY_HEADER));
        Assertions.assertEquals("ae26c18d-1ecd-4b80-a291-44316d4384f0", request.getHeader("Healenium-Tenant-Id"));
    }

    @Test
    public void testSetAuthHeadersOmittedWhenNoApiKey() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("sessionKey", "test-session-key");
        map.put("hlm.server.url", "http://localhost:7878");
        map.put("hlm.imitator.url", "http://localhost:8000");
        map.put("hlm.ai.url", "http://localhost:6565");

        Config config = ConfigFactory.parseMap(map);
        RestClient client = new RestClient(config);

        HttpRequest request = new HttpRequest(org.openqa.selenium.remote.http.HttpMethod.GET, "/elements");
        Method method = RestClient.class.getDeclaredMethod("setAuthHeaders", HttpRequest.class);
        method.setAccessible(true);
        method.invoke(client, request);

        Assertions.assertNull(request.getHeader(RestClient.API_KEY_HEADER));
        Assertions.assertNull(request.getHeader("Healenium-Tenant-Id"));
    }

    @Test
    public void testHasTenantOrApiKey() {
        Map<String, Object> mapWithApiKey = new HashMap<>();
        mapWithApiKey.put("sessionKey", "test-session-key");
        mapWithApiKey.put("hlm.server.url", "http://localhost:8085");
        mapWithApiKey.put("hlm.imitator.url", "http://localhost:8000");
        mapWithApiKey.put("hlm.ai.url", "http://localhost:6565");
        mapWithApiKey.put("hlm.api.key", "some-api-key");
        RestClient clientWithApiKey = new RestClient(ConfigFactory.parseMap(mapWithApiKey));
        Assertions.assertTrue(clientWithApiKey.hasTenantOrApiKey());

        Map<String, Object> mapWithTenantOnly = new HashMap<>();
        mapWithTenantOnly.put("sessionKey", "test-session-key");
        mapWithTenantOnly.put("hlm.server.url", "http://localhost:8085");
        mapWithTenantOnly.put("hlm.imitator.url", "http://localhost:8000");
        mapWithTenantOnly.put("hlm.ai.url", "http://localhost:6565");
        mapWithTenantOnly.put("hlm.tenant.id", "some-tenant-id");
        RestClient clientWithTenant = new RestClient(ConfigFactory.parseMap(mapWithTenantOnly));
        Assertions.assertTrue(clientWithTenant.hasTenantOrApiKey());

        Map<String, Object> mapOss = new HashMap<>();
        mapOss.put("sessionKey", "test-session-key");
        mapOss.put("hlm.server.url", "http://localhost:7878");
        mapOss.put("hlm.imitator.url", "http://localhost:8000");
        mapOss.put("hlm.ai.url", "http://localhost:6565");
        RestClient clientOss = new RestClient(ConfigFactory.parseMap(mapOss));
        Assertions.assertFalse(clientOss.hasTenantOrApiKey());
    }

    @Test
    public void testGetTenantSettingsGracefulFallbackWhenBackendUnavailable() {
        Map<String, Object> map = new HashMap<>();
        map.put("sessionKey", "test-session-key");
        // Non-existent port to test connection error fallback
        map.put("hlm.server.url", "http://localhost:59999");
        map.put("hlm.imitator.url", "http://localhost:8000");
        map.put("hlm.ai.url", "http://localhost:6565");
        map.put("hlm.api.key", "hlm_test_key");

        RestClient client = new RestClient(ConfigFactory.parseMap(map));
        // Should not throw exception, but return Optional.empty()
        Assertions.assertTrue(client.getTenantSettings().isEmpty());
    }
}
