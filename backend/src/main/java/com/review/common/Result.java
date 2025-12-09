package com.review.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private String code;

    private String message;

    private T data;

    public static <T> Result<T> success() {
        return new Result<>("SUCCESS", "操作成功", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>("SUCCESS", "操作成功", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>("SUCCESS", message, data);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>("ERROR", message, null);
    }

    public static <T> Result<T> error(String code, String message) {
        return new Result<>(code, message, null);
    }
}
