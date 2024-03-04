package util.async;

@FunctionalInterface
public interface TriFunction<T1, T2, T3, O> {
    O apply(T1 param1, T2 param2, T3 param3);
}
