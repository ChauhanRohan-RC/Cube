package util.async;

import java.io.IOException;

public interface IOFunction<I, O> {
    O apply(I in) throws IOException;
}
