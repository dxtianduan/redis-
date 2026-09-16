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

        // ★【本次修复】把真实的异常摘要一并返回，而不是永远只说"服务器异常"。
        //
        // 为什么必须改？这个类原来对所有运行时异常一律返回"服务器异常"，
        // 而前端 common.js 会把这个 errorMsg 原样弹出来。于是无论是
        //     Redis 脚本报错（ERR value is not an integer...）、
        //     SQL 列名写错（Unknown column 'stoke'）、
        //     还是空指针，
        // 用户看到的都是同一句"服务器异常" —— 报错信息彻底失去定位能力，
        // 只能靠翻 IDEA 控制台，问题排查全靠猜。
        //
        // 带上 message 之后，前端弹出的就是真正的原因（例如上面那句 Redis 报错），
        // 一眼就能判断该去查数据、还是查代码。
        //
        // ⚠️ 生产环境请改回只返回"服务器异常"：异常信息可能包含表名、字段名、
        //    内部路径等实现细节，直接返回给外部是信息泄露。
        String detail = e.getMessage();
        if (detail == null || detail.trim().isEmpty()) {
            return Result.fail("服务器异常（" + e.getClass().getSimpleName() + "，无详细信息，请看后端控制台）");
        }
        return Result.fail("服务器异常：" + detail);
    }
}
