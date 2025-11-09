package guru.qa.ui.allure;

import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Единый DSL для вложенных шагов. Совместим с Allure 2/3.
 * Используем ТОЛЬКО этот класс (никаких @Step-аннотаций).
 */
public final class Steps {
    private static final Logger LOG = LoggerFactory.getLogger("Steps");

    private Steps() {
    }

    /**
     * Вложенный шаг без возвращаемого значения.
     */
    public static void step(String name, Runnable body) {
        long t0 = System.nanoTime();
        LOG.info("🟦 {}", name);
        Allure.step(name, body::run);
        long ms = Duration.ofNanos(System.nanoTime() - t0).toMillis();
        LOG.info("🟩 {} — {} мс", name, ms);
    }

    /**
     * Вложенный шаг с результатом.
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
