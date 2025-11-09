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
 * Экран управления языками (универсально RU/EN).
 * 1) "Your languages" / "Ваши языки" — список текущих языков + пункт "Add language" / "Добавить язык".
 * 2) "Add a language" / "Добавить язык" — Compose-экран (без стабильных id), выбор по тексту языка.
 * <p>
 * Стабильные id:
 * - section_header_text — заголовок "Your languages" / "Ваши языки";
 * - wiki_language_title — названия языков и пункт "Add language" / "Добавить язык".
 * <p>
 * Инварианты:
 * - Только SelenideAppiumElement/Collection;
 * - Без UiSelector; клики — только если displayed && enabled && attribute(clickable)="true".
 */
@SuppressWarnings("UnusedReturnValue")
@Slf4j
public class LanguagesScreen {

    public static final String ADD_LANGUAGE_EN = "Add language";
    public static final String ADD_LANGUAGE_RU = "Добавить язык";

    // ---------- Your languages ----------
    private final SelenideAppiumElement headerYourLanguages =
            $(id("org.wikipedia.alpha:id/section_header_text"));

    private final SelenideAppiumCollection titlesYourLanguages =
            $$(xpath(
                    "//*[@resource-id='org.wikipedia.alpha:id/wiki_language_title' " +
                            "and not(normalize-space(@text)='Add language' or normalize-space(@text)='Добавить язык')]"
            ));

    private final SelenideAppiumElement addLanguageButton = $(xpath("//*[child::*[@resource-id='org.wikipedia.alpha:id/wiki_language_title' and " +
            "(contains(normalize-space(@text),  '" + ADD_LANGUAGE_EN + "') or contains(normalize-space(@text),  '" + ADD_LANGUAGE_RU + "'))]]" +
            "[@clickable='true']"));

    // ---------- Helpers ----------
    private static void clickWhenReady(SelenideAppiumElement el, String name) {
        el.shouldBe(Condition.visible.because(name + " должна быть видима"))
                .shouldBe(Condition.enabled.because(name + " должна быть доступна"))
                .shouldHave(Condition.attribute("clickable", "true")
                        .because(name + " должна быть кликабельна"))
                .tap();
    }

    // ---------- Универсальные шаги (RU/EN) ----------

    public LanguagesScreen checkYourLanguagesScreen() {
        return step("Языки: проверяем экран 'Ваши языки' / 'Your languages'", () -> {
            headerYourLanguages.shouldBe(Condition.visible)
                    .shouldHave(Condition.or("RU/EN header",
                            Condition.exactText("Ваши языки"),
                            Condition.exactText("Your languages")));
            return this;
        });
    }

    public LanguagesScreen tapAddLanguageCard() {
        return step("Языки: нажать пункт '" + ADD_LANGUAGE_RU + "' / '" + ADD_LANGUAGE_EN + "'", () -> {
            clickWhenReady(addLanguageButton, "Пункт «" + ADD_LANGUAGE_RU + "»");
            return this;
        });
    }

    // ---------- Add a language (Compose) ----------

    public LanguagesScreen assertLanguagePresent(List<String> names) {
        String stepName = "Языки: убедиться, что язык присутствует в списке — " + String.join(" / ", names);
        return step(stepName, () -> {
            titlesYourLanguages.shouldHave(CollectionCondition.texts(names));
            return this;
        });
    }

    public List<String> getCurrentLanguageTitles() {
        return step("Языки: получить текущие значения списка «Your languages»", () -> {
            headerYourLanguages.shouldBe(Condition.visible)
                    .shouldHave(Condition.or("RU/EN header",
                            Condition.exactText("Ваши языки"),
                            Condition.exactText("Your languages")));

            titlesYourLanguages.shouldHave(com.codeborne.selenide.CollectionCondition.sizeGreaterThan(0));

            // Возвращаем ИЗМЕНЯЕМЫЙ список и исключаем пункт «Add language»
            List<String> values = titlesYourLanguages.texts().stream()
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .filter(s -> !(s.equalsIgnoreCase("Add language") || s.equalsIgnoreCase("Добавить язык")))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

            step("Найдены языки: " + values, () -> {
            });
            log.info("[Languages] Текущие языки (без пункта добавления): {}", values);
            return values;
        });
    }
}
