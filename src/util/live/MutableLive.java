package util.live;

public class MutableLive<T> extends Live<T> {

    public MutableLive(T data) {
        super(data);
    }

    public MutableLive() {
        super();
    }

    @Override
    public void set(T data, boolean notify) {
        super.set(data, notify);
    }

    @Override
    public void set(T data) {
        super.set(data);
    }

    @Override
    public void postSet(T data, boolean notify) {
        super.postSet(data, notify);
    }

    @Override
    public void postSet(T data) {
        super.postSet(data);
    }

    @Override
    public void setShouldPost(T data, boolean post) {
        super.setShouldPost(data, post);
    }
    

    @Override
    public boolean setIfChange(T data, boolean notify) {
        return super.setIfChange(data, notify);
    }

    @Override
    public boolean setIfChange(T data) {
        return super.setIfChange(data);
    }

    @Override
    public void postSetIfChange(T data, boolean notify) {
        super.postSetIfChange(data, notify);
    }

    @Override
    public void postSetIfChange(T data) {
        super.postSetIfChange(data);
    }

    @Override
    public void setIfChangeShouldPost(T data, boolean post) {
        super.setIfChangeShouldPost(data, post);
    }
}
