package util.async;

import org.jetbrains.annotations.NotNull;

public interface ThrowableCFunction<Input, Output> {

    Output begin(Input input, @NotNull CancellationProvider c) throws Throwable;

    @NotNull
    default ThrowableCTask<Output> toTask(Input in) {
        return c -> begin(in, c);
    }

}
