package util.async;

import java.io.IOException;

public interface IOTask<T> {
    T begin() throws IOException;
}
