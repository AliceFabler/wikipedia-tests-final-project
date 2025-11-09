package guru.qa.ui.screens.components;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.appium.SelenideAppiumElement;
import io.appium.java_client.AppiumBy;
import lombok.extern.slf4j.Slf4j;

import static com.codeborne.selenide.appium.SelenideAppium.$;
import static guru.qa.ui.allure.Steps.step;

/*
 * 🎯 MASTER PROMPT (final)
 * ArticleOverflowMenu:
 * — Меню по ⋮ на экране статьи; пункты выбираем по видимому тексту (XPath2, RU/EN).
 * — Методы-ярлыки для популярных пунктов.
 */

@SuppressWarnings("UnusedReturnValue")
@Slf4j
public class ArticleOverflowMenu {

    /**
     * Корневой контейнер дропа — достаточная проверка открытия.
     */
    private final SelenideAppiumElement root =
            $(AppiumBy.id("org.wikipedia.alpha:id/overflowList"));

    // Пункты меню из дампа (id внутри списка overflowList) :contentReference[oaicite:5]{index=5}
    private final SelenideAppiumElement exploreItem =
            $(AppiumBy.id("org.wikipedia.alpha:id/page_explore"));

    public ArticleOverflowMenu goToExplore() {
        return step("В меню выбрать «Лента»", () -> {
            exploreItem
                    .shouldBe(Condition.visible.because("Элемент меню «Лента» должен быть виден"))
                    .shouldBe(Condition.enabled.because("Элемент меню «Лента» должен быть доступен"))
                    .shouldHave(Condition.attribute("clickable", "true")
                            .because("Элемент меню «Лента» должен быть кликабелен"))
                    .tap();
            return this;
        });
    }

    public ArticleOverflowMenu shouldBeOpen() {
        return step("Оверфлоу открыт", () -> {
            root.shouldBe(Condition.visible.because("Должно открыться выпадающее меню"));
            return this;
        });
    }
}
