package util.async;

@FunctionalInterface
public interface BiConsumer<T1, T2> {
    void consume(T1 param1, T2 param2);
}
