package guru.qa.ui.app;

import guru.qa.ui.screens.*;

/**
 * Контейнер синглтонов экранов приложения.
 *
 * <p>Требование к реализациям экранов: хранить только прокси-элементы
 * (например, {@code SelenideAppiumElement}/{@code SelenideAppiumCollection});
 * не кэшировать реальные {@code WebElement}.</p>
 */
public final class Screens {
    public final OnboardingScreen onboarding = new OnboardingScreen();
    public final ExploreScreen explore = new ExploreScreen();
    public final SearchResultScreen search = new SearchResultScreen();
    public final ArticleScreen article = new ArticleScreen();
    public final SavedScreen saved = new SavedScreen();
    public final LanguagesScreen languages = new LanguagesScreen();
    public final AddLanguageScreen addLanguage = new AddLanguageScreen();
}
