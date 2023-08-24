package util.live;

public class Live<T> extends BaseLive<T, BaseLive.Observer<T>> {

    public Live(T data) {
        super(data);
    }

    public Live() {
        super();
    }
}
