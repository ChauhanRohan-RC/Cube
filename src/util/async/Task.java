package util.async;


/**
 * Definition of a task with some output
 * */
@FunctionalInterface
public interface Task<Output> {
    Output begin();
}
