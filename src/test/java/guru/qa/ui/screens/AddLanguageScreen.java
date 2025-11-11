package guru.qa.ui.screens;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.appium.SelenideAppiumCollection;
import com.codeborne.selenide.appium.SelenideAppiumElement;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static com.codeborne.selenide.appium.SelenideAppium.$;
import static com.codeborne.selenide.appium.SelenideAppium.$$;
import static guru.qa.ui.allure.Steps.step;
import static io.appium.java_client.AppiumBy.xpath;

/**
 * Экран «Добавить язык» (Wikipedia Android).
 *
 * <p><b>Назначение:</b> верификация открытия экрана, выбор языка (первая строка)
 * и запоминание названий (эндоним и, при наличии, английский экзоным).
 * Для строк списка используются структурные XPath 2.0-локаторы и правило кликабельности.</p>
 *
 * <p><b>Инварианты:</b> Java 21, Appium 3 + UiAutomator2 (XPath 2.0), Selenide-Appium;
 * без UiSelector/TouchAction и Thread.sleep — только {@link Condition}-ожидания.</p>
 *
 * <p><b>Публичные методы:</b>
 * {@link #shouldBeOpen()}, {@link #selectFirstLanguageAndRemember()}, {@link #getRememberedLanguagePretty()}.</p>
 */
@Slf4j
public class AddLanguageScreen {

    //region Locators

    /** Первый TextView в строке — эндоним. */
    static final String ROW_LOCAL_NAME_REL =
            ".//android.widget.TextView[normalize-space(@text)!=''][1]";
    /** Последний TextView в строке — англ. экзоным (если есть). */
    static final String ROW_EN_NAME_REL =
            ".//android.widget.TextView[normalize-space(@text)!=''][position()=last()]";

    /** Кнопка «Назад» (EN/RU) по content-desc. */
    SelenideAppiumElement backButton = $(
            xpath(
                    "//*[child::*[(contains(normalize-space(@content-desc),'Go back') or contains(normalize-space(@content-desc),'Назад'))]" +
                            " and child::*[contains(@class,'Button')]]"
            )
    );

    /** Кликабельные строки под секцией “All languages/Все языки”. */
    SelenideAppiumCollection languageRows = $$(
            xpath(
                    "//*[child::*[contains(normalize-space(@text),'All languages') or contains(normalize-space(@text),'Все языки')]]" +
                            "//android.view.View[@clickable='true']"
            )
    );

    //endregion

    //region Remembered selection
    @Getter
    String rememberedLanguageLocal;   // напр., "Español"
    @Getter
    String rememberedLanguageEnglish; // напр., "Spanish"
    //endregion

    //region Helpers

    /** Безопасная подстановка строки в XPath. */
    private static String xq(String s) {
        if (s == null) return "''";
        if (!s.contains("'")) return "'" + s + "'";
        return "concat('" + s.replace("'", "',\"'\",'") + "')";
    }

    //endregion

    /**
     * Проверить, что экран открыт: «Назад» виден, список языков не пуст.
     *
     * @return текущий экран
     * @throws AssertionError если тулбар/список недоступен
     */
    public AddLanguageScreen shouldBeOpen() {
        step("Проверить, что экран «Добавить язык» открыт", () -> {
            step("Тулбар видим (кнопка «Назад»)", () -> {
                try {
                    backButton.shouldBe(Condition.visible);
                    log.debug("[AddLanguage] backButton видим.");
                } catch (Throwable t) {
                    log.error("[AddLanguage] Тулбар/Back не найден: {}", t.getMessage(), t);
                    throw new AssertionError("Ожидалась видимость кнопки «Назад», элемент не найден/невиден.", t);
                }
            });

            step("Список языков не пуст", () -> {
                try {
                    languageRows.shouldBe(CollectionCondition.sizeGreaterThan(0));
                    log.debug("[AddLanguage] languageRows.size={}", languageRows.size());
                } catch (Throwable t) {
                    log.error("[AddLanguage] Пустой или недоступный список: {}", t.getMessage(), t);
                    throw new AssertionError("Ожидался непустой список языков под «All languages/Все языки».", t);
                }
            });

            step("Лог примера первой строки", () -> {
                try {
                    String sample = languageRows.first().$(xpath(ROW_LOCAL_NAME_REL)).getText();
                    log.info("[AddLanguage] Первая строка отображает: {}", sample);
                } catch (Throwable t) {
                    log.warn("[AddLanguage] Не удалось прочитать текст первой строки: {}", t.getMessage());
                }
            });
        });
        return this;
    }

