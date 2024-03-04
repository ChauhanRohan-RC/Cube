package util.async;

public interface ThrowableFunction<Input, Output> {

    Output begin(Input input) throws Throwable;

}
