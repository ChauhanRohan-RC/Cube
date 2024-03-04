package main;

import gl.animation.interpolator.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.StringJoiner;

public enum InterpolatorInfo {

    DEFAULT("default",
            "Default",
            null
    ),

    LINEAR("linear",
            "Linear",
            LinearInterpolator.getSingleton()
    ),

    BOUNCE("bounce",
            "Bounce",
            BounceInterpolator.getSingleton()
    ),

    ACCELERATE("acc",
            "Accelerate",
            new AccelerateInterpolator()
    ),

    DECELERATE("dec",
            "Decelerate",
            new DecelerateInterpolator()
    ),

    ACCELERATE_DECELERATE("acd",
            "Accelerate Decelerate",
            AccelerateDecelerateInterpolator.getSingleton()
    ),

    ANTICIPATE("anticipate",
            "Anticipate",
            new AnticipateInterpolator()
    ),

    OVERSHOOT("overshoot",
            "Overshoot",
            new AnticipateOvershootInterpolator()
    );

    @NotNull
    public final String key;
    @NotNull
    public final String displayName;
    @Nullable
    public final Interpolator interpolator;

    InterpolatorInfo(@NotNull String key, @NotNull String displayName, @Nullable Interpolator interpolator) {
        this.key = key;
        this.displayName = displayName;
        this.interpolator = interpolator;
    }

    public static String getDisplayInfo() {
        final StringJoiner sj = new StringJoiner("\n");

        for (InterpolatorInfo info : values()) {
            sj.add("\t" + info.key + " -> " + info.displayName);
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
