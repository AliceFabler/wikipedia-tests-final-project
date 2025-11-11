package guru.qa.ui.allure;

import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Минималистичный DSL для вложенных шагов Allure (совместим с Allure 2/3).
 *
 * <p><b>Особенности:</b>
 * <ul>
 *   <li>Единая точка создания шагов без использования {@code @Step}-аннотаций;</li>
 *   <li>Логирование начала и завершения шага через SLF4J с измерением длительности;</li>
 *   <li>Поддержка шагов с результатом и без результата.</li>
 * </ul>
 *
 * <p><b>Потокобезопасность:</b> класс не хранит состояния, статические методы являются потокобезопасными.</p>
 *
 * <p><b>Примеры:</b></p>
 * <pre>{@code
 * // Шаг без возвращаемого значения
 * Steps.step("Открыть экран настроек", () -> {
 *     settingsButton.click();
 * });
 *
 * // Шаг с результатом
 * String title = Steps.step("Получить заголовок статьи", () -> articleTitle.getText());
 * }</pre>
 *
 * @see Allure#step(String, Runnable)
 * @see Allure#step(String, Supplier)
 */
public final class Steps {
    private static final Logger LOG = LoggerFactory.getLogger("Steps");

    private Steps() {
    }

    /**
     * Выполнить вложенный шаг Allure без возвращаемого значения.
     *
     * @param name человеко-читаемое имя шага (отображается в отчёте и логах)
     * @param body исполняемое действие шага
     * @throws RuntimeException если {@code body} выбросит исключение — оно пробрасывается далее и будет отражено в Allure
     */
    public static void step(String name, Runnable body) {
        long t0 = System.nanoTime();
        LOG.info("🟦 {}", name);
        Allure.step(name, body::run);
        long ms = Duration.ofNanos(System.nanoTime() - t0).toMillis();
        LOG.info("🟩 {} — {} мс", name, ms);
    }

    /**
     * Выполнить вложенный шаг Allure с возвращаемым значением.
     *
     * @param name  человеко-читаемое имя шага (отображается в отчёте и логах)
     * @param body  поставщик результата шага
     * @param <T>   тип возвращаемого значения
     * @return результат, возвращённый {@code body}
     * @throws RuntimeException если {@code body} выбросит исключение — оно пробрасывается далее и будет отражено в Allure
     */
    public static <T> T step(String name, Supplier<T> body) {
        long t0 = System.nanoTime();
        LOG.info("🟦 {}", name);
        T result = Allure.step(name, body::get);
        long ms = Duration.ofNanos(System.nanoTime() - t0).toMillis();
        LOG.info("🟩 {} — {} мс", name, ms);
        return result;
    }
}
