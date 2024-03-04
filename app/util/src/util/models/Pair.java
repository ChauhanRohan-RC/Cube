package util.models;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public  class Pair<F, S> implements Serializable {

    public static final long serialVersionUID = 9958088698489767L;

    public F first;
    public S second;

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public Pair(@NotNull Pair<F, S> source) {
        first = source.first;
        second = source.second;
    }

    public static int toInt(@Nullable Boolean bool) {
        return (bool == null)? -1: bool? 1: 0;
    }

    @Nullable
    public static Boolean toBoolean(int booleanInt) {
        return switch (booleanInt) {
            case 0 -> false;
            case 1 -> true;
            default -> null;
        };
    }


    public static class Uno<T> extends Pair<T, T> implements Serializable {

        public static final long serialVersionUID = 995808869232392371L;

        public Uno(@NotNull Pair<T, T> source) {
            super(source);
        }

        public Uno(T first, T second) {
            super(first, second);
        }
    }

    public static class Str extends Uno<String> implements Serializable {

        public static final long serialVersionUID = 8077662936121325L;

        public Str(@NotNull Pair<String, String> source) {
            super(source);
        }

        public Str(String first, String second) {
            super(first, second);
        }
    }


    public static class Bool implements Serializable {
        
        public static final long serialVersionUID = 807766293612132L;

        public boolean first;
        public boolean second;
        
        public Bool(@NotNull Bool src) {
            this(src.first, src.second);
        }

        public Bool(boolean first, boolean second) {
            this.first = first;
            this.second = second;
        }
    }


    public static class Byte implements Serializable {
        
        public static final long serialVersionUID = 807766293612132L;
        
        public byte first;
        public byte second;

        public Byte(@NotNull Byte src) {
            this(src.first, src.second);
        }

        public Byte(byte first, byte second) {
            this.first = first;
            this.second = second;
        }
    }

    public static class Int implements Serializable {
        
        public static final long serialVersionUID = 8077662936121321L;

        public int first;
        public int second;

        public Int(@NotNull Int src) {
            this(src.first, src.second);
        }

        public Int(int first, int second) {
            this.first = first;
            this.second = second;
        }
    }

    public static class Char implements Serializable {

        public static final long serialVersionUID = 8077665498665736234L;

        public char first;
        public char second;

        public Char(@NotNull Char src) {
            this(src.first, src.second);
        }

        public Char(char first, char second) {
            this.first = first;
            this.second = second;
        }
    }


    public static class Long implements Serializable {
        
        public static final long serialVersionUID = 8077662936121322L;

        public long first;
        public long second;

        public Long(@NotNull Long src) {
            this(src.first, src.second);
        }

        public Long(long first, long second) {
            this.first = first;
            this.second = second;
        }
    }

    public static class Float implements Serializable {
        
        public static final long serialVersionUID = 8077662936121323L;

        public float first;
        public float second;

        public Float(@NotNull Float src) {
            this(src.first, src.second);
        }

        public Float(float first, float second) {
            this.first = first;
            this.second = second;
        }
    }

    public static class Double implements Serializable {
        
        public static final long serialVersionUID = 8077662936121324L;

        public double first;
        public double second;

        public Double(@NotNull Double src) {
            this(src.first, src.second);
        }

        public Double(double first, double second) {
            this.first = first;
            this.second = second;
        }
    }


}
