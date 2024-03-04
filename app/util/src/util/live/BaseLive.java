package util.live;

import org.jetbrains.annotations.NotNull;
import util.async.Async;

public class BaseLive<T, O extends BaseLive.Observer<? super T>> extends Listeners<O> {

    @FunctionalInterface
    public interface Observer<T> {
        void onChanged(@NotNull BaseLive<? extends T, ?> live, T old);

        default void onActiveStateChanged(@NotNull BaseLive<? extends T, ?> live, boolean nowActive) {
        }
    }

    private volatile T mData;

    public BaseLive(T data) {
        this.mData = data;
    }

    public BaseLive() {
    }

    @Override
    protected void onActive() {
        super.onActive();
    }

    @Override
    protected void onInactive() {
        super.onInactive();
    }


    @Override
    protected boolean shouldAddListener(@NotNull O listener) {
        return super.shouldAddListener(listener);
    }

    @Override
    protected void onListenerAdded(@NotNull O listener) {
        super.onListenerAdded(listener);
        onObserverActiveStateChanged(listener, true);
    }

    @Override
    protected void onListenerRemoved(@NotNull O listener) {
        super.onListenerRemoved(listener);
        onObserverActiveStateChanged(listener, false);
    }





    protected void onChanged(T oldValue) {
        dispatchOnMainThread(o -> o.onChanged(BaseLive.this, oldValue));
    }

    protected void onObserverActiveStateChanged(@NotNull O observer, boolean isActive) {
        Async.postIfNotOnMainThread(() -> observer.onActiveStateChanged(BaseLive.this, isActive));
    }

    public T get() {
        return mData;
    }

//    @MainThread
    protected void set(T data, boolean notify) {
        final T old = mData;
        mData = data;
        if (notify) {
            onChanged(old);
        }
    }

//    @MainThread
    protected void set(T data) {
        set(data, true);
    }

    protected void postSet(T data, boolean notify) {
        Async.uiPost(() -> set(data, notify));
    }

    protected void postSet(T data) {
        postSet(data, true);
    }

    protected void setShouldPost(T data, boolean post) {
        if (post) {
            postSet(data);
        } else {
            set(data);
        }
    }


//    @MainThread
    protected boolean setIfChange(T data, boolean notify) {
        if (mData != data) {
            set(data, notify);
            return true;
        } return false;
    }

//    @MainThread
    protected boolean setIfChange(T data) {
        return setIfChange(data, true);
    }


    protected void postSetIfChange(T data, boolean notify) {
        Async.uiPost(() -> setIfChange(data, notify));
    }

    protected void postSetIfChange(T data) {
        postSetIfChange(data, true);
    }

    protected void setIfChangeShouldPost(T data, boolean post) {
        if (post) {
            postSetIfChange(data);
        } else {
            setIfChange(data);
        }
    }
}
