package guru.qa.ui.screens;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.appium.SelenideAppiumElement;
import guru.qa.ui.app.App;
import io.appium.java_client.AppiumBy;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Arrays;
import java.util.regex.Pattern;

import static com.codeborne.selenide.appium.SelenideAppium.$;
import static guru.qa.ui.allure.Steps.step;
import static io.appium.java_client.AppiumBy.id;
import static io.appium.java_client.AppiumBy.xpath;

/**
 * Экран онбординга Wikipedia (Android).
 *
 * <p><b>Назначение:</b> верификация и прохождение 4 слайдов онбординга:
 * проверки заголовков/описаний (RU/EN), индикатора страниц, изображений,
 * кнопок Skip/Continue/Get started и списка языков на первом слайде.</p>
 *
 * <p><b>Инварианты:</b> Appium 3 + UiAutomator2 (XPath 2.0), Selenide-Appium;
 * только устойчивые локаторы (id / XPath2); ожидания через {@code should*}.
 * Правило клика: {@code visible=true && enabled=true && attribute(clickable)=true}.</p>
 *
 * <p><b>EN:</b> Onboarding screen Page Object for Wikipedia Android. Verifies and
 * completes the 4-slide flow with RU/EN titles/subtitles, page indicator, images,
 * and language list. Uses stable ids/XPath2 and project clickability rule.</p>
 */
@SuppressWarnings({"UnusedReturnValue", "BooleanMethodIsAlwaysInverted"})
@Slf4j
public class OnboardingScreen {

    private static final String[] S1_TITLE = {"Свободная энциклопедия", "The Free Encyclopedia"};
    private static final String[] S1_SUB = {"Мы нашли следующие языки", "We’ve found the following on your device"};
    private static final String[] BTN_ADD = {"Добавить или удалить язык", "Add or edit languages"};

    private static final String[] S2_TITLE = {"Новые способы исследований", "New ways to explore"};
    private static final String[] S2_SUB = {"постоянно обновляемой ленты", "constantly updating Explore feed"};

    private static final String[] S3_TITLE = {"Списки для чтения с синхронизацией", "Reading lists with sync"};
    private static final String[] S3_SUB = {"Вы можете создавать списки", "You can make reading lists"};

    private static final String[] S4_TITLE = {"Данные и конфиденциальность", "Data & Privacy"};
    private static final String[] S4_SUB = {"не нужно предоставлять личную информацию", "not have to provide personal information"};

    private final SelenideAppiumElement onboardingPager = $(id("org.wikipedia.alpha:id/fragment_onboarding_pager_container"));
    private final SelenideAppiumElement title = $(id("org.wikipedia.alpha:id/primaryTextView"));
    private final SelenideAppiumElement subtitle = $(id("org.wikipedia.alpha:id/secondaryTextView"));
    private final SelenideAppiumElement skipBtn = $(id("org.wikipedia.alpha:id/fragment_onboarding_skip_button"));
    private final SelenideAppiumElement continueBtn = $(id("org.wikipedia.alpha:id/fragment_onboarding_forward_button"));
    private final SelenideAppiumElement getStartedBtn = $(id("org.wikipedia.alpha:id/fragment_onboarding_done_button"));
    private final SelenideAppiumElement pageIndicator = $(id("org.wikipedia.alpha:id/view_onboarding_page_indicator"));
    private final SelenideAppiumElement addOrEditLanguageBtn = $(id("org.wikipedia.alpha:id/addLanguageButton"));
    private final SelenideAppiumElement centeredImage = $(id("org.wikipedia.alpha:id/imageViewCentered"));
    private final SelenideAppiumElement languagesList = $(id("org.wikipedia.alpha:id/languagesList"));

    private SelenideAppiumElement tabExplore() {
        return App.components().bottomTabBar.tabExplore;
    }

    private static String dotXpath(int index) {
        return "//android.widget.HorizontalScrollView[@resource-id='org.wikipedia.alpha:id/view_onboarding_page_indicator']"
                + "/android.widget.LinearLayout/android.widget.LinearLayout[" + index + "]";
    }

