package util.async;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface TaskConsumer<T> extends Consumer<T> {

    void onFailed(@Nullable Throwable t);

    default TaskConsumer<T> andThen(@NotNull TaskConsumer<? super T> another) {
        return andThen(this, another);
    }

    @NotNull
    static <T> TaskConsumer<T> andThen(@NotNull TaskConsumer<? super T> first, @NotNull TaskConsumer<? super T> second) {
        return new TaskConsumer<T>() {
            @Override
            public void consume(T data) {
                first.consume(data);
                second.consume(data);
            }

            @Override
            public void onCancelled(@Nullable T dataProcessedYet) {
                first.onCancelled(dataProcessedYet);
                second.onCancelled(dataProcessedYet);
            }

            @Override
            public void onFailed(@Nullable Throwable t) {
                first.onFailed(t);
                second.onFailed(t);
            }
        };
    }
}
