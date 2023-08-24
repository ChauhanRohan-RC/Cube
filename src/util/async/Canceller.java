package util.async;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;

/**
 * Definition of Object that can cancel process and provide Cancellation status
 * */
public interface Canceller extends CancellationProvider {

    void cancel(boolean interrupt);

    @NotNull
    static Canceller basic() {
        return new BaseC();
    }

    @NotNull
    static Canceller ofFuture(@NotNull Future<?> future) {
        return new FutureC(future);
    }


    /**
     * Simple Canceller that only keeps track of cancellation
     * */
    class BaseC implements Canceller {

        private volatile boolean cancelled;

        @Override
        public void cancel(boolean interrupt) {
            cancelled = true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }

    /**
     * Canceller backed by a {@link Future future} object
     * */
    class FutureC implements Canceller {

        @NotNull
        private final Future<?> mFuture;

        public FutureC(@NotNull Future<?> future) {
            mFuture = future;
        }

        @NotNull
        public Future<?> getFuture() {
            return mFuture;
        }

        @Override
        public void cancel(boolean interrupt) {
            mFuture.cancel(interrupt);
        }

        @Override
        public boolean isCancelled() {
            return mFuture.isCancelled();
        }

    }
}
