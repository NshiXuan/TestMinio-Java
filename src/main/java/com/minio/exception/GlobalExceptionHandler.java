package com.minio.exception;

import com.minio.model.dto.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // @ResponseBody
    @ExceptionHandler(MyException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R myException(MyException e) {
        //记录异常
        log.error("系统异常 --- ", e.getCommonErr().getMsg(), e);

        return R.error(e.getCommonErr().getCode(), e.getCommonErr().getMsg());
    }
}