    /**
     * Выбрать первую строку списка и запомнить тексты.
     *
     * @return текущий экран
     * @throws AssertionError если список пуст или клик не удался
     */
    public AddLanguageScreen selectFirstLanguageAndRemember() {
        step("Выбрать первый язык и запомнить его", () -> {
            step("Убедиться, что список не пуст", () -> {
                try {
                    languageRows.shouldBe(CollectionCondition.sizeGreaterThan(0));
                } catch (Throwable t) {
                    log.error("[AddLanguage] Нет строк для выбора: {}", t.getMessage(), t);
                    throw new AssertionError("Нет доступных строк языков для выбора.", t);
                }
            });

            final SelenideAppiumElement row = step("Взять первую строку (visible)", () -> {
                try {
                    SelenideAppiumElement r = languageRows.first().shouldBe(Condition.visible);
                    log.debug("[AddLanguage] Первая строка видима.");
                    return r;
                } catch (Throwable t) {
                    log.error("[AddLanguage] Первая строка недоступна: {}", t.getMessage(), t);
                    throw new AssertionError("Первая строка языков не найдена/невидима.", t);
                }
            });

            step("Запомнить тексты выбранной строки", () -> rememberRowTexts(row));
            step("Клик по первой строке (правило кликабельности)", () -> clickWithRule("Первая строка языка", row));
            step("Лог выбора", () -> logSelection("первый язык"));
        });
        return this;
    }

    private void clickWithRule(String humanName, SelenideAppiumElement el) {
        try {
            el.shouldBe(Condition.visible);
            if (isClickReady(el)) {
                el.click();
                log.debug("[AddLanguage] Клик по «{}» выполнен (прямой).", humanName);
                return;
            }
            SelenideAppiumElement child = $(el.$(xpath(".//*[@clickable='true' or @focusable='true'][1]")));
            if (child.exists() && child.is(Condition.visible) && isClickReady(child)) {
                child.click();
                log.debug("[AddLanguage] Клик по «{}» выполнен через кликабельного потомка.", humanName);
                return;
            }
            el.click();
            log.debug("[AddLanguage] Клик по «{}» выполнен (fallback).", humanName);
        } catch (Throwable t) {
            log.error("[AddLanguage] Ошибка клика по «{}»: {}", humanName, t.getMessage(), t);
            throw new AssertionError("Не удалось кликнуть по элементу: " + humanName, t);
        }
    }

    private void rememberRowTexts(SelenideAppiumElement row) {
        try {
            rememberedLanguageLocal = safeGetText(row, ROW_LOCAL_NAME_REL);
            rememberedLanguageEnglish = safeGetText(row, ROW_EN_NAME_REL);
            log.info("[AddLanguage] Запомнено: local='{}', english='{}'", rememberedLanguageLocal, rememberedLanguageEnglish);
        } catch (Throwable t) {
            log.warn("[AddLanguage] Не удалось прочитать/запомнить тексты строки: {}", t.getMessage());
        }
    }

    private void logSelection(String context) {
        log.info("[AddLanguage] Выбор завершён ({}): {}", context, getRememberedLanguagePretty());
    }

    /** Правило клика: displayed && enabled && clickable. */
    private boolean isClickReady(SelenideAppiumElement el) {
        try {
            return el.isDisplayed()
                    && "true".equalsIgnoreCase(String.valueOf(el.getAttribute("enabled")))
                    && "true".equalsIgnoreCase(String.valueOf(el.getAttribute("clickable")));
        } catch (Exception e) {
            log.warn("[AddLanguage] Не удалось прочитать атрибуты кликабельности: {}", e.getMessage());
            return false;
        }
    }

    /** Найти строку по одному видимому тексту под секцией «All languages». */
    private SelenideAppiumElement languageRowUnderAllLanguagesByText(String languageText) {
        String header = "//*[child::*[contains(normalize-space(@text),'All languages') or contains(normalize-space(@text),'Все языки')]]";
        String xp = header
                + "//android.view.View[@clickable='true']"
                + "[child::*[contains(normalize-space(@text), " + xq(languageText) + ")]]";
        return $(xpath(xp));
    }

    /** Найти строку по любому из вариантов текста под секцией «All languages». */
    private SelenideAppiumElement languageRowUnderAllLanguagesByAnyText(String... variants) {
        if (variants == null || variants.length == 0) {
            throw new IllegalArgumentException("Нужно передать хотя бы один вариант текста");
        }
        String header = "//*[child::*[contains(normalize-space(@text),'All languages') or contains(normalize-space(@text),'Все языки')]]";

        StringBuilder or = new StringBuilder();
        for (int i = 0; i < variants.length; i++) {
            if (i > 0) or.append(" or ");
            or.append("contains(normalize-space(@text), ").append(xq(variants[i])).append(")");
        }

        String xp = header
                + "//android.view.View[@clickable='true']"
                + "[child::*[" + or + "]]";

        return $(xpath(xp));
    }

    /** Безопасно вычитать текст дочернего узла внутри строки по относительному XPath. */
    private String safeGetText(SelenideAppiumElement row, String relativeXpath) {
        try {
            return row.$(xpath(relativeXpath)).getText();
        } catch (Throwable t) {
            log.debug("[AddLanguage] safeGetText: не удалось прочитать текст по {}: {}", relativeXpath, t.getMessage());
            return null;
        }
    }

    /**
     * Красивое имя выбранного языка: {@code "Español (Spanish)"} или только эндоним.
     *
     * @return строка для отчёта/логов или {@code null}, если ничего не запомнено
     */
    public String getRememberedLanguagePretty() {
        if (rememberedLanguageLocal == null || rememberedLanguageLocal.isBlank()) return null;
        return (rememberedLanguageEnglish != null && !rememberedLanguageEnglish.isBlank())
                ? rememberedLanguageLocal + " (" + rememberedLanguageEnglish + ")"
                : rememberedLanguageLocal;
    }
}
