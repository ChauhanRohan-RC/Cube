package util.misc;

public interface Flaggable {

    int getFlags();

    default boolean hasAllFlags(int flags) {
        return hasAllFlags(getFlags(), flags);
    }

    default boolean hasAnyFlag(int flags) {
        return hasAnyFlag(getFlags(), flags);
    }

    default boolean areFlagsDifferent(int flags) {
        return areFlagsDifferent(getFlags(), flags);
    }

    /**
     * sets new Flags, implementations should use {@link #areFlagsDifferent(int)} to check if flags have been areDifferent
     *
     * @param flags : new flags to setMain
     * @param refresh : should refresh if flags areDifferent
     *
     * @return whether flags are areDifferent after this call
     *
     * @see #areFlagsDifferent(int)
     * */
    boolean setFlags(int flags, boolean refresh);

    /**
     * @return whether to refresh automatically when flags are updated
     * */
    default boolean autoRefreshOnFlagsChanged() { return true; }

    default boolean setFlags(int flags) {
        return setFlags(flags, autoRefreshOnFlagsChanged());
    }

    default boolean addFlags(int flags, boolean refresh) {
        return setFlags(addFlags(getFlags(), flags), refresh);
    }

    default boolean addFlags(int flags) {
        return addFlags(flags, autoRefreshOnFlagsChanged());
    }

    default boolean removeFlags(int flags, boolean refresh) {
        return setFlags(removeFlags(getFlags(), flags), refresh);
    }

    default boolean removeFlags(int flags) {
        return removeFlags(flags, autoRefreshOnFlagsChanged());
    }

    default boolean setFlagsEnabled(int flags, boolean enabled, boolean refresh) {
        return enabled? addFlags(flags, refresh): removeFlags(flags, refresh);
    }

    default boolean setFlagsEnabled(int flags, boolean enabled) {
        return setFlagsEnabled(flags, enabled, autoRefreshOnFlagsChanged());
    }

    default boolean toggleFlag(int flag, boolean refresh) {
        return hasAllFlags(flag)? removeFlags(flag, refresh): addFlags(flag, refresh);
    }

    default boolean toggleFlag(int flag) {
        return toggleFlag(flag, autoRefreshOnFlagsChanged());
    }



    /* ...................  Static Utilities   .................... */

    static boolean hasAnyFlag(int main, int child) {
        return (main & child) != 0;
    }

    static boolean hasAllFlags(int main, int child) {
        return (main & child) == child;
    }

    static boolean areFlagsDifferent(int one, int two) {
        return (one ^ two) != 0;
    }

    static int addFlags(int main, int flags) {
        return main | flags;
    }

    static int removeFlags(int main, int flags) {
        return main &~flags;
    }

    /**
     * @return true only if flags contains onFlags and do not contain offFlags
     * */
    static boolean checkOnOff(int flags, int onFlags, int offFlags) {
        return (flags & (onFlags | offFlags)) == onFlags;
    }



    class Base implements Flaggable {

        private int mFlags;

        public Base(int initial) {
            mFlags = initial;
        }

        public Base() {
            this(0);
        }

        @Override
        public int getFlags() {
            return mFlags;
        }

        @Override
        public boolean setFlags(int flags, boolean refresh) {
            if (areFlagsDifferent(flags)) {
                final int old = mFlags;
                mFlags = flags;
                onFlagsChanged(old, flags, refresh);
                return true;
            }

            return false;
        }

        protected void onFlagsChanged(int oldFlags, int newFlags, boolean refresh) {
        }
    }


    class VolatileFlags implements Flaggable {

        private volatile int mFlags;

        public VolatileFlags(int initial) {
            mFlags = initial;
        }

        public VolatileFlags() {
            this(0);
        }

        @Override
        public int getFlags() {
            return mFlags;
        }

        @Override
        public boolean setFlags(final int flags, boolean refresh) {
            final int old = mFlags;
            if (Flaggable.areFlagsDifferent(old, flags)) {
                mFlags = flags;
                onFlagsChanged(old, flags, refresh);
                return true;
            }

            return false;
        }

        protected void onFlagsChanged(int oldFlags, int newFlags, boolean refresh) {
        }
    }

}
