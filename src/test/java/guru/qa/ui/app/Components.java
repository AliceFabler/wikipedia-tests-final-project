package guru.qa.ui.app;

import guru.qa.ui.screens.components.BottomTabBar;
import guru.qa.ui.screens.components.WikiOverlays;
import guru.qa.ui.screens.components.WikiSnackbar;

/**
 * Синглтоны общих UI-компонентов.
 */
public final class Components {
    public final BottomTabBar bottomTabBar = new BottomTabBar();
    public final WikiOverlays overlays = new WikiOverlays();
    public final WikiSnackbar snackbar = new WikiSnackbar();
}
