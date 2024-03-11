package main;

import gl.animation.interpolator.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.StringJoiner;

public enum InterpolatorInfo {

    DEFAULT("default",
            null,
            "Default"
    ),

    LINEAR("linear",
            LinearInterpolator.getSingleton(),
            "Linear"
    ),

    BOUNCE("bounce",
            BounceInterpolator.getSingleton(),
            "Bounce"
    ),

    ACCELERATE("acc",
            new AccelerateInterpolator(),
            "Accelerate",
            "Acc"
    ),

    DECELERATE("dec",
            new DecelerateInterpolator(),
            "Decelerate",
            "Dec"
    ),

    ACCELERATE_DECELERATE("acd",
            AccelerateDecelerateInterpolator.getSingleton(),
            "Accelerate Decelerate",
            "Acc-Dec"
    ),

    ANTICIPATE("anticipate",
            new AnticipateInterpolator(),
            "Anticipate"
    ),

    OVERSHOOT("overshoot",
            new AnticipateOvershootInterpolator(),
            "Overshoot"
    );

    @NotNull
    public final String key;
    @Nullable
    public final Interpolator interpolator;

    @NotNull
    public final String displayName;
    @Nullable
    public final String shortDisplayName;

    InterpolatorInfo(@NotNull String key, @Nullable Interpolator interpolator, @NotNull String displayName, @Nullable String shortDisplayName) {
        this.key = key;
        this.interpolator = interpolator;
        this.displayName = displayName;
        this.shortDisplayName = shortDisplayName;
    }

    InterpolatorInfo(@NotNull String key, @Nullable Interpolator interpolator, @NotNull String displayName) {
        this(key, interpolator, displayName, null);
    }

    @NotNull
    public String getDisplayNamePreferShort() {
        return shortDisplayName != null? shortDisplayName: displayName;
    }

    public static String getDisplayInfo(boolean shortNames) {
        final StringJoiner sj = new StringJoiner("\n");

        for (InterpolatorInfo info : values()) {
            sj.add("\t" + info.key + " -> " + (shortNames? info.getDisplayNamePreferShort(): info.displayName));
        }

        return sj.toString();
    }

    @Nullable
    public static InterpolatorInfo fromKey(@Nullable String key) {
        if (key == null || key.isEmpty())
            return null;

        for (InterpolatorInfo info : values()) {
            if (info.key.equals(key))
                return info;
        }

        return null;
    }

    public static InterpolatorInfo fromInterpolator(@Nullable Interpolator interpolator, @Nullable InterpolatorInfo defaultValue) {
        if (interpolator == null)
            return defaultValue;

        for (InterpolatorInfo info : values()) {
            if (info.interpolator == interpolator || (info.interpolator != null && info.interpolator.getClass() == interpolator.getClass()))
                return info;
        }

        return defaultValue;
    }
}
