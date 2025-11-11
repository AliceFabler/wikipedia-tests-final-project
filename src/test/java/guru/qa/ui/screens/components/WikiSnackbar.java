package guru.qa.ui.screens.components;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.appium.SelenideAppiumElement;
import io.appium.java_client.AppiumBy;

import static com.codeborne.selenide.appium.SelenideAppium.$;
import static guru.qa.ui.allure.Steps.step;

/**
 * Компонент Snackbar (Material) в Wikipedia.
 *
 * <p>Ожидает подтверждение сохранения по тексту с подстрокой
 * «сохранено» / «saved». В качестве резервного варианта — поиск по локализованному
 * тексту в XPath.</p>
 *
 * <p><b>EN:</b> Material Snackbar utility. Waits for “saved” confirmation text
 * (RU/EN), with a fallback XPath matcher.</p>
 */
public class WikiSnackbar {

    private final SelenideAppiumElement text =
            $(AppiumBy.id("com.google.android.material:id/snackbar_text"));

    private final SelenideAppiumElement textByXpath =
            $(AppiumBy.xpath("//*[contains(lower-case(@text),'сохранено') or contains(lower-case(@text),'saved')]"));

    /**
     * Дождаться появления подтверждения сохранения в Snackbar.
     *
     * <p><b>EN:</b> Wait until saved-confirmation appears in a Snackbar.</p>
     */
    public void waitSavedConfirmation() {
        step("Snackbar: дождаться подтверждения сохранения", () -> {
            if (text.exists()) {
                text.shouldBe(Condition.visible);
            } else {
                textByXpath.shouldBe(Condition.visible);
            }
        });
    }
}
