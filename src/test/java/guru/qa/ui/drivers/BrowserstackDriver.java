package guru.qa.ui.drivers;

import com.codeborne.selenide.WebDriverProvider;
import guru.qa.ui.config.AuthConfig;
import guru.qa.ui.config.RemoteConfig;
import lombok.extern.slf4j.Slf4j;
import org.aeonbits.owner.ConfigFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class BrowserstackDriver implements WebDriverProvider {

    private static @NotNull MutableCapabilities getCapabilities(@Nullable Capabilities capabilities, AuthConfig auth, RemoteConfig mobile) {
        MutableCapabilities caps = capabilities == null
                ? new MutableCapabilities()
                : new MutableCapabilities(capabilities);

        // Auth (masked in logs)
        caps.setCapability("browserstack.user", auth.getUserName());
        caps.setCapability("browserstack.key", auth.getKey());

        // App Automate core
        caps.setCapability("app", mobile.getApp());              // e.g. bs://<app-id>
        caps.setCapability("device", mobile.getDevice());        // e.g. "Google Pixel 3"
        caps.setCapability("os_version", mobile.getOsVersion()); // e.g. "10.0"

        // Meta
        caps.setCapability("project", mobile.getProject());      // defaults preserved
        caps.setCapability("build", mobile.getBuild());
        caps.setCapability("name", mobile.getSessionName());
        return caps;
    }

    private static void propagatePrefixedSystemProps(MutableCapabilities caps, String prefix) {
        Properties sys = System.getProperties();
        for (Map.Entry<Object, Object> e : sys.entrySet()) {
            String key = String.valueOf(e.getKey());
            if (key.startsWith(prefix)) {
                caps.setCapability(key, e.getValue());
            }
        }
    }

    /* ================================= helpers ================================= */

    @SuppressWarnings("SameParameterValue")
    private static URL toUrl(String raw, String key) {
        try {
            return URI.create(raw).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Bad " + key + ": " + raw, e);
        }
    }

    private static String mask(String secret) {
        if (secret == null || secret.isBlank()) return "<empty>";
        return secret.length() <= 4 ? "***" : secret.substring(0, 2) + "***";
    }

    @Override
    public @NotNull WebDriver createDriver(@Nullable Capabilities capabilities) {
        // Owner merges: System props, ENV, classpath:${env}.properties, classpath:auth/remote.properties
        AuthConfig auth = ConfigFactory.create(AuthConfig.class, System.getProperties());
        RemoteConfig mobile = ConfigFactory.create(RemoteConfig.class, System.getProperties());

        // Start with incoming caps (if any), then enrich
        MutableCapabilities caps = getCapabilities(capabilities, auth, mobile);

        // Pass-through any -Dbstack.* and -Dappium:* as-is (useful for fine-tuning)
        propagatePrefixedSystemProps(caps, "bstack.");
        propagatePrefixedSystemProps(caps, "appium:");

        URL hubUrl = toUrl(auth.getRemoteUrl(), "remoteUrl");

        log.info("Starting BrowserStack session");
        log.info("Hub: {}", hubUrl);
        log.info("Project/Build/Name: {}/{}/{}", mobile.getProject(), mobile.getBuild(), mobile.getSessionName());
        log.info("Device: {} / OS {}", mobile.getDevice(), mobile.getOsVersion());
        log.info("App: {}", mobile.getApp());
        log.info("User: {} / Key: {}***", auth.getUserName(), mask(auth.getKey()));

        try {
            return new RemoteWebDriver(hubUrl, caps);
        } catch (RuntimeException e) {
            log.error("""
                            Failed to create RemoteWebDriver for BrowserStack.
                            Check:
                              • remoteUrl='{}'
                              • app='{}' (should be bs://<app-id> or uploaded)
                              • device='{}', os_version='{}'
                              • credentials user='{}'
                            Cause: {}
                            """, hubUrl, mobile.getApp(), mobile.getDevice(), mobile.getOsVersion(),
                    auth.getUserName(), e.toString(), e);
            throw new IllegalStateException("BrowserStack driver creation failed. See logs.", e);
        }
    }
}
