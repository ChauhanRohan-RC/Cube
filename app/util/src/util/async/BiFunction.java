package util.async;

@FunctionalInterface
public interface BiFunction<I1, I2, O> {
    O apply(I1 first, I2 second);
}
