package util.async;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Async {

    /**
     *  No. of available CPU-CORES, can be different from actual physical core count
     *  */
    public static final int NO_CPU_CORES = Runtime.getRuntime().availableProcessors();

    /**
     * Thread Pool Executor with unlimited workers (but will prune unused threads after 60s, so very efficient)
     * */
    public static final ExecutorService THREAD_POOL_EXECUTOR = Executors.newCachedThreadPool();

//    /**
//     *  Handler of Main(UI) thread
//     *  */
//    public static final Handler UI_HANDLER = new Handler(Looper.getMainLooper());

    /**
     * Optimal stack frames count (no of deep method calls in a thread)
     * */
    public static final int OPTIMAL_STACK_FRAME_COUNT = 600;


    public static boolean isOnMainThread() {
        return EventQueue.isDispatchThread();
    }

//    public static void cancelUiPosts(@NotNull Runnable runnable) {
//        UI_HANDLER.removeCallbacks(runnable);
//    }
    
    public static void uiPost(@NotNull Runnable runnable, int delayMs) {
        if (delayMs <= 0) {
            EventQueue.invokeLater(runnable);
        } else {
            final Timer t = new Timer(delayMs, e -> runnable.run());
            t.setRepeats(false);
            t.start();
        }
    }

    public static void uiPost(@NotNull Runnable runnable) {
        uiPost(runnable, 0);
    }

    public static void postIfNotOnMainThread(@NotNull Runnable runnable) {
        if (isOnMainThread()) {
            runnable.run();
        } else {
            uiPost(runnable);
        }
    }

    public static <T> void uiPost(T data, Consumer<T> consumer, int delayMs) {
        uiPost(() -> consumer.consume(data), delayMs);
    }

    public static <T> void uiPost(T data, Consumer<T> consumer) {
        uiPost(data, consumer, 0);
    }


    public static void throwIfCancelled(@Nullable CancellationProvider c) throws CancellationException {
        if (c != null && c.isCancelled())
            throw CancellationProvider.EXCEPTION;
    }



    public static void execute(@NotNull Runnable r) {
        THREAD_POOL_EXECUTOR.execute(r);
    }

    @NotNull
    public static Canceller execute(@NotNull Runnable mainBackTask, @NotNull Runnable postUiTask, int uiTaskDelay) {
        final Canceller c = Canceller.basic();

        execute(() -> {
            mainBackTask.run();
            uiPost(() -> { if (!c.isCancelled()) postUiTask.run(); }, uiTaskDelay);
        });

        return c;
    }

    public static @NotNull Canceller execute(@NotNull Runnable mainBackTask, @NotNull Runnable postUiTask) {
        return execute(mainBackTask, postUiTask, 0);
    }

    public static @NotNull <T> Canceller execute(@NotNull Task<? extends T> task, @Nullable Consumer<? super T> handler, int handlerDelay) {
        final Canceller c = Canceller.basic();

        execute(() -> {
            final T out = task.begin();
            if (handler != null) {
                uiPost(() -> handler.onProcessed(out, c), handlerDelay);
            }
        });

        return c;
    }

    public static @NotNull <T> Canceller execute(@NotNull Task<? extends T> task, @Nullable Consumer<? super T> consumer) {
        return execute(task, consumer, 0);
    }

    public static @NotNull <In, Out> Canceller execute(@NotNull Function<? super In, ? extends Out> function, @Nullable Consumer<? super Out> consumer, In input, int consumerDelay) {
        return execute(() -> function.apply(input), consumer, consumerDelay);
    }

    public static @NotNull <In, Out> Canceller execute(@NotNull Function<? super In, ? extends Out> function, @Nullable Consumer<? super Out> consumer, In input) {
        return execute(function, consumer, input, 0);
    }

    public static @NotNull <T> Canceller execute(@NotNull ThrowableTask<T> task, @Nullable TaskConsumer<T> consumer, int consumerDelayMs) {
        final Canceller c = Canceller.basic();

        execute(() -> {
            T out;
            try {
                out = task.begin();
                if (consumer != null) {
                    uiPost(() -> consumer.onProcessed(out, c), consumerDelayMs);
                }
            } catch (Throwable e) {
                if (consumer != null) {
                    uiPost(() -> consumer.onFailed(e), consumerDelayMs);
                }
            }
        });

        return c;
    }

    public static @NotNull <T> Canceller execute(@NotNull ThrowableTask<T> task, @Nullable TaskConsumer<T> consumer) {
        return execute(task, consumer, 0);
    }

    public static @NotNull <In, Out> Canceller execute(@NotNull ThrowableFunction<In, Out> task, @Nullable TaskConsumer<Out> consumer, In input, int consumerDelayMs) {
        return execute(() -> task.begin(input), consumer, consumerDelayMs);
    }

    public static @NotNull <In, Out> Canceller execute(@NotNull ThrowableFunction<In, Out> task, @Nullable TaskConsumer<Out> consumer, In input) {
        return execute(task, consumer, input, 0);
    }




    public static void performNoThrow(@NotNull ThrowableRunnable task, @Nullable String msg) {
        try {
            task.run();
        } catch (Throwable t) {
            System.err.println(msg);
            t.printStackTrace(System.err);
        }
    }

    public static void performNoThrow(@NotNull ThrowableRunnable task) {
        performNoThrow(task, null);
    }


    public static void close(@Nullable AutoCloseable closeable) {
        if (closeable != null) {
            performNoThrow(closeable::close);
        }
    }

    public static void closeAll(@Nullable AutoCloseable... closeables) {
        if (closeables != null) {
            for (AutoCloseable c: closeables) {
                close(c);
            }
        }
    }



    public static int stackFrameCount() {
        return Thread.currentThread().getStackTrace().length;
    }

    /**
     * Executes the given task on same thread if thread stack frame count is optimal, else switches thread
     * */
    public static void executeSafelyWorker(@NotNull Runnable task) {
        final int stackFrameCount = stackFrameCount();
//        Log.d(CopierBottomSheet.TAG, "executeSafely: StackSize: " + stackFrameCount);

        if (stackFrameCount >= OPTIMAL_STACK_FRAME_COUNT) {
//            Log.d(CopierBottomSheet.TAG, "executeSafely: stack size equal or above " + OPTIMAL_STACK_FRAME_COUNT + ", switching thread...");
            execute(task);        // on another thread
        } else {
//            Log.d(CopierBottomSheet.TAG, "executeSafely: same thread");
            task.run();         // on same thread
        }
    }


    /**
     * A Cancellable ThrowableRunnable executor
     * */
    public static class CExecutor implements Canceller {

        private volatile boolean cancelled;

        @Nullable
        private Future<?> future;


        /**
         *  Check the running status of the task
         *
         * @return true if task is still running, false otherwise
         * */
        public boolean isRunning() { return !(future == null || future.isDone()); }

        /**
         * Check the cancellation status of the task
         *
         * Note : it does not guarantees that the task is killed
         * @return true if task is cancelled via {@link #cancel(boolean)}, false otherwise
         * */
        @Override
        public boolean isCancelled() { return cancelled; }


        /**
         * Cancels execution of this task
         *
         * this method DO NOT STOP background execution, callers must call {@link #isCancelled()} from
         * {@link CRun#run(CancellationProvider)} on {@link Canceller}
         *
         * if task is not yet started, task will not be executed
         * if task is running, {@param interrupt} determines whether to interrupt execution or not
         *
         * if already cancelled, this has no effect
         * */
        @Override
        public void cancel(boolean interrupt) {
            cancelled = true;
            if (future != null) {
                future.cancel(interrupt);
                future = null;
            }
        }




        private void ensurePreConditions() {
            if (THREAD_POOL_EXECUTOR.isShutdown())
                throw new IllegalStateException("Thread Pool Executor is already shut down !!");
            if (isRunning())
                throw new IllegalStateException("ThrowableRunnable is already Running !!");
            cancelled = false;
        }


        /**
         * Executes the given {@link CRun} in Background
         * This is heart of all execute methods, every execute methods delegated to this method
         *
         * @param cRun CRun to execute in background
         * */
        public void execute(@NotNull CRun cRun) {
            ensurePreConditions();
            future = THREAD_POOL_EXECUTOR.submit(() -> cRun.run(CExecutor.this));
        }


        /**
         * Executes a {@link CRun} in background ,after completion of which invokes {@param post}
         * on MainThread after {@param postTaskDelayMs}
         *
         * @param background : CRun background action to perform in background
         * @param post : Runnable to invoke on Main-Thread after background task completed
         * @param postTaskDelayMs : delay in invoking post task
         * */
        public void execute(@NotNull CRun background, @NotNull Runnable post, int postTaskDelayMs) {
            execute((CancellationProvider c) -> {
                background.run(c);
                if (!c.isCancelled()) {
                    uiPost(() -> {
                        if (!c.isCancelled())
                            post.run();
                    }, postTaskDelayMs);
                }
            });
        }

        /**
         * Same as calling {@link #execute(CRun, Runnable, int)}
         * with NO DELAY (0 ms)
         * */
        public void execute(@NotNull CRun background, @NotNull Runnable post) {
            execute(background, post, 0);
        }


        /**
         * Executes the given {@link CTask} on background and invokes supplied data consumer
         * with it's output after {@param consumerDelayMs}
         *
         * @param task ThrowableRunnable to execute in background
         * @param consumer DataHandler which handles the output of given task
         * @param consumerDelayMs delay to invoke consumer
         * */
        public <Out> void execute(@NotNull CTask<? extends Out> task, @NotNull Consumer<? super Out> consumer, int consumerDelayMs) {
            execute((CancellationProvider c) ->{
                final Out output = task.begin(c);
                uiPost(() -> consumer.onProcessed(output, c), consumerDelayMs);
            });
        }

        /**
         * Same as calling {@link #execute(CTask, Consumer, int)}
         * with NO DELAY (0 ms)
         * */
        public <Out> void execute(@NotNull CTask<? extends Out> task, @NotNull Consumer<? super Out> consumer) {
            execute(task, consumer, 0);
        }


        /** Executes the given {@link CFunction} on background and invokes supplied data handler
         * with it's output after {@param consumerDelayMs}
         *
         * @param function : i/o ThrowableRunnable to execute in background
         * @param input : Input to be fed in ioTask
         * @param consumer : DataHandler which handles the output of given task
         * @param consumerDelayMs : delay to invoke handler
         * */
        public <In, Out> void execute(@NotNull CFunction<? super In, ? extends Out> function, In input, @NotNull Consumer<? super Out> consumer, int consumerDelayMs) {
            execute((CancellationProvider c) -> function.apply(input, c), consumer, consumerDelayMs);
        }

        /** Same as calling {@link #execute(CFunction, Object, Consumer, int)}
         * with NO DELAY (0 ms)
         * */
        public <In, Out> void execute(@NotNull CFunction<? super In, ? extends Out> function, In input, @NotNull Consumer<? super Out> consumer) {
            execute(function, input, consumer, 0);
        }

        public <In> void execute(@NotNull CVoidFunction<? super In> function, In input) {
            execute((CancellationProvider c) -> function.apply(input, c));
        }


        public <Out> void execute(@NotNull ThrowableCTask<? extends Out> task, @Nullable TaskConsumer<? super Out> consumer, int consumerDelayMs) {
            execute((CancellationProvider c) ->{
                try {
                    Out out = task.begin(c);
                    if (consumer != null) {
                        uiPost(() -> consumer.onProcessed(out, c), consumerDelayMs);
                    }
                } catch (Throwable e) {
                    if (consumer != null) {
                        uiPost(() -> consumer.onFailed(e));
                    }
                }
            });
        }

        public <Out> void execute(@NotNull ThrowableCTask<? extends Out> task, @Nullable TaskConsumer<? super Out> consumer) {
            execute(task, consumer, 0);
        }

        public <In, Out> void execute(@NotNull ThrowableCFunction<? super In, ? extends Out> function, @Nullable TaskConsumer<? super Out> consumer, In input, int consumerDelayMs) {
            execute(function.toTask(input), consumer, consumerDelayMs);
        }

        public <In, Out> void execute(@NotNull ThrowableCFunction<? super In, ? extends Out> function, @Nullable TaskConsumer<? super Out> consumer, In input) {
            execute(function, consumer, input, 0);
        }
    }





    /**
     * An Async task Executor
     * */
    public static abstract class Executor<Input, Progress, Output> implements Canceller {

        private volatile boolean cancelled;
        private Future<?> future;

        /** Cancels execution of this task
         *
         * this method DO NOT STOP background execution, callers must call {@link #isCancelled()} from
         * {@link #inBackground(Object)} to check cancellation status
         *
         * if task is not yet started, task will not be executed
         * if task is running, {@param interrupt} determines whether to interrupt execution or not
         * it does guarantees that {@link #onComplete(Object)} will not be invoked
         *
         * if already cancelled, this has no effect
         * */
        @Override
        public void cancel(boolean interrupt) {
            cancelled = true;
            if (future != null) {
                future.cancel(interrupt);
                future = null;
            }
        }

        /** Check the cancellation status of the task
         *
         * Note : it does not guarantees that the task is killed, but it does guarantees that {@link #onComplete(Object)} will not be invoked
         * @return true if task is cancelled via {@link #cancel(boolean)}, false otherwise
         * */
        @Override
        public boolean isCancelled() { return cancelled; }


        /** Check the running status of the task
         *
         * @return true if task is still running, false otherwise
         * */
        public boolean isRunning() { return !(future == null || future.isDone()); }


        private void ensurePreConditions() {
            if (THREAD_POOL_EXECUTOR.isShutdown())
                throw new IllegalStateException("Thread Pool Executor is already shut down !!");
            if (isRunning())
                throw new IllegalStateException("ThrowableRunnable is already Running !!");
            cancelled = false;
        }

        /** Call this method from {@link #inBackground(Object)} to Posts Progress to main Thread
         *
         * 1. This will not check for cancellation
         * 2. Calls {@link #onProgress(Object)} with progress specified
         * */
        protected void postProgress(Progress progress) {
            uiPost(progress, this::onProgress);
        }

        /** Executes the task with given Input
         *
         * if task is cancelled via {@link #cancel(boolean)} task will not be executed
         *
         * @throws IllegalStateException if {@link #THREAD_POOL_EXECUTOR} is already shut down or if {@link #isRunning()}
         * @throws java.util.concurrent.RejectedExecutionException if {@link #THREAD_POOL_EXECUTOR} rejects execution
         * */
        public void execute(final Input input) {
            ensurePreConditions();

            final Runnable backTask = () -> {
                Output output = inBackground(input);
                if (!cancelled) {
                    uiPost(() -> {
                        future = null;
                        onComplete(output);
                    });
                }
            };

            onStart();
            future = THREAD_POOL_EXECUTOR.submit(backTask);
        }


        protected void onStart() {}

        protected abstract Output inBackground(final Input input);

        protected void onProgress(Progress progress) {}

        protected void onComplete(Output output) {}
    }


    public static void shutDown() {
        THREAD_POOL_EXECUTOR.shutdownNow();
    }
}
