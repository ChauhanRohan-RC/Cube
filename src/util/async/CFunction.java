package util.async;

import org.jetbrains.annotations.NotNull;


/**
 *  A cancellable Input-Output ThrowableRunnable (I/O)
 * */
@FunctionalInterface
public interface CFunction<Input, Output> {
    Output apply(Input input, @NotNull CancellationProvider c);

    default void executeOn(@NotNull Async.CExecutor cExecutor, Input input, @NotNull Consumer<Output> handler, int delayMs) {
        cExecutor.execute(this, input, handler, delayMs);
    }

    default void executeOn(@NotNull Async.CExecutor cExecutor, Input input, @NotNull Consumer<Output> handler) {
        executeOn(cExecutor, input, handler, 0);
    }

    default @NotNull Async.CExecutor executeOnNew(Input input, @NotNull Consumer<Output> handler, int delayMs) {
        final Async.CExecutor executor = new Async.CExecutor();
        executeOn(executor, input, handler, delayMs);
        return executor;
    }

    default @NotNull Async.CExecutor executeOnNew(Input input, @NotNull Consumer<Output> handler) {
        return executeOnNew(input, handler, 0);
    }
}
