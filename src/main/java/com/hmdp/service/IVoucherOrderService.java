package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     * 秒杀下单入口。
     * 现在只做两件事：执行 Lua 脚本判断资格 → 把订单丢进阻塞队列 → 立刻返回订单id。
     * 真正的数据库写入由后台线程异步完成。
     */
    Result killVoucher(Long voucherId);

    /**
     * 真正写数据库：判断一人一单 → 扣库存 → 建订单。
     *
     * 【为什么参数从 Long voucherId 改成 VoucherOrder】
     * 这个方法现在是被"线程池里的后台线程"调用的，而 UserHolder 底层是 ThreadLocal，
     * 它只在处理 HTTP 请求的那个线程里有效，换到别的线程就取不到用户了。
     * 所以用户id、券id、订单id必须在主线程里提前组装好，整体传进来。
     */
    void createVoucherOrder(VoucherOrder voucherOrder);
}
