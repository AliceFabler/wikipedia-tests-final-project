package guru.qa.ui.app;

/**
 * Глобальный доступ к синглтонам экранов и компонентов.
 * Реализация — "singleton per thread" через InheritableThreadLocal:
 * • при последовательном запуске — единый набор объектов на весь ран,
 * • при параллели — безопасно изолируется по потоку.
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
     * Доступ к синглтонам экранов
     */
    public static Screens screens() {
        return get().screens;
    }

    /**
     * Доступ к синглтонам общих компонентов
     */
    public static Components components() {
        return get().components;
    }

    /**
     * Сброс контейнера (вызываем после закрытия драйвера).
     */
    public static void reset() {
        TL.remove();
    }
}
