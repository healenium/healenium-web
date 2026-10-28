package com.epam.healenium;

import com.epam.healenium.model.TenantSettingsDto;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;

import java.util.UUID;

public class SelfHealingEngineSettingsTest {

    @AfterEach
    public void tearDown() {
        System.clearProperty("heal-enabled");
        System.clearProperty("recovery-tries");
        System.clearProperty("score-cap");
        System.clearProperty("selector-type");
        System.clearProperty("healed-type");
        ConfigFactory.invalidateCaches();
    }

    private RemoteWebDriver createMockDriver() {
        return new RemoteWebDriver() {
            @Override
            public SessionId getSessionId() {
                return new SessionId("test-session-123");
            }
        };
    }

    @Test
    public void testTenantSettingsOverrideDefaults() {
        RemoteWebDriver mockDriver = createMockDriver();
        SelfHealingEngine engine = new SelfHealingEngine(mockDriver);

        // Verify initial defaults
        Assertions.assertTrue(engine.isHealingEnabled());
        Assertions.assertEquals(0.6, engine.getScoreCap(), 0.001);

        // Apply remote tenant settings
        TenantSettingsDto remote = new TenantSettingsDto()
                .setTenantId(UUID.randomUUID())
                .setHealEnabled(false)
                .setRecoveryTries(3)
                .setScoreCap(0.85)
                .setHealedType("xpath");

        engine.applyTenantSettings(remote);

        // Verify cascaded values
        Assertions.assertFalse(engine.isHealingEnabled());
        Assertions.assertEquals(0.85, engine.getScoreCap(), 0.001);
        Assertions.assertEquals(3, engine.getConfig().getInt("recovery-tries"));
        Assertions.assertEquals("xpath", engine.getConfig().getString("selector-type"));
        Assertions.assertEquals("xpath", engine.getConfig().getString("healed-type"));
    }

    @Test
    public void testJvmSystemPropertiesOverrideTenantSettings() {
        String propKey = "heal-enabled";
        System.setProperty(propKey, "true");
        ConfigFactory.invalidateCaches();
        try {
            RemoteWebDriver mockDriver = createMockDriver();
            SelfHealingEngine engine = new SelfHealingEngine(mockDriver);

            // Remote says healEnabled = false, but JVM -Dheal-enabled=true
            TenantSettingsDto remote = new TenantSettingsDto()
                    .setHealEnabled(false)
                    .setRecoveryTries(4);

            engine.applyTenantSettings(remote);

            // JVM property must win!
            Assertions.assertTrue(engine.isHealingEnabled());
            Assertions.assertTrue(engine.getConfig().getBoolean("heal-enabled"));
            // recoveryTries was not set via JVM, so remote value applies
            Assertions.assertEquals(4, engine.getConfig().getInt("recovery-tries"));
        } finally {
            System.clearProperty(propKey);
        }
    }

    @Test
    public void testPartialTenantSettings() {
        RemoteWebDriver mockDriver = createMockDriver();
        SelfHealingEngine engine = new SelfHealingEngine(mockDriver);

        TenantSettingsDto remote = new TenantSettingsDto()
                .setRecoveryTries(5);

        engine.applyTenantSettings(remote);

        // Only recovery-tries is updated, others remain defaults
        Assertions.assertEquals(5, engine.getConfig().getInt("recovery-tries"));
        Assertions.assertTrue(engine.isHealingEnabled());
        Assertions.assertEquals(0.6, engine.getScoreCap(), 0.001);
    }

    @Test
    public void testCssSelectorNormalization() {
        RemoteWebDriver mockDriver = createMockDriver();
        SelfHealingEngine engine = new SelfHealingEngine(mockDriver);

        TenantSettingsDto remote = new TenantSettingsDto()
                .setHealedType("css");

        engine.applyTenantSettings(remote);

        Assertions.assertEquals("css", engine.getConfig().getString("healed-type"));
        Assertions.assertEquals("cssSelector", engine.getConfig().getString("selector-type"));
    }
}
