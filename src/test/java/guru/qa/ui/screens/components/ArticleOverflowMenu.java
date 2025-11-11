package guru.qa.ui.screens.components;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.appium.SelenideAppiumElement;
import io.appium.java_client.AppiumBy;
import lombok.extern.slf4j.Slf4j;

import static com.codeborne.selenide.appium.SelenideAppium.$;
import static guru.qa.ui.allure.Steps.step;

/**
 * Меню «⋮» на экране статьи (Article Overflow).
 * <p>Выбор пунктов выполняется по стабильным ресурсным идентификаторам приложения.</p>
 *
 * <p><b>EN:</b> Article screen overflow (⋮) menu component.
 * Items are addressed via stable resource-ids.</p>
 */
@SuppressWarnings("UnusedReturnValue")
@Slf4j
public class ArticleOverflowMenu {

    /** Корневой контейнер drop-down — минимальная проверка открытия. */
    private final SelenideAppiumElement root =
            $(AppiumBy.id("org.wikipedia.alpha:id/overflowList"));

    /** Пункт меню «Лента / Explore». */
    private final SelenideAppiumElement exploreItem =
            $(AppiumBy.id("org.wikipedia.alpha:id/page_explore"));

    /**
     * Выбрать пункт «Лента» в меню.
     *
     * <p><b>EN:</b> Select “Explore” in the overflow menu.</p>
     * @return текущий объект меню
     */
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

    /**
     * Проверить, что оверфлоу открыт.
     *
     * <p><b>EN:</b> Assert that overflow menu is open.</p>
     * @return текущий объект меню
     */
    public ArticleOverflowMenu shouldBeOpen() {
        return step("Оверфлоу открыт", () -> {
            root.shouldBe(Condition.visible.because("Должно открыться выпадающее меню"));
            return this;
        });
    }
}
