package guru.qa.ui.app;

import guru.qa.ui.screens.*;

/**
 * Синглтоны экранов. ⚠️ Внутри экранов должны быть только прокси-элементы
 * (SelenideAppiumElement/SelenideAppiumCollection), не кэшируйте реальные WebElement.
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
