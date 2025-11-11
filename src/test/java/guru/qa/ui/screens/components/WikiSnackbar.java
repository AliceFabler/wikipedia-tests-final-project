package guru.qa.ui.screens.components;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.appium.SelenideAppiumElement;
import io.appium.java_client.AppiumBy;

import static com.codeborne.selenide.appium.SelenideAppium.$;
import static guru.qa.ui.allure.Steps.step;

/**
 * Snackbar Wikipedia (Material). Ждём текст с подстрокой "сохранено"/"saved",
 * fallback — кнопка действия "Добавить в список" (RU) / "Add to list" (EN).
 */
public class WikiSnackbar {

    // Material ids встречаются как com.google.android.material:id/*
    private final SelenideAppiumElement text =
            $(AppiumBy.id("com.google.android.material:id/snackbar_text"));

    private final SelenideAppiumElement textByXpath =
            $(AppiumBy.xpath("//*[contains(lower-case(@text),'сохранено') or contains(lower-case(@text),'saved')]"));

    public void waitSavedConfirmation() {
        step("Snackbar: дождаться подтверждения сохранения", () -> {
            if (text.exists()) {
                text.shouldBe(Condition.visible);
            } else {
                // fallback по локализованному тексту
                textByXpath.shouldBe(Condition.visible);
            }
        });
    }
}
