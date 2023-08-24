package util.live;

import util.misc.CollectionUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.*;


public class WeakListeners<T> implements ListenersI<T> {

    @NonNls
    private final List<WeakReference<T>> mListeners = Collections.synchronizedList(new LinkedList<>());

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

        mListeners.add(new WeakReference<>(listener));
        if (mListeners.size() == 1) {
            onActive();
        }

        onListenerAdded(listener);
        return true;
    }

    public final boolean prune() {
        if (mListeners.isEmpty())
            return false;

        final boolean changed = mListeners.removeIf(ref -> ref.get() == null);
        if (changed && mListeners.isEmpty()) {
            onInactive();
        }

        return changed;
    }

    @Override
    public final boolean removeListener(@NotNull T listener) {
        final boolean removed = mListeners.removeIf(ref -> {
            final T l = ref.get();
            return l != null && l.equals(listener);
        });

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
        for (WeakReference<T> ref: mListeners) {
            final T l = ref.get();
            if (l != null && l.equals(listener))
                return true;
        }

        return false;
    }


    @NotNull
    @Override
    public final Collection<T> iterationCopy() {
        if (CollectionUtil.isEmpty(mListeners))
            return Collections.emptyList();

        final List<T> list = new LinkedList<>();
        final Iterator<WeakReference<T>> itr = mListeners.iterator();

        while (itr.hasNext()) {
            final WeakReference<T> ref = itr.next();
            final T l = ref.get();
            if (l != null) {
                list.add(l);
            } else {
                itr.remove();
            }
        }

        return list;
    }
}

