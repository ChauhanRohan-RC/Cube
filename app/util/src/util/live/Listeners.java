package util.live;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import util.misc.CollectionUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Listeners<T> implements ListenersI<T> {

    @NonNls
    private final List<T> mListeners = Collections.synchronizedList(new LinkedList<>());

    protected void onActive() {
    }

    protected void onInactive() {
    }

    protected boolean shouldAddListener(@NotNull T listener) {
        return true;
    }


    protected void onListenerAdded(@NotNull T listener) {
    }


    protected void onListenerRemoved(@NotNull T listener) {
    }


    @Override
    public final int listenersCount() {
        return mListeners.size();
    }

    @Override
    public final boolean addListener(@NotNull T listener) {
        if (!shouldAddListener(listener))
            return false;

        mListeners.add(listener);
        if (mListeners.size() == 1) {
            onActive();
        }

        onListenerAdded(listener);
        return true;
    }

    @Override
    public final boolean removeListener(@NotNull T listener) {
        final boolean removed = mListeners.remove(listener);

        if (removed) {
            onListenerRemoved(listener);
            if (mListeners.isEmpty()) {
                onInactive();
            }
        }

        return removed;
    }

    @Override
    public final boolean containsListener(@NotNull T listener) {
        return mListeners.contains(listener);
    }

    @NotNull
    @Override
    public final Collection<T> iterationCopy() {
        final List<T> ls = mListeners;
        if (CollectionUtil.isEmpty(ls))
            return Collections.emptyList();

        return CollectionUtil.linkedListCopy(ls);
    }
}
