package kev.app.timeless.util;

public interface Result<T> {
    void onSuccess(T t);
    void onFailure(Exception e);
}
