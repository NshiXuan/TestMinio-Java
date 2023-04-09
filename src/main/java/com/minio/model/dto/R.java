package com.minio.model.dto;

import com.minio.exception.Code;
import lombok.Data;

import java.io.Serializable;

@Data
public class R<T> implements Serializable {
    private Integer code; // 0代表成功 -1代表失败
    private T data;
    private String msg;

    public R() {
    }

    public R(Integer code, T data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
    }

    public R(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static <T> R<T> success() {
        return new R<>(0, "success");
    }

    public static <T> R<T> success(T data) {
        return new R<>(0, data, "success");
    }

    public static <T> R<T> success(String msg, T data) {
        return new R<>(0, data, msg);
    }

    public static <T> R<T> success(Code code) {
        return new R<>(code.getCode(), code.getMsg());
    }

    public static <T> R<T> success(Code code, T data) {
        return new R<>(code.getCode(), data, code.getMsg());
    }

    public static <T> R<T> error(String msg) {
        return new R<>(-1, msg);
    }

    public static <T> R<T> error(Code code) {
        return new R<>(code.getCode(), code.getMsg());
    }

    public static <T> R<T> error(Integer code, String msg) {
        return new R<>(code, msg);
    }
}
