package guru.qa.ui.screens;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.appium.SelenideAppiumCollection;
import com.codeborne.selenide.appium.SelenideAppiumElement;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.codeborne.selenide.appium.SelenideAppium.$;
import static com.codeborne.selenide.appium.SelenideAppium.$$;
import static guru.qa.ui.allure.Steps.step;
import static io.appium.java_client.AppiumBy.id;
import static io.appium.java_client.AppiumBy.xpath;

/**
 * Экран управления языками / <b>Languages screen</b> (RU/EN).
 *
 * <p><b>Что умеет:</b>
 * <ul>
 *   <li>Проверка экрана «Your languages / Ваши языки»;</li>
 *   <li>Переход к «Add language / Добавить язык»;</li>
 *   <li>Проверка наличия языка(ов) в списке;</li>
 *   <li>Получение текущего списка языков (без пункта добавления).</li>
 * </ul>
 *
 * <p><b>Инварианты:</b> только стабильные локаторы id/XPath2; элементы — SelenideAppiumElement/Collection;
 * клики выполняются при {@code displayed && enabled && attribute(clickable)="true"}.</p>
 *
 * <p><b>EN:</b> Manages “Your languages” list and opens “Add language”. Uses stable ids/XPath2 and
 * enforces project clickability rule.</p>
 */
@SuppressWarnings("UnusedReturnValue")
@Slf4j
public class LanguagesScreen {

    /** Текст «Add language» в EN. */
    public static final String ADD_LANGUAGE_EN = "Add language";
    /** Текст «Добавить язык» в RU. */
    public static final String ADD_LANGUAGE_RU = "Добавить язык";

    /** Заголовок секции «Your languages / Ваши языки». */
    private final SelenideAppiumElement headerYourLanguages =
            $(id("org.wikipedia.alpha:id/section_header_text"));

    /** Все элементы языков (кроме пункта «Add language / Добавить язык»). */
    private final SelenideAppiumCollection titlesYourLanguages =
            $$(xpath(
                    "//*[@resource-id='org.wikipedia.alpha:id/wiki_language_title' " +
                            "and not(normalize-space(@text)='Add language' or normalize-space(@text)='Добавить язык')]"
            ));

    /** Карточка «Add language / Добавить язык» (кликабельный контейнер). */
    private final SelenideAppiumElement addLanguageButton = $(xpath(
            "//*[child::*[@resource-id='org.wikipedia.alpha:id/wiki_language_title' and " +
                    "(contains(normalize-space(@text),  '" + ADD_LANGUAGE_EN + "') or contains(normalize-space(@text),  '" + ADD_LANGUAGE_RU + "'))]]" +
                    "[@clickable='true']"));

    /**
     * Единый кликер по правилу кликабельности проекта.
     *
     * @param el   элемент
     * @param name имя для сообщений ожиданий
     */
    private static void clickWhenReady(SelenideAppiumElement el, String name) {
        el.shouldBe(Condition.visible.because(name + " должна быть видима"))
                .shouldBe(Condition.enabled.because(name + " должна быть доступна"))
                .shouldHave(Condition.attribute("clickable", "true")
                        .because(name + " должна быть кликабельна"))
                .tap();
    }

    /**
     * Проверить экран «Your languages / Ваши языки».
     *
     * <p><b>EN:</b> Assert the “Your languages” screen is visible (RU/EN header).</p>
     * @return текущий экран
     */
    public LanguagesScreen checkYourLanguagesScreen() {
        return step("Языки: проверяем экран 'Ваши языки' / 'Your languages'", () -> {
            headerYourLanguages.shouldBe(Condition.visible)
                    .shouldHave(Condition.or("RU/EN header",
                            Condition.exactText("Ваши языки"),
                            Condition.exactText("Your languages")));
            return this;
        });
    }

    /**
     * Нажать «Add language / Добавить язык».
     *
     * <p><b>EN:</b> Tap “Add language”.</p>
     * @return текущий экран
     */
    public LanguagesScreen tapAddLanguageCard() {
        return step("Языки: нажать пункт '" + ADD_LANGUAGE_RU + "' / '" + ADD_LANGUAGE_EN + "'", () -> {
            clickWhenReady(addLanguageButton, "Пункт «" + ADD_LANGUAGE_RU + "»");
            return this;
        });
    }

    /**
     * Убедиться, что список содержит искомые языки (точные тексты).
     *
     * <p><b>EN:</b> Assert that given language titles are present.</p>
     * @param names ожидаемые названия
     * @return текущий экран
     */
    public LanguagesScreen assertLanguagePresent(List<String> names) {
        String stepName = "Языки: убедиться, что язык присутствует в списке — " + String.join(" / ", names);
        return step(stepName, () -> {
            titlesYourLanguages.shouldHave(CollectionCondition.texts(names));
            return this;
        });
    }

    /**
     * Получить текущие языки из секции «Your languages» без пункта «Add language».
     *
     * <p><b>EN:</b> Return current language titles excluding “Add language”.</p>
     * @return изменяемый список строк
     */
    public List<String> getCurrentLanguageTitles() {
        return step("Языки: получить текущие значения списка «Your languages»", () -> {
            headerYourLanguages.shouldBe(Condition.visible)
                    .shouldHave(Condition.or("RU/EN header",
                            Condition.exactText("Ваши языки"),
                            Condition.exactText("Your languages")));

            titlesYourLanguages.shouldHave(com.codeborne.selenide.CollectionCondition.sizeGreaterThan(0));

            List<String> values = titlesYourLanguages.texts().stream()
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .filter(s -> !(s.equalsIgnoreCase("Add language") || s.equalsIgnoreCase("Добавить язык")))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

            step("Найдены языки: " + values, () -> {});
            log.info("[Languages] Текущие языки (без пункта добавления): {}", values);
            return values;
        });
    }
}
