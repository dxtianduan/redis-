package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        //【修复】强制把 type 设为 1（1 = 秒杀券）。
        // 前端页面只有在 type == 1 时才渲染"限时抢购 + 倒计时 + 剩余量"。
        // 原来这里没有设置 type，完全依赖请求体里传的值：
        // 如果请求里没写 type、或者写成了 0，存进数据库就是 0，
        // 结果是"秒杀表有数据、主表却标记成普通券"，前端永远显示普通按钮。
        // 这个方法本身就叫 addSeckillVoucher（新增秒杀券），type 就必须是 1，不该让调用方来决定。
        voucher.setType(1);

        //【修复】基本校验：秒杀券必须有库存和起止时间。
        // 少了任何一个，前端拿到 null 就没法渲染倒计时，接口也查不出剩余量。
        // 在这里拦住比存进数据库再排错容易得多。
        if (voucher.getStock() == null || voucher.getStock() < 0) {
            throw new IllegalArgumentException("秒杀券必须填写 stock（库存），且不能为负数");
        }
        if (voucher.getBeginTime() == null || voucher.getEndTime() == null) {
            throw new IllegalArgumentException("秒杀券必须填写 beginTime（开始时间）和 endTime（结束时间）");
        }
        if (voucher.getEndTime().isBefore(voucher.getBeginTime())) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }

        // 保存优惠券（注意：此时 voucher.getId() 由 MyBatis-Plus 回填，就是下面写秒杀表要用的券id）
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
    }
}
