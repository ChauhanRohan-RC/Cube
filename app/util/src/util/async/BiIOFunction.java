package util.async;

import java.io.IOException;

public interface BiIOFunction<P1, P2, O> {
    O apply(P1 one, P2 two) throws IOException;
}
