package com.minio.exception;

public class MyException extends RuntimeException {
    private Code commonErr;

    public MyException() {
    }

    public MyException(Code commonErr) {
        this.commonErr = commonErr;
    }

    public Code getCommonErr() {
        return commonErr;
    }

    public void setCommonErr(Code commonErr) {
        this.commonErr = commonErr;
    }

    public static void cast(Code commonErr) {
        throw new MyException(commonErr);
    }
}
