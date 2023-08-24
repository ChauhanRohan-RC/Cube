package util.misc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface CollectionUtil {

    static @NotNull <T> Iterator<T> iterator(@NotNull T[] values) {
        return new Iterator<T>() {
            int nextPointer = 0;

            @Override
            public boolean hasNext() {
                return nextPointer < values.length;
            }

            @Override
            public T next() {
                return values[nextPointer++];
            }
        };
    }



    static @NotNull <T> Iterator<T> chain(@NotNull Iterator<T> first, @NotNull Iterator<T> second) {
        return new Iterator<T>() {
            private @NotNull Iterator<T> mCurrent = first;
            @Override
            public boolean hasNext() { return mCurrent.hasNext() || (mCurrent = second).hasNext(); }
            @Override
            public T next() { return mCurrent.next(); }
        };
    }

    @SafeVarargs
    static @NotNull <T> Iterator<T> chain(@NotNull Iterator<T> first, @NotNull Iterator<T>... others) {
        Iterator<T> result = first;

        for (Iterator<T> i: others)
            result = chain(result, i);

        return result;
    }

    @SafeVarargs
    static <T> boolean has(@NotNull T o, @NotNull T... list) {
        for (T e: list)
            if (o.equals(e)) return true;
        return false;
    }

    static boolean hasNull(@NotNull Object[] list) {
        for (Object o: list)
            if (o == null) return true;
        return false;
    }


    static <T> void addAll(@NotNull Collection<? extends T> src, @NotNull Collection<? super T> dest, @Nullable Predicate<? super T> filter, @Nullable Consumer<? super T> addCall) {
        final Consumer<T> adder = addCall != null? (T t) -> {
            addCall.accept(t);
            dest.add(t);
        }: dest::add;

        if (filter == null) {
            for (T t: src)
                adder.accept(t);
        } else {
            for (T t: src) {
                if (filter.test(t))
                    adder.accept(t);
            }
        }

    }


    static <T> void addAll(@NotNull Collection<? extends T> src, @NotNull Collection<? super T> dest, @Nullable Predicate<? super T> filter) {
        addAll(src, dest, filter, null);
    }

    static <T> void addAll(@NotNull Collection<? extends T> src, @NotNull Collection<? super T> dest) {
        addAll(src, dest, null);
    }


    static @NotNull <T> Comparator<T> chain(@NotNull Comparator<? super T> first, @NotNull Comparator<? super T> second) {
        return (T one, T two) -> {
            final int result = first.compare(one, two);
            return (result != 0)? result: second.compare(one, two);
        };
    }

    @SafeVarargs
    static @NotNull <T> Comparator<T> chain(@NotNull Comparator<? super T> first, @NotNull Comparator<? super T>... others) {
        Comparator<T> result = first::compare;

        for (Comparator<? super T> c: others)
            result = chain(result, c);

        return result;
    }

    static @NotNull <T> Comparator<T> reversed(@NotNull Comparator<T> source) {
        return (T one, T two) -> source.compare(two, one);
    }

    static <T> boolean NotNullAnd(@Nullable T data, @NotNull Predicate<? super T> filter) {
        return data != null && filter.test(data);
    }

    static <T> T ifNull(@Nullable T source, T defaultValue) {
        return source != null? source: defaultValue;
    }


    static int size(@Nullable Collection<?> collection) {
        return collection != null? collection.size(): 0;
    }

    static boolean isEmpty(@Nullable Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    static boolean notEmpty(@Nullable Collection<?> collection) {
        return !isEmpty(collection);
    }

    static int size(@Nullable Map<?, ?> map) {
        return map != null? map.size(): 0;
    }

    static boolean isEmpty(@Nullable Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    static boolean notEmpty(@Nullable Map<?, ?> map) {
        return !isEmpty(map);
    }



    @Nullable
    static <T> List<T> wrapSingletonList(@Nullable T item) {
        return item != null? Collections.singletonList(item): null;
    }


    @NotNull
    static <T> ArrayList<T> arrayListCopy(@NotNull Collection<T> source, int sizeInc, @Nullable Predicate<? super T> filter) {
        final ArrayList<T> result = new ArrayList<>(source.size() + sizeInc);
        addAll(source, result, filter);
        return result;
    }

    @NotNull
    static <T> ArrayList<T> arrayListCopy(@NotNull Collection<T> source, int sizeInc) {
        return arrayListCopy(source, sizeInc, null);
    }

    @NotNull
    static <T> ArrayList<T> arrayListCopy(@NotNull Collection<T> source) {
        return arrayListCopy(source, 0);
    }

    @NotNull
    static <T> LinkedList<T> linkedListCopy(@NotNull Collection<T> source, @Nullable Predicate<? super T> filter) {
        final LinkedList<T> result = new LinkedList<>();
        addAll(source, result, filter);
        return result;
    }

    @NotNull
    static <T> LinkedList<T> linkedListCopy(@NotNull Collection<T> source) {
        return linkedListCopy(source, null);
    }


    static double @NotNull[] toDoubleArray(@NotNull Collection<? extends Number> c) {
        return c.stream().mapToDouble(Number::doubleValue).toArray();
    }

    static boolean contentEquals(@Nullable Collection<?> one, @Nullable Collection<?> two) {
        if (one == two)
            return true;

        if (isEmpty(one)) {
            return isEmpty(two);
        }

        if (isEmpty(two)) {
            return false;
        }

        if (one.size() != two.size()) {
            return false;
        }

        final Iterator<?> e1 = one.iterator(), e2 = two.iterator();

        while (e1.hasNext() && e2.hasNext()) {
            if (!(Objects.equals(e1.next(), e2.next())))
                return false;
        }

        return !(e1.hasNext() || e2.hasNext());
    }



}
