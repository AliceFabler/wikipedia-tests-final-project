package guru.qa.ui.app;

import guru.qa.ui.screens.components.BottomTabBar;
import guru.qa.ui.screens.components.WikiOverlays;
import guru.qa.ui.screens.components.WikiSnackbar;

/**
 * Контейнер синглтонов общих UI-компонентов.
 *
 * <p>Экземпляры предназначены для повторного использования в рамках потока
 * через {@code App.components()}.</p>
 */
public final class Components {
    public final BottomTabBar bottomTabBar = new BottomTabBar();
    public final WikiOverlays overlays = new WikiOverlays();
    public final WikiSnackbar snackbar = new WikiSnackbar();
}
