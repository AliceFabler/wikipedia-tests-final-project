package guru.qa.ui.tests;

import guru.qa.ui.app.App;
import io.qameta.allure.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;

import java.util.List;

import static guru.qa.ui.allure.Steps.step;

/**
 * Онбординг (Wikipedia Android) — UI-тесты.
 *
 * <p><b>Назначение:</b> проверить последовательность и контент онбординга,
 * а также управление языками с первого слайда.</p>
 *
 * <p><b>Инварианты:</b> шаги оформлены через вложенные Allure-step; действия выполняются
 * через Page Object’ы проекта; локаторы — стабильные (id/accessibilityId/XPath2 по месту).</p>
 *
 * <p><b>EN:</b> Onboarding UI tests: validate the 4-slide flow and add a language
 * from the first slide. Steps use nested Allure steps and stable locators via Page Objects.</p>
 */
@Slf4j
@Epic("Wikipedia (Android)")
@Feature("Onboarding / Онбординг")
@Owner("Alice Fabler")
@Tags({@Tag("android"), @Tag("local"), @Tag("remote"), @Tag("wikipedia")})
@Severity(SeverityLevel.NORMAL)
public class OnboardingTests extends TestBase {

    /**
     * Проверить, что 4 слайда онбординга последовательно завершаются
     * появлением и нажатием кнопки «Get started».
     *
     * <p><b>EN:</b> Verify the 4 onboarding slides in order ending with the “Get started” button.</p>
     */
    @Test
    @DisplayName("Онбординг: 4 слайда последовательно завершаются кнопкой «Get started»")
    @Story("Контент и последовательность слайдов")
    @Description("Проверяем, что все 4 слайда онбординга показываются по порядку и завершаются появлением кнопки «Get started».")
    @Issue("HOMEWORK-1528")
    @AllureId("40940")
    void onboardingSlidesContentTest() {
        App.screens().onboarding.completeOnboardingFlow();
    }

    /**
     * Добавить язык через первый экран онбординга и убедиться,
     * что язык появился в списке «Your languages».
     *
     * <p><b>EN:</b> Add a language from the first onboarding slide
     * and verify it appears in “Your languages”.</p>
     */
    @Test
    @DisplayName("Онбординг: добавить язык через первый экран (универсально по локализации)")
    @Story("Управление языками во время онбординга")
    @Description("Открыть выбор языков на первом слайде → перейти в «Add language» → выбрать первый язык → убедиться, что он появился в «Your languages».")
    @Issue("HOMEWORK-1529")
    @AllureId("40941")
    void addLanguageViaOnboardingTest() {
        step("Открыть выбор языков на первом слайде онбординга", () -> {
            App.screens().onboarding.shouldBeVisible();
            App.screens().onboarding.openAddOrEditLanguages();
        });

        final List<String> before = step("Считать текущие языки (до добавления)",
                () -> App.screens().languages.getCurrentLanguageTitles()
        );

        step("Перейти в 'Add language' и выбрать первый язык из полного списка", () -> {
            App.screens().languages.tapAddLanguageCard();
            App.screens().addLanguage
                    .shouldBeOpen()
                    .selectFirstLanguageAndRemember();
        });

        final String pickedLocal = App.screens().addLanguage.getRememberedLanguageLocal();
        before.add(pickedLocal);

        step("Проверить, что выбранный язык появился в 'Your languages'", () -> {
            App.screens().languages
                    .checkYourLanguagesScreen()
                    .assertLanguagePresent(before);
        });
    }
}
