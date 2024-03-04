package util.async;

import org.jetbrains.annotations.NotNull;

public interface CVoidFunction<Input> {

    void apply(Input input, @NotNull CancellationProvider c);

    default void executeOn(@NotNull Async.CExecutor executor, Input input) {
        executor.execute(this, input);
    }

    default @NotNull Async.CExecutor executeOnNew(Input input) {
        final @NotNull Async.CExecutor executor = new Async.CExecutor();
        executeOn(executor, input);
        return executor;
    }
}
