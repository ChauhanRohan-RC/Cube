package util.async;


import org.jetbrains.annotations.NotNull;

/** A cancellable ThrowableRunnable */
@FunctionalInterface
public interface CRun {

    /** A method that can be cancelled
     * cancellation Status must be checked often, to stop the ThrowableRunnable as soon as possible
     * */
    void run(@NotNull CancellationProvider c);

    default void executeOn(@NotNull Async.CExecutor cExecutor) {
        cExecutor.execute(this);
    }

    default @NotNull Async.CExecutor executeOnNew() {
        final Async.CExecutor executor = new Async.CExecutor();
        executeOn(executor);
        return executor;
    }
}
