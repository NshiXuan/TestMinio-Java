package com.minio.exception;

public enum Code {
    SERVICE_BUSY(500, "服务繁忙"), // 枚举分隔不能使用; 需要使用,
    UPLOAD_ERROR(1001, "上传失败");

    private Integer code;
    private String msg;

    private Code(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
