package util.models;

public class Wrapper<T> {

    private T value;

    public Wrapper(T value) {
        this.value = value;
    }

    public synchronized T getSync() { return value; }
    public synchronized void setSync(T value) { this.value = value; }

    public T get() { return value; }
    public void set(T value) { this.value = value; }


    public static class Long {

        private long val;

        public Long(long value) {
            val = value;
        }

        public Long() {}

        public synchronized long getSync() { return val; }
        public synchronized void setSync(long value) { val = value; }

        public long get() { return val; }
        public void set(long value) { val = value; }

        public void add(long delta) {
            val += delta;
        }

        public void subtract(long delta) {
            val -= delta;
        }
    }

    public static class Int {

        private int val;

        public Int(int value) {
            val = value;
        }

        public Int() {}

        public synchronized int getSync() { return val; }
        public synchronized void setSync(int value) { val = value; }

        public int get() { return val; }
        public void set(int value) { val = value; }

        public void add(int delta) {
            val += delta;
        }

        public void subtract(int delta) {
            val -= delta;
        }
    }


    public static class Doub {

        private double val;

        public Doub(double value) {
            val = value;
        }

        public Doub() {}

        public synchronized double getSync() { return val; }
        public synchronized void setSync(double value) { val = value; }

        public double get() { return val; }
        public void set(double value) { val = value; }

        public void add(double delta) {
            val += delta;
        }

        public void subtract(double delta) {
            val -= delta;
        }
    }

    public static class Bool {

        private boolean val;

        public Bool(boolean value) {
            val = value;
        }

        public Bool() {}

        public synchronized boolean getSync() { return val; }
        public synchronized void setSync(boolean value) { val = value; }

        public boolean get() { return val; }
        public void set(boolean value) { val = value; }

        public final boolean invert() {
            final boolean new_val = !val;
            val = new_val;
            return new_val;
        }
    }

}
