package com.github.zuihou.base;

import com.github.zuihou.exception.BizException;
import com.github.zuihou.exception.code.BaseExceptionCode;
import com.github.zuihou.utils.JSONUtils;


/**
 * @author zuihou
 * @createTime 2017-12-13 10:55
 */
public class Result<T> {
    public static final String DEF_ERROR_MESSAGE = "系统繁忙，请稍候再试";
    public static final String HYSTRIX_ERROR_MESSAGE = "请求超时，请稍候再试";
    public static final int SUCCESS_CODE = 0;
    public static final int FAIL_CODE = -1;
    public static final int TIMEOUT_CODE = -2;
    /**
     * 统一参数验证异常
     */
    public static final int VALID_EX_CODE = -9;
    public static final int OPERATION_EX_CODE = -10;
    /**
     * 调用是否成功标识，0：成功，-1:系统繁忙，此时请开发者稍候再试 详情见[ExceptionCode]
     */
    private int code;

    /**
     * 调用结果
     */
    private T data;

    /**
     * 结果消息，如果调用成功，消息通常为空T
     */
    private String msg = "ok";

    private Result() {
        super();
    }

    public Result(int code, T data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
    }

    public static <E> Result<E> result(int code, E data, String msg) {
        return new Result<>(code, data, msg);

    }

    /**
     * 请求成功消息
     *
     * @param data 结果
     * @return RPC调用结果
     */
    public static <E> Result<E> success(E data) {
        return new Result<>(SUCCESS_CODE, data, "ok");
    }

    public static Result<Boolean> success() {
        return new Result<>(SUCCESS_CODE, true, "ok");
    }

    /**
     * 请求成功方法 ，data返回值，msg提示信息
     *
     * @param data 结果
     * @param msg  消息
     * @return RPC调用结果
     */
    public static <E> Result<E> success(E data, String msg) {
        return new Result<>(SUCCESS_CODE, data, msg);
    }

    /**
     * 请求失败消息
     *
     * @param msg
     * @return
     */
    public static <E> Result<E> fail(int code, String msg) {
        return new Result<>(code, null, (msg == null || msg.isEmpty()) ? DEF_ERROR_MESSAGE : msg);
    }

    public static <E> Result<E> fail(String msg) {
        return fail(OPERATION_EX_CODE, msg);
    }


    public static <E> Result<E> fail(BaseExceptionCode exceptionCode) {
        return validFail(exceptionCode);
    }

    public static <E> Result<E> fail(BizException exception) {
        if (exception == null) {
            return fail(DEF_ERROR_MESSAGE);
        }
        return new Result<>(exception.getCode(), null, exception.getMessage());
    }

    /**
     * 请求失败消息，根据异常类型，获取不同的提供消息
     *
     * @param throwable 异常
     * @return RPC调用结果
     */
    public static <E> Result<E> fail(Throwable throwable) {
        return fail(FAIL_CODE, throwable != null ? throwable.getMessage() : DEF_ERROR_MESSAGE);
    }

    public static <E> Result<E> validFail(String msg) {
        return new Result<>(VALID_EX_CODE, null, (msg == null || msg.isEmpty()) ? DEF_ERROR_MESSAGE : msg);
    }

    public static <E> Result<E> validFail(BaseExceptionCode exceptionCode) {
        return new Result<>(exceptionCode.getCode(), null,
                (exceptionCode.getMsg() == null || exceptionCode.getMsg().isEmpty()) ? DEF_ERROR_MESSAGE : exceptionCode.getMsg());
    }

    public static <E> Result<E> timeout() {
        return fail(TIMEOUT_CODE, HYSTRIX_ERROR_MESSAGE);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * 逻辑处理是否成功
     *
     * @return 是否成功
     */
    public Boolean getIsSuccess() {
        return this.code == SUCCESS_CODE;
    }

    @Override
    public String toString() {
        return JSONUtils.toJsonString(this);
    }
}
