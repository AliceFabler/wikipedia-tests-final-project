package guru.qa.ui.screens;

/*
 * Что это за экран (структура)
 * <p>
 * Top App Bar
 * <p>
 * Левая кнопка-стрелка «назад» (ImageButton, content-desc типа Navigate up — локализуется).
 * <p>
 * Заголовок: «Add a language» (локализуется).
 * <p>
 * Иконка «поиск» справа (обычно ImageButton/ImageView, content-desc Search, локализуется).
 * <p>
 * Секция: «All languages» (локализуется).
 * <p>
 * Прокручиваемый список языков: каждая строка содержит:
 * <p>
 * Локальное самоназвание языка (например, Español, 日本語, Русский, Deutsch и т.д. — не зависит от локали устройства, т.к. это эндоним).
 * <p>
 * Английское название под ним (например, Spanish, Japanese, Russian, German).
 * <p>
 * Экран — без устойчивых resource-id на строках списка (Compose/системный экран), поэтому используем XPath 2.0 и структурные селекторы + проверку «кликабельности».
 */

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
 * <h1>AddLanguageScreen — экран «Добавить язык» (Wikipedia Android)</h1>
 *
 * <p><b>Назначение:</b> Page Object для системного экрана выбора языков. Класс инкапсулирует:
 * проверку открытия экрана, открытие поиска, выбор языка (первого в списке, по тексту/по нескольким вариантам),
 * а также «запоминание» названий выбранного языка (эндоним и при наличии — английский экзоным).</p>
 *
 * <h2>Технологический стек и инварианты проекта</h2>
 * <ul>
 *   <li>Java 21, Appium Server 3.x + UIAutomator2 (XPath 2.0), Selenide-Appium.</li>
 *   <li><b>Локаторы:</b> приоритетно id/accessibilityId, при отсутствии — <b>XPath 2.0</b> с аккуратной нормализацией текста.</li>
 *   <li><b>Запреты:</b> никаких UiSelector/TouchAction, никаких Thread.sleep (только ожидания через {@link Condition}).</li>
 *   <li><b>Кликабельность:</b> перед кликом убеждаемся, что элемент
 *   <code>displayed==true</code>, <code>enabled==true</code>, <code>attribute(clickable)==true</code>.
 *   Если контейнер «не кликабелен», пытаемся кликнуть кликабельного потомка, иначе — безопасный fallback-клик.</li>
 *   <li><b>Отчётность:</b> чистые <i>вложенные шаги Allure 3</i> через {@code Allure.step(...)} (без аннотаций {@code @Step}).</li>
 * </ul>
 *
 * <h2>Стратегия локализации и устойчивость</h2>
 * <ul>
 *   <li>Кнопки Back/Search находятся по <i>content-desc</i> с поддержкой EN/RU (например, «Go back/Назад», «Search/Поиск»).</li>
 *   <li>Список языков ищется <b>под секцией «All languages/Все языки»</b>. Строки — кликабельные контейнеры с TextView внутри.</li>
 * </ul>
 *
 * <h2>Публичные методы</h2>
 * <ul>
 *   <li>{@link #shouldBeOpen()} — проверяет, что экран открыт (Back видим, список не пуст), логирует пример первой строки.</li>
 *   <li>{@link #selectFirstLanguageAndRemember()} — выбирает первую строку списка и запоминает её тексты.</li>
 *   <li>{@link #getRememberedLanguagePretty()} — отформатированное имя выбранного языка (например, «Español (Spanish)»).</li>
 * </ul>
 *
 * <h2>Синхронизация и ошибки</h2>
 * <ul>
 *   <li>Только «умные» ожидания: {@code shouldBe(Condition.visible/sizeGreaterThan)}.</li>
 *   <li>При сбоях бросается {@link AssertionError} с понятным сообщением; дополнительно пишем подробный лог (debug/info/warn/error).</li>
 * </ul>
 *
 * <h2>Пример использования</h2>
 *
 * <pre>{@code
 * new AddLanguageScreen()
 *     .shouldBeOpen()
 *     .openSearch() // опционально
 *     .selectLanguageByAnyVisibleText("Русский", "Russian", "русский");
 *
 * String picked = new AddLanguageScreen().getRememberedLanguagePretty();
 * // например: "Русский (Russian)"
 * }</pre>
 *
 * <h2>Подсказки по траблшутингу</h2>
 * <ul>
 *   <li>Если список «пустой»: проверьте, что секция «All languages/Все языки» присутствует в текущем билде
 *       (в системном/Compose-экране это может меняться), и что строки действительно помечены {@code clickable='true'}.</li>
 *   <li>Если Back/Search не находятся: убедитесь, что <i>content-desc</i> действительно содержит «Go back/Назад» и «Search/Поиск».</li>
 *   <li>Если клик не проходит: часто кликается <i>дочерний</i> элемент — класс содержит безопасный fallback на кликабельного потомка.</li>
 * </ul>
 *
 * <h2>Параллельные прогоны</h2>
 * <p>Класс хранит в полях только «последний запомненный выбор»; создавайте новый инстанс на сценарий,
 * не делитесь одним объектом между потоками.</p>
 */
@Slf4j
public class AddLanguageScreen {

    //region Locators

    /**
     * Тексты внутри строки: 1-й TextView — эндоним; последний — англ. экзоным (если есть)
     */
    static final String ROW_LOCAL_NAME_REL =
            ".//android.widget.TextView[normalize-space(@text)!=''][1]";
    static final String ROW_EN_NAME_REL =
            ".//android.widget.TextView[normalize-space(@text)!=''][position()=last()]";
    /**
     * ⬅️ Back (EN/RU) по образцу пользователя
     */
    SelenideAppiumElement backButton = $(
            xpath(
                    "//*[child::*[(contains(normalize-space(@content-desc),'Go back') or contains(normalize-space(@content-desc),'Назад'))]" +
                            " and child::*[contains(@class,'Button')]]"
            )
    );
    /**
     * 📃 Кликабельные строки под секцией “All languages/Все языки”
     */
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

    //region Public actions (отчётность — через Allure.step)

    /**
     * Безопасная подстановка строки в XPath.
     */
    private static String xq(String s) {
        if (s == null) return "''";
        if (!s.contains("'")) return "'" + s + "'";
        return "concat('" + s.replace("'", "',\"'\",'") + "')";
    }

    /**
     * Проверить, что экран открыт (вложенные шаги Allure).
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
    //endregion

    //region Internals (вложенность строится в public-методах)

    /**
     * Выбрать первый язык и запомнить его.
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
            // fallback: кликабельный потомок
            SelenideAppiumElement child = $(el.$(xpath(".//*[@clickable='true' or @focusable='true'][1]")));
            if (child.exists() && child.is(Condition.visible) && isClickReady(child)) {
                child.click();
                log.debug("[AddLanguage] Клик по «{}» выполнен через кликабельного потомка.", humanName);
                return;
            }
            // финальный fallback
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

    /**
     * displayed && enabled && clickable
     */
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

    /**
     * Поиск строки по одному тексту.
     */
    private SelenideAppiumElement languageRowUnderAllLanguagesByText(String languageText) {
        String header = "//*[child::*[contains(normalize-space(@text),'All languages') or contains(normalize-space(@text),'Все языки')]]";
        String xp = header
                + "//android.view.View[@clickable='true']"
                + "[child::*[contains(normalize-space(@text), " + xq(languageText) + ")]]";
        return $(xpath(xp));
    }

    /**
     * Поиск строки по любому из вариантов текста.
     */
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

    /**
     * Вытянуть текст дочернего узла внутри строки по относительному XPath.
     */
    private String safeGetText(SelenideAppiumElement row, String relativeXpath) {
        try {
            return row.$(xpath(relativeXpath)).getText();
        } catch (Throwable t) {
            log.debug("[AddLanguage] safeGetText: не удалось прочитать текст по {}: {}", relativeXpath, t.getMessage());
            return null;
        }
    }

    /**
     * Красивый вывод запомнённого языка.
     */
    public String getRememberedLanguagePretty() {
        if (rememberedLanguageLocal == null || rememberedLanguageLocal.isBlank()) return null;
        return (rememberedLanguageEnglish != null && !rememberedLanguageEnglish.isBlank())
                ? rememberedLanguageLocal + " (" + rememberedLanguageEnglish + ")"
                : rememberedLanguageLocal;
    }

    //endregion
}
