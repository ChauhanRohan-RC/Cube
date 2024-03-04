package util.async;

import java.util.concurrent.CancellationException;

/**
 * Cancellation Signal Provider
 * */
public interface CancellationProvider {

    CancellationProvider UN_CANCELLABLE = () -> false;

    CancellationException EXCEPTION = new CancellationException("CancellationProvider Generic Exception");

    boolean isCancelled();

    default void throwIfCancelled(String msg) {
        if (isCancelled())
            throw new CancellationException(msg);
    }

    default void throwIfCancelled() {
        if (isCancelled())
            throw EXCEPTION;
    }

}
