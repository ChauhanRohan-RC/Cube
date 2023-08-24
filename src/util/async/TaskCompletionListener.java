package util.async;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface TaskCompletionListener<T> extends TaskConsumer<T> {

    @Override
    default void consume(T data) {
        onTaskComplete(data, false, false, null);
    }

    @Override
    default void onCancelled(@Nullable T dataProcessedYet) {
        onTaskComplete(dataProcessedYet, false, true, null);
    }

    @Override
    default void onFailed(@Nullable Throwable t) {
        onTaskComplete(null, true, false, t);
    }

    void onTaskComplete(T data, boolean failed, boolean cancelled, @Nullable Throwable error);
}
