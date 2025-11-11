package guru.qa.ui.app;

/**
 * Потоковый контейнер для глобального доступа к синглтонам экранов и компонентов.
 *
 * <p>Модель — «singleton per thread» на основе {@link InheritableThreadLocal}:
 * один экземпляр {@code App} на поток. При последовательном запуске —
 * единый набор объектов на весь ран; при параллельном — изоляция по потокам.</p>
 *
 * <p><b>Использование:</b>
 * <pre>{@code
 * App.screens().explore.open();
 * App.components().bottomTabBar.openSaved();
 * App.reset(); // вызывать после закрытия драйвера
 * }</pre>
 */
public final class App {
    private static final InheritableThreadLocal<App> TL = new InheritableThreadLocal<>();

    private final Screens screens = new Screens();
    private final Components components = new Components();

    private App() {
    }

    private static App get() {
        App inst = TL.get();
        if (inst == null) {
            inst = new App();
            TL.set(inst);
        }
        return inst;
    }

    /**
     * Возвращает контейнер синглтонов экранов текущего потока.
     *
     * @return экземпляр {@link Screens}
     */
    public static Screens screens() {
        return get().screens;
    }

    /**
     * Возвращает контейнер синглтонов общих компонентов текущего потока.
     *
     * @return экземпляр {@link Components}
     */
    public static Components components() {
        return get().components;
    }

    /**
     * Сбрасывает контейнер текущего потока.
     * Рекомендуется вызывать после закрытия WebDriver.
     */
    public static void reset() {
        TL.remove();
    }
}
