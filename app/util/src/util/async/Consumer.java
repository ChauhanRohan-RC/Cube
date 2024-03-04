package util.async;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;


/**
 * Definition of Consumer that consumes (processes) data
 * */
@FunctionalInterface
public interface Consumer<T> {

    @SuppressWarnings("rawtypes")
    Consumer NOOP = in -> {};


    /**
     * Handles the cooked data
     *
     * @param data data cooked by process
     * */
    void consume(T data);

    /**
     * Notification that main processes cooking the data has been cancelled
     *
     * @param dataProcessedYet : partial data cooked yet, or {@code null}
     * */
    default void onCancelled(@Nullable T dataProcessedYet) { }

    default void onProcessed(@Nullable T data, boolean cancelled) {
        if (cancelled) {
            onCancelled(data);
        } else {
            consume(data);
        }
    }

    default void onProcessed(@Nullable T data, @Nullable CancellationProvider c) {
        onProcessed(data, c != null && c.isCancelled());
    }

    @NotNull
    default Consumer<T> filter(@Nullable Predicate<? super T> filter) {
        return filter(this, filter);
    }

    @NotNull
    default Consumer<T> andThen(@NotNull Consumer<? super T> other) {
        return andThen(this, other);
    }

    @NotNull
    default java.util.function.Consumer<T> tpLegacy() {
        return this::consume;
    }


    @NotNull
    static <T> Consumer<T> filter(@NotNull Consumer<T> main, @Nullable Predicate<? super T> filter) {
        return filter != null? (T item) -> {
            if (filter.test(item))
                main.consume(item);
        }: main;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    static <T> Consumer<T> noop() { return (Consumer<T>) NOOP; }

    @NotNull
    static <T> Consumer<T> andThen(@NotNull Consumer<? super T> first, @NotNull Consumer<? super T> second) {
        return new Consumer<T>() {
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
        };
    }
}