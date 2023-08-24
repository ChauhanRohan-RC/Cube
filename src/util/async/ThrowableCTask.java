package util.async;

import org.jetbrains.annotations.NotNull;

public interface ThrowableCTask<Output> {

    Output begin(@NotNull CancellationProvider c) throws Throwable;

}
