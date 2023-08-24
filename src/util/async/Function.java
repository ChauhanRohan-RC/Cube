package util.async;

/**
 *  Definition of a task with an input and Output (I/O)
 *  */
@FunctionalInterface
public interface Function<Input, Output> {
    Output apply(Input input);
}