package com.xingtu.learning.common;

/**
 * 统一响应结构
 */
public record Result<T>(int code, String msg, T data) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "ok", data);
    }

    public static Result<Void> ok() {
        return new Result<>(200, "ok", null);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(500, msg, null);
    }
}
