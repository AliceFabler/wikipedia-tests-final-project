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

@Slf4j
@Epic("Wikipedia (Android)")
@Feature("Onboarding / Онбординг")
@Owner("Alice Fabler")
@Tags({@Tag("android"), @Tag("local"), @Tag("remote"), @Tag("wikipedia")})
@Severity(SeverityLevel.NORMAL)
public class OnboardingTests extends TestBase {

    @Test
    @DisplayName("Онбординг: 4 слайда последовательно завершаются кнопкой «Get started»")
    @Story("Контент и последовательность слайдов")
    @Description("Проверяем, что все 4 слайда онбординга показываются по порядку и завершаются появлением кнопки «Get started».")
    @Issue("HOMEWORK-1528")
    @AllureId("40940")
    void onboardingSlidesContentTest() {
        App.screens().onboarding.completeOnboardingFlow();
    }

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

        // Считаем актуальные языки ДО
        final List<String> before = step("Считать текущие языки (до добавления)",
                () -> App.screens().languages.getCurrentLanguageTitles()
        );

        // Переходим в Add language и выбираем первый язык
        step("Перейти в 'Add language' и выбрать первый язык из полного списка", () -> {
            App.screens().languages.tapAddLanguageCard();
            App.screens().addLanguage
                    .shouldBeOpen()
                    .selectFirstLanguageAndRemember();
        });

        // Проверяем, что язык появился
        final String pickedLocal = App.screens().addLanguage.getRememberedLanguageLocal();
        before.add(pickedLocal);

        step("Проверить, что выбранный язык появился в 'Your languages'", () -> {
            App.screens().languages
                    .checkYourLanguagesScreen()
                    .assertLanguagePresent(before);
        });
    }
}
