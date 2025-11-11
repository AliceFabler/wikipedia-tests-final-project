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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class BrowserstackDriver implements WebDriverProvider {

    private static final String FREE_FALLBACK_DEVICE = "Google Pixel 3";
    private static final String FREE_FALLBACK_OS     = "9.0";

    @Override
    public @NotNull WebDriver createDriver(@Nullable Capabilities incoming) {
        // Owner merges: System props, ENV, classpath:${env}.properties, classpath:auth/remote.properties
        AuthConfig auth   = ConfigFactory.create(AuthConfig.class, System.getProperties());
        RemoteConfig mobile = ConfigFactory.create(RemoteConfig.class, System.getProperties());

        URL hubUrl = toUrl(auth.getRemoteUrl(), "remoteUrl");

        // 1) W3C caps (Appium 3): platformName (топ-уровень) + appium:* + bstack:options
        MutableCapabilities caps = incoming == null ? new MutableCapabilities() : new MutableCapabilities(incoming);
        putIfAbsent(caps, "platformName", "Android");              // W3C стандарт
        putIfAbsent(caps, "appium:automationName", "UiAutomator2");

        String appProperty = Objects.toString(mobile.getApp(), "").trim();
        if (appProperty.isEmpty()) {
            throw new IllegalStateException("remote.properties: 'app' пустой. Укажи bs://<app-id> или путь к APK внутри репо.");
        }
        String appForBs = resolveAppForBrowserStack(appProperty, auth);
        caps.setCapability("appium:app", appForBs);

        Map<String, Object> bstack = new HashMap<>();
        req(bstack, "deviceName", mobile.getDevice(), "remote.properties: 'device' пустой");
        req(bstack, "osVersion",  mobile.getOsVersion(), "remote.properties: 'os_version' пустой");

        putIfNotBlank(bstack, "projectName", mobile.getProject());
        putIfNotBlank(bstack, "buildName",   mobile.getBuild());
        putIfNotBlank(bstack, "sessionName", mobile.getSessionName());

        // Версия Appium на стороне BS
        putIfNotBlank(bstack, "appiumVersion", nullTo(mobile.getAppiumVersion(), "2.19.0"));
        // Полезные флаги BrowserStack
//        putIfNotBlank(bstack, "networkLogs", nullTo(mobile.getNetworkLogs(), "true"));
        putIfNotBlank(bstack, "debug",       nullTo(mobile.getDebug(),       "true"));
        putIfNotBlank(bstack, "video",       nullTo(mobile.getVideo(),       "true"));

        // Pass-through: -Dbstack.* → внутрь bstack:options;  -Dappium:* → в caps
        propagateSystemPropsToBstackOptions(bstack, "bstack.");
        propagatePrefixedSystemProps(caps, "appium:");

        // Креды (совместимость с SDK и YAML)
        putIfNotBlank(bstack, "userName",  auth.getUserName());
        putIfNotBlank(bstack, "accessKey", auth.getKey());

        caps.setCapability("bstack:options", bstack);

        log.info("=== BrowserStack session ===");
        log.info("Hub: {}", hubUrl);
        log.info("Project/Build/Name: {}/{}/{}", mobile.getProject(), mobile.getBuild(), mobile.getSessionName());
        log.info("Device: {} / OS {}", bstack.get("deviceName"), bstack.get("osVersion"));
        log.info("App: {}", appForBs);
        log.info("User: {} / Key: {}", mask(auth.getUserName()), mask(auth.getKey()));

        boolean freeFallbackEnabled = !"false".equalsIgnoreCase(System.getProperty("bs.freePlan.fallback", "true"));

        try {
            return new RemoteWebDriver(hubUrl, caps);
        } catch (RuntimeException first) {
            if (freeFallbackEnabled && isFreePlanDeviceError(first)) {
                log.warn("BrowserStack Free plan limitation detected — retrying on {} / {}",
                        FREE_FALLBACK_DEVICE, FREE_FALLBACK_OS);
                // переключаем девайс/ОС в уже собранных bstack:options и пробуем ещё раз
                @SuppressWarnings("unchecked")
                Map<String, Object> bs = (Map<String, Object>) caps.getCapability("bstack:options");
                if (bs != null) {
                    bs.put("deviceName", FREE_FALLBACK_DEVICE);
                    bs.put("osVersion",  FREE_FALLBACK_OS);
                    // пометим сессию в дашборде
                    String sName = Objects.toString(bs.get("sessionName"), "UI");
                    bs.put("sessionName", sName + " (free fallback)");
                    caps.setCapability("bstack:options", bs);
                }
                try {
                    return new RemoteWebDriver(hubUrl, caps);
                } catch (RuntimeException second) {
                    throw wrap(second, hubUrl, mobile, auth, caps);
                }
            }
            throw wrap(first, hubUrl, mobile, auth, caps);
        }
    }

    /* ===================== app resolver / uploader ===================== */

    private static String resolveAppForBrowserStack(String appProp, AuthConfig auth) {
        // 1) уже bs://... — ок
        if (appProp.startsWith("bs://")) return appProp;
        // 2) http(s):// — ок (BS сам зальёт по URL)
        if (appProp.startsWith("http://") || appProp.startsWith("https://")) return appProp;

        // 3) путь к файлу (относительный к workspace/корню проекта, либо абсолютный)
        Path p = Path.of(appProp);
        if (!Files.exists(p)) {
            // попробуем относительный путь от текущей рабочей директории процесса
            p = Path.of(System.getProperty("user.dir"), appProp).normalize();
        }
        if (!Files.exists(p)) {
            throw new IllegalStateException("APK not found: " + appProp + " (cwd=" + System.getProperty("user.dir") + ")");
        }

        String uploaded = uploadApkToBrowserStack(p, auth);
        if (uploaded == null || !uploaded.startsWith("bs://")) {
            throw new IllegalStateException("BrowserStack upload failed or returned unexpected app_url: " + uploaded);
        }
        return uploaded;
    }

    /**
     * Загрузка APK в BrowserStack App Automate: POST /app-automate/upload (multipart/form-data)
     * Возвращает bs://<app-id> из поля "app_url".
     */
    private static String uploadApkToBrowserStack(Path apk, AuthConfig auth) {
        log.info("Uploading APK to BrowserStack: {}", apk);
        try {
            String boundary   = "----BSFormBoundary" + UUID.randomUUID();
            byte[] fileBytes  = Files.readAllBytes(apk);
            String filename   = apk.getFileName().toString();

            // multipart вручную (Java 11+ HttpClient)
            String partHeader = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n" +
                    "Content-Type: application/octet-stream\r\n\r\n";
            String partFooter = "\r\n--" + boundary + "--\r\n";

            byte[] headerBytes = partHeader.getBytes();
            byte[] footerBytes = partFooter.getBytes();
            byte[] body = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
            System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
            System.arraycopy(fileBytes, 0, body, headerBytes.length, fileBytes.length);
            System.arraycopy(footerBytes, 0, body, headerBytes.length + fileBytes.length, footerBytes.length);

            String basic = Base64.getEncoder().encodeToString((auth.getUserName() + ":" + auth.getKey()).getBytes());

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api-cloud.browserstack.com/app-automate/upload"))
                    .timeout(Duration.ofMinutes(5))
                    .header("Authorization", "Basic " + basic)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                String json = resp.body();
                // {"app_url":"bs://<id>", ...}
                Matcher m = Pattern.compile("\"app_url\"\\s*:\\s*\"(bs://[^\"]+)\"").matcher(json);
                if (m.find()) {
                    String url = m.group(1);
                    log.info("Uploaded to BrowserStack: {}", url);
                    return url;
                }
                log.error("Upload ok but 'app_url' not found. Response: {}", json);
                return null;
            } else {
                log.error("Upload failed. HTTP {} Body: {}", resp.statusCode(), resp.body());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Failed to upload APK to BrowserStack: " + e, e);
        }
    }

    /* ============================ utils ============================ */

    private static boolean isFreePlanDeviceError(Throwable t) {
        String full = collectMsg(t).toLowerCase(Locale.ROOT);
        return full.contains("not available in free plan")
                || full.contains("available in free plan");
    }

    private static String collectMsg(Throwable t) {
        StringBuilder sb = new StringBuilder();
        while (t != null) {
            if (t.getMessage() != null) sb.append(t.getMessage()).append("\n");
            t = t.getCause();
        }
        return sb.toString();
    }

    private static IllegalStateException wrap(RuntimeException e, URL hubUrl, RemoteConfig mobile,
                                              AuthConfig auth, Capabilities caps) {
        log.error("""
                  Failed BS session.
                  Hub={} device={} os={} app={} user={}
                  Caps={}
                  Cause={}
                  """,
                hubUrl, mobile.getDevice(), mobile.getOsVersion(), mobile.getApp(), auth.getUserName(),
                caps, e.toString(), e);
        return new IllegalStateException("BrowserStack driver creation failed. See logs.", e);
    }

    private static String nullTo(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }

    private static void putIfAbsent(MutableCapabilities caps, String key, Object value) {
        if (caps.getCapability(key) == null && value != null) caps.setCapability(key, value);
    }

    private static void putIfNotBlank(Map<String, Object> map, String key, String val) {
        if (val != null && !val.isBlank()) map.put(key, val);
    }

    private static void req(Map<String, Object> map, String key, String val, String err) {
        if (val == null || val.isBlank()) throw new IllegalStateException(err);
        map.put(key, val);
    }

    private static URL toUrl(String raw, String key) {
        try {
            return URI.create(raw).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Bad " + key + ": " + raw, e);
        }
    }

    private static String mask(String s) {
        if (s == null || s.isBlank()) return "<empty>";
        return s.length() <= 4 ? "***" : s.substring(0, 2) + "***";
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

    private static void propagateSystemPropsToBstackOptions(Map<String, Object> bstack, String prefix) {
        Properties sys = System.getProperties();
        for (Map.Entry<Object, Object> e : sys.entrySet()) {
            String key = String.valueOf(e.getKey());
            if (key.startsWith(prefix)) {
                String shortKey = key.substring(prefix.length());
                bstack.put(shortKey, e.getValue());
            }
        }
    }
}
