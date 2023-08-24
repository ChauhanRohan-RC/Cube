package util.async;

import org.jetbrains.annotations.NotNull;


/**
 * A cancellable task with a certain Output
 * */
@FunctionalInterface
public interface CTask<Output> {
    Output begin(@NotNull CancellationProvider c);

    default void executeOn(@NotNull Async.CExecutor cExecutor, @NotNull Consumer<? super Output> handler, int delayMs) {
        cExecutor.execute(this, handler, delayMs);
    }

    default void executeOn(@NotNull Async.CExecutor cExecutor, @NotNull Consumer<? super Output> handler) {
        executeOn(cExecutor, handler, 0);
    }

    default @NotNull Async.CExecutor executeOnNew(@NotNull Consumer<? super Output> handler, int delayMs) {
        final Async.CExecutor executor = new Async.CExecutor();
        executor.execute(this, handler, delayMs);
        return executor;
    }

    default @NotNull Async.CExecutor executeOnNew(@NotNull Consumer<? super Output> handler) {
        return executeOnNew(handler, 0);
    }
}