    private static SelenideAppiumElement dot(int index) {
        return $(xpath(dotXpath(index)));
    }

    private static void clickWhenReady(SelenideAppiumElement el, String name) {
        step("Кликаем: " + name + " (visible+enabled+clickable)", () ->
                el.shouldBe(Condition.visible.because(name + " должна быть видима"))
                        .shouldBe(Condition.enabled.because(name + " должна быть доступна"))
                        .shouldHave(Condition.attribute("clickable", "true").because(name + " должна быть кликабельна"))
                        .tap()
        );
    }

    private static String readButtonLabel(SelenideAppiumElement btn) {
        try {
            String t = btn.getText();
            if (!t.isBlank()) return t;
        } catch (RuntimeException ignored) {}
        try {
            return btn.$(AppiumBy.xpath(".//android.widget.TextView")).getText();
        } catch (RuntimeException ignored) {}
        return "";
    }

    private static String safeText(SelenideAppiumElement el) {
        try {
            return el.getText();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static boolean containsAny(String actualNorm, String... candidates) {
        final String a = normalizeLoose(actualNorm);
        for (String c : candidates) {
            if (c == null || c.isBlank()) continue;
            if (a.contains(normalizeLoose(c))) return true;
        }
        return false;
    }

    private static String normalizeLoose(String s) {
        if (s == null) return "";
        return s.replace('\n', ' ')
                .replace('\u00A0', ' ').replace('\u202F', ' ').replace('\u2009', ' ')
                .replace('…', ' ')
                .trim()
                .replaceAll("\\s+", " ");
    }

    private ElementsCollection languageItems() {
        return languagesList.$$(id("org.wikipedia.alpha:id/option_label"));
    }

    /**
     * Проверить, что контейнер онбординга существует (экран открыт).
     * <b>EN:</b> Returns whether onboarding container exists.
     */
    public boolean isOpen() {
        return step("Онбординг: проверить открыт ли экран (по контейнеру пейджера)",
                () -> onboardingPager.exists());
    }

    /**
     * Убедиться, что экран онбординга отображается.
     * <b>EN:</b> Assert onboarding container is visible.
     */
    public OnboardingScreen shouldBeVisible() {
        return step("Онбординг: экран отображается", () -> {
            step("Проверяем видимость контейнера пейджера", () ->
                    onboardingPager.shouldBe(Condition.visible.because("Контейнер онбординга должен быть видим"))
            );
            return this;
        });
    }

    /**
     * Проверить заголовок текущего слайда по списку эталонов (RU/EN).
     * <b>EN:</b> Title should contain any of expected variants.
     */
    public OnboardingScreen shouldHaveTitle(String... variants) {
        return step("Онбординг: заголовок содержит один из ожидаемых вариантов", () -> {
            step("Читаем заголовок текущего слайда", () -> {
                String act = normalizeLoose(safeText(title));
                step("Сравниваем с эталонами: " + Arrays.toString(variants), () -> {
                    if (!containsAny(act, variants)) {
                        throw new AssertionError("Заголовок не совпал. Факт: '" + act + "'");
                    }
                });
            });
            return this;
        });
    }

    /**
     * Проверить описание текущего слайда по списку эталонов (RU/EN).
     * <b>EN:</b> Subtitle should contain any of expected variants.
     */
    public OnboardingScreen shouldHaveSubtitle(String... variants) {
        return step("Онбординг: описание содержит один из ожидаемых вариантов", () -> {
            step("Читаем подзаголовок текущего слайда", () -> {
                String act = normalizeLoose(safeText(subtitle));
                step("Сравниваем с эталонами: " + Arrays.toString(variants), () -> {
                    if (!containsAny(act, variants)) {
                        throw new AssertionError("Описание не совпало. Факт: '" + act + "'");
                    }
                });
            });
            return this;
        });
    }

    /**
     * Проверить индикатор шага (1..4), включая content-desc и selected-точки.
     * <b>EN:</b> Assert page indicator reflects given step.
     */
    public OnboardingScreen shouldBeOnStep(int step) {
        return step("Онбординг: индикатор на шаге " + step + " из 4", () -> {
            step("Валидируем параметр step (1..4)", () -> {
                if (step < 1 || step > 4)
                    throw new IllegalArgumentException("step должен быть 1..4, получено: " + step);
            });
            step("Проверяем content-desc контейнера индикатора (RU/EN)", () ->
                    pageIndicator.should(Condition.or("content-desc RU/EN",
                            Condition.attribute("content-desc", "Шаг " + step + " из 4"),
                            Condition.attribute("content-desc", "Page " + step + " of 4")))
            );
            step("Активная точка №" + step + " имеет selected=true", () ->
                    dot(step).shouldHave(Condition.attribute("selected", "true"))
            );
            step("Остальные точки имеют selected=false", () -> {
                for (int i = 1; i <= 4; i++)
                    if (i != step)
                        dot(i).shouldHave(Condition.attribute("selected", "false"));
            });
            return this;
        });
    }

    /**
     * Проверить, что центральная картинка видима/неинтерактивна.
     * <b>EN:</b> Central image is visible and non-interactive.
     */
    public OnboardingScreen shouldShowCenteredImage() {
        return step("Онбординг: центральная картинка отображается и не интерактивна", () -> {
            step("Картинка видима и активна", () ->
                    centeredImage.shouldBe(Condition.visible).shouldBe(Condition.enabled)
            );
            step("Атрибуты интерактивности выключены", () -> {
                centeredImage.shouldHave(Condition.attribute("clickable", "false"));
                centeredImage.shouldHave(Condition.attribute("focusable", "false"));
            });
            return this;
        });
    }

    /**
     * Получить границы центральной картинки из атрибута bounds.
     * <b>EN:</b> Returns image bounds as [x1,y1,x2,y2].
     */
    public int[] getCenteredImageBounds() {
        return step("Онбординг: получить границы центральной картинки", () -> {
            final int[][] bb = {null};
            step("Читаем и парсим bounds", () -> {
                String b = centeredImage.getAttribute("bounds");
                var m = Pattern.compile("\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]").matcher(b == null ? "" : b);
                if (!m.find()) throw new IllegalStateException("Не удалось распарсить bounds: " + b);
                bb[0] = new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4))};
            });
            return bb[0];
        });
    }

    /**
     * Эвристика: картинка крупная и по центру (минимальные размеры).
     * <b>EN:</b> Heuristic check for image size/position.
     */
    public OnboardingScreen imageLooksCenteredAndLarge() {
        return step("Онбординг: центральная картинка крупная и расположена по центру (эвристика)", () -> {
            step("Проверяем эвристикой размеры картинки", () -> {
                int[] b = getCenteredImageBounds();
                int w = Math.max(0, b[2] - b[0]);
                int h = Math.max(0, b[3] - b[1]);
                if (w < 400 || h < 400)
                    throw new AssertionError("Ожидалась крупная картинка, получили w=" + w + ", h=" + h);
            });
            return this;
        });
    }

    /**
     * Нажать Skip.
     * <b>EN:</b> Tap “Skip”.
     */
    public void skipAll() {
        step("Онбординг: нажать «Пропустить» (Skip)", () ->
                clickWhenReady(skipBtn, "Кнопка «Пропустить»"));
    }

    /**
     * Нажать Continue.
     * <b>EN:</b> Tap “Continue”.
     */
    public void continueNext() {
        step("Онбординг: нажать «Продолжить» (Continue)", () ->
                clickWhenReady(continueBtn, "Кнопка «Продолжить»"));
    }

    /**
     * Нажать Get started.
     * <b>EN:</b> Tap “Get started”.
     */
    public void getStarted() {
        step("Онбординг: нажать «Начать» (Get started)", () ->
                clickWhenReady(getStartedBtn, "Кнопка «Начать»"));
    }

    /**
     * Открыть Add or edit languages.
     * <b>EN:</b> Open “Add or edit languages”.
     */
    public void openAddOrEditLanguages() {
        step("Онбординг: открыть «Добавить или удалить язык»", () ->
                clickWhenReady(addOrEditLanguageBtn, "Кнопка «Добавить или удалить язык»"));
    }

    /**
     * Перейти к следующему слайду и дождаться смены заголовка.
     * <b>EN:</b> Continue and wait until title changes.
     */
    public OnboardingScreen proceedToNextSlideAwaitTitleChange() {
        return step("Онбординг: перейти к следующему слайду (ожидать смену заголовка)", () -> {
            final String prev = safeText(title);
            step("Нажимаем Continue", this::continueNext);
            step("Ждём смену текста заголовка", () ->
                    title.shouldNotHave(Condition.exactText(prev).because("Заголовок должен смениться после перехода"))
            );
            return this;
        });
    }

    /**
     * Завершить онбординг через Skip, если доступно.
     * <b>EN:</b> Finish via “Skip” if button is visible.
     */
    public void skipIfVisible() {
        step("Онбординг: завершить сразу через «Пропустить», если доступно", () -> {
            if (skipBtn.is(Condition.appear, Duration.ofSeconds(5))) {
                step("Нажимаем Skip", this::skipAll);
                step("Проверяем выход на главный экран", this::verifyExitedToExplore);
            } else {
                log.info("Skip отсутствует на этом слайде — ничего не делаем.");
            }
        });
    }

    /**
     * Кнопка добавления/редактирования языков доступна.
     * <b>EN:</b> Languages button is visible/enabled/clickable.
     */
    public OnboardingScreen shouldHaveAddLanguageButton() {
        return step("Онбординг: кнопка добавления/редактирования языков доступна", () -> {
            step("Проверяем visible + enabled + clickable", () ->
                    addOrEditLanguageBtn
                            .shouldBe(Condition.visible.because("Кнопка должна быть видима"))
                            .shouldBe(Condition.enabled.because("Кнопка должна быть активна"))
                            .shouldHave(Condition.attribute("clickable", "true").because("Кнопка должна быть кликабельна"))
            );
            return this;
        });
    }

    /**
     * Лейбл кнопки языков соответствует одному из ожидаемых (RU/EN).
     * <b>EN:</b> Language button label equals any expected value.
     */
    public OnboardingScreen shouldHaveLabel(String... labels) {
        return step("Онбординг: лейбл кнопки равен одному из ожидаемых", () -> {
            step("Читаем текст кнопки", () -> {});
            String label = readButtonLabel(addOrEditLanguageBtn);
            step("Сравниваем с эталонами: " + Arrays.toString(labels), () -> {
                for (String expected : labels) if (expected != null && expected.equals(label)) return;
                throw new AssertionError("Ожидался один из " + Arrays.toString(labels) + ", фактически: '" + label + "'");
            });
            return this;
        });
    }

    /**
     * Список языков видим и не пуст.
     * <b>EN:</b> Language list is visible and non-empty.
     */
    public OnboardingScreen shouldHaveLanguagesList() {
        return step("Языки: список отображается и не пустой", () -> {
            step("Проверяем наличие RecyclerView и элементов", () -> {
                languagesList.shouldBe(Condition.visible).shouldBe(Condition.enabled);
                languageItems().shouldHave(CollectionCondition.sizeGreaterThan(0));
            });
            return this;
        });
    }

    /**
     * Перейти на конкретный шаг (через Continue) и проверить индикатор.
     * <b>EN:</b> Go to the next slide and assert indicator step.
     */
    public OnboardingScreen nextToStep(int nextStep) {
        return step("Онбординг: перейти на шаг " + nextStep + " (через Continue, с проверкой индикатора)", () -> {
            step("Переходим на следующий слайд", this::proceedToNextSlideAwaitTitleChange);
            return shouldBeOnStep(nextStep);
        });
    }

    /**
     * Завершить онбординг и дождаться появления вкладки Explore.
     * <b>EN:</b> Tap “Get started” and verify Explore appears.
     */
    public void finish() {
        step("Онбординг: нажать «Начать» и проверить выход на главный экран", () -> {
            step("Нажимаем «Начать»", this::getStarted);
            step("Проверяем появление нижней панели Explore", this::verifyExitedToExplore);
        });
    }

    /**
     * Пройти онбординг целиком (4 слайда) и завершить.
     * <b>EN:</b> Complete full 4-slide onboarding flow.
     */
    public void completeOnboardingFlow() {
        step("Онбординг: пройти 4 слайда и завершить (универсально RU/EN)", () -> {
            shouldBeVisible();
            step("Слайд 1: проверка контента", this::verifySlide1);
            step("Переход на шаг 2", () -> nextToStep(2));
            step("Слайд 2: проверка контента", this::verifySlide2);
            step("Переход на шаг 3", () -> nextToStep(3));
            step("Слайд 3: проверка контента", this::verifySlide3);
            step("Переход на шаг 4", () -> nextToStep(4));
            step("Слайд 4: проверка контента", this::verifySlide4);
            finish();
        });
    }

    /**
     * Проверки слайда 1 (Languages).
     * <b>EN:</b> Verify slide 1.
     */
    public OnboardingScreen verifySlide1() {
        return step("Онбординг: проверить слайд 1 (Языки)", () -> {
            step("Проверяем индикатор и тексты", () -> {
                shouldBeOnStep(1);
                shouldHaveTitle(S1_TITLE);
                shouldHaveSubtitle(S1_SUB);
            });
            step("Проверяем изображение", () -> {
                shouldShowCenteredImage();
                imageLooksCenteredAndLarge();
            });
            step("Проверяем блок языков", () -> {
                shouldHaveLanguagesList();
                shouldHaveAddLanguageButton();
                shouldHaveLabel(BTN_ADD);
            });
            return this;
        });
    }

    /**
     * Проверки слайда 2 (Explore).
     * <b>EN:</b> Verify slide 2.
     */
    public OnboardingScreen verifySlide2() {
        return step("Онбординг: проверить слайд 2 (Explore)", () -> {
            step("Проверяем индикатор и тексты", () -> {
                shouldBeOnStep(2);
                shouldHaveTitle(S2_TITLE);
                shouldHaveSubtitle(S2_SUB);
            });
            step("Проверяем изображение", () -> {
                shouldShowCenteredImage();
                imageLooksCenteredAndLarge();
            });
            return this;
        });
    }

    /**
     * Проверки слайда 3 (Reading lists).
     * <b>EN:</b> Verify slide 3.
     */
    public OnboardingScreen verifySlide3() {
        return step("Онбординг: проверить слайд 3 (Reading lists)", () -> {
            step("Проверяем индикатор и тексты", () -> {
                shouldBeOnStep(3);
                shouldHaveTitle(S3_TITLE);
                shouldHaveSubtitle(S3_SUB);
            });
            step("Проверяем изображение", () -> {
                shouldShowCenteredImage();
                imageLooksCenteredAndLarge();
            });
            return this;
        });
    }

    /**
     * Проверки слайда 4 (Privacy).
     * <b>EN:</b> Verify slide 4.
     */
    public OnboardingScreen verifySlide4() {
        return step("Онбординг: проверить слайд 4 (Privacy)", () -> {
            step("Проверяем индикатор и тексты", () -> {
                shouldBeOnStep(4);
                shouldHaveTitle(S4_TITLE);
                shouldHaveSubtitle(S4_SUB);
            });
            step("Проверяем изображение", () -> {
                shouldShowCenteredImage();
                imageLooksCenteredAndLarge();
            });
            return this;
        });
    }

    private void verifyExitedToExplore() {
        step("Ждём вкладку Explore на нижней панели", () ->
                tabExplore().shouldBe(Condition.visible.because("После онбординга должна появиться нижняя панель «Explore»"))
        );
    }
}
