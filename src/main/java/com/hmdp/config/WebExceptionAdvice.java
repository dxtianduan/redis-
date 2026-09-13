package com.hmdp.config;

import com.hmdp.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class WebExceptionAdvice {

    /**
     * 【新增】参数校验类异常单独处理，把真实原因返回给前端。
     * <p>
     * 为什么需要这个？因为下面的 handleRuntimeException 会把所有运行时异常
     * 统一变成"服务器异常"。而 IllegalArgumentException 是我们自己主动抛的
     * 业务校验提示（比如"秒杀券必须填写 beginTime"），这种提示应该让调用方看到，
     * 否则用接口文档测试时永远只看到"服务器异常"，根本不知道哪个参数填错了。
     * <p>
     * 注意：真正的程序 bug（空指针、SQL 错误等）依然走下面的兜底分支，
     * 只返回"服务器异常"，不把内部细节暴露给外部，这是安全的做法。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数校验失败：{}", e.getMessage());
        return Result.fail(e.getMessage() == null ? "参数不合法" : e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        log.error(e.toString(), e);
        return Result.fail("服务器异常");
    }
}
