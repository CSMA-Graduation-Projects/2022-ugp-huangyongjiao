package com.example.exception;

import com.example.common.Result;
import com.example.common.enums.ResultCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public Result exceptionHandler(Exception e, HttpServletRequest request) {
        log.error("请求地址: {}", request.getRequestURI(), e);
        return Result.error(ResultCodeEnum.SYSTEM_ERROR.code, "系统异常");
    }

    @ExceptionHandler(CustomException.class)
    public Result customExceptionHandler(CustomException e, HttpServletRequest request) {
        log.error("业务异常，请求地址: {}, 异常信息: {}", request.getRequestURI(), e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }
}