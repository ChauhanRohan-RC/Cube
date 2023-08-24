package util.async;

import java.io.IOException;

public interface IOConsumer<T> {

    void consume(T data) throws IOException;
}
