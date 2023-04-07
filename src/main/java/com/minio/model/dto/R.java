package com.minio.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class R<T> implements Serializable {
    private Integer code;
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

    public static <T> R<T> error(Integer code, String msg) {
        return new R<>(code, msg);
    }
}
