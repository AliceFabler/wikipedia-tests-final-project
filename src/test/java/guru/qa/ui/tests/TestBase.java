package guru.qa.ui.tests;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.logevents.SelenideLogger;
import guru.qa.ui.app.App;
import guru.qa.ui.drivers.BrowserstackDriver;
import guru.qa.ui.drivers.LocalDriver;
import guru.qa.ui.helpers.Attach;
import guru.qa.ui.logging.PrettySelenideRuListener;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.MDC;

import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.WebDriverRunner.hasWebDriverStarted;

/*
 * 🎯 MASTER PROMPT — TestBase (reverted) c доработками логирования/репортинга
 *
 * Цель:
 *   Базовый класс мобильных UI-тестов, который:
 *   — выбирает WebDriverProvider по deviceHost ("local"/"remote");
 *   — настраивает Selenide и слушатели: AllureSelenide + русский PrettySelenideRuListener;
 *   — перед каждым тестом создаёт сессию (open());
 *   — после каждого теста делает аттачи (local: скрин+сорс; remote: сорс+видео) и закрывает сессию.
 *
 * Важно:
 *   • Без @Step — шаги только через Allure.step(...) в тестах.
 *   • Не добавляем activateApp/terminateApp и «one-driver-per-run».
 *   • Не меняем выбранную вами схему "open/close per test".
 */
public class TestBase {

    /**
     * Хелпер: читаем deviceHost без изменения старой семантики (см. beforeAll).
     */
    @SuppressWarnings("unused")
    private static String deviceHost() {
        String fromSys = System.getProperty("deviceHost");
        if (fromSys != null && !fromSys.isBlank()) return fromSys.trim().toLowerCase();
        String fromEnv = System.getenv("DEVICE_HOST");
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv.trim().toLowerCase();
        return "local";
    }

    /**
     * Глобальная конфигурация: выбор драйвера, время ожиданий, лог-листенеры.
     * Семантика выбора deviceHost сохранена: приоритет у System.getProperty("deviceHost","local").
     */
    @BeforeAll
    static void beforeAll() {
        // 1) Выбор провайдера драйвера (без изменения вашей логики)
        String deviceHost = System.getProperty("deviceHost", "local");
        Configuration.browser = "remote".equalsIgnoreCase(deviceHost)
                ? BrowserstackDriver.class.getName()
                : LocalDriver.class.getName();

        // 2) Базовые настройки Selenide для мобилки
        Configuration.browserSize = null;      // у мобилки нет окна браузера
        Configuration.timeout = 30_000;        // общий timeout ожиданий
        Configuration.pageLoadTimeout = 0L;    // неактуально для нативных экранов
        Configuration.pageLoadStrategy = "none";
        Configuration.reportsFolder = ".allure-results"; // единое место артефактов для CI

        // 3) Слушатели логов
        // 3.1) Русский красивый лог Selenide (не дублируем)
        if (!SelenideLogger.hasListener("pretty-ru")) {
            SelenideLogger.addListener("pretty-ru", new PrettySelenideRuListener());
        }
        // 3.2) Интеграция с Allure: без автогенерации шагов Selenide (шаги — только в тестах)
        if (!SelenideLogger.hasListener("AllureSelenide")) {
            SelenideLogger.addListener("AllureSelenide",
                    new AllureSelenide()
                            .savePageSource(true)
                            .screenshots(true)
                            .includeSelenideSteps(false)
            );
        }
    }

    /**
     * Перед каждым тестом: помечаем имя теста в MDC (красивые логи) и создаём сессию.
     */
    @BeforeEach
    void beforeEach(TestInfo info) {
        // Имя теста попадёт в наш log4j2 шаблон как [%X{test}]
        if (info != null && info.getDisplayName() != null) {
            MDC.put("test", info.getDisplayName());
        }
        // Триггер создания сессии через ваш WebDriverProvider (без URL)
        open();
    }

    /**
     * После каждого теста: аттачи и закрытие сессии.
     * • remote: pageSource → close → video(sessionId)
     * • local: screenshot → pageSource → close
     */
    @AfterEach
    void afterEach() {
        if (!hasWebDriverStarted()) {
            // Драйвер не стартовал (например, Appium недоступен) — выходим тихо
            MDC.remove("test");
            return;
        }

        String deviceHost = System.getProperty("deviceHost", "local");
        try {
            if ("remote".equalsIgnoreCase(deviceHost)) {
                // Для BS сначала сохраним источник страницы, потом закроем сессию и приложим видео
                String sessionId = Selenide.sessionId() != null ? Selenide.sessionId().toString() : null;
                try {
                    Attach.pageSource();
                } catch (Throwable ignored) {
                }
                closeWebDriver();
                App.reset();
                if (sessionId != null) {
                    try {
                        Attach.addVideo(sessionId);
                    } catch (Throwable ignored) {
                    }
                }
            } else {
                // Локальный прогон: скриншот и page source до закрытия
                try {
                    Attach.screenshotAs("Last screenshot");
                } catch (Throwable ignored) {
                }
                try {
                    Attach.pageSource();
                } catch (Throwable ignored) {
                }
                closeWebDriver();
                App.reset();
            }
        } finally {
            // Чистим контекст логов в любом случае
            MDC.remove("test");
        }
    }
}
