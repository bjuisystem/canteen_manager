package com.javaweb.canteen.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.javaweb.canteen.common.MyTimeUtils;
import com.javaweb.canteen.entity.*;
import com.javaweb.canteen.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 配置定时任务
 */
@Slf4j
@Component
public class MyScheduleTask {
    @Autowired
    private SaleService saleService;

    @Autowired
    private BlanketOrderService blanketOrderService;

    @Autowired
    private ShopCartService shopCartService;

    @Autowired
    private TimeConfigService timeConfigService;

    /**
     * 每月的1号会自动生成月度销售信息
     */
    @Scheduled(cron = "0 0 0 1 * ?")    // 执行时间为每月的1号的00:00:00
    public void addMonthSale(){
        // 构建销售对象
        Sale sale = new Sale();
        // 获取对应年月
        Date currDate = DateUtil.lastMonth();
        String month = DateUtil.formatDate(currDate).substring(0,7);
        sale.setMonth(month);

        // 查询这个月总金额
        BigDecimal totalPrice = BigDecimal.ZERO;
        LambdaQueryWrapper<BlanketOrder> queryWrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotEmpty(month)){
            month = month + "-01";
            Date begin = MyTimeUtils.getMonthOfBeginTime(month);
            Date end = MyTimeUtils.getMonthOfEndTime(month);
            queryWrapper.between(BlanketOrder::getMealDate, begin, end);
        }
        List<BlanketOrder> list = blanketOrderService.list(queryWrapper);
        for (BlanketOrder bo : list) {
            if (bo.getTotalPrice() != null) {
                totalPrice = totalPrice.add(bo.getTotalPrice());
            }
        }
        sale.setTotalPrice(totalPrice);

        boolean res = saleService.save(sale);
        log.info("上个月的销售订单自动生成{}", res ? "成功" : "失败");
    }

    /**
     * 每周的周一的0点整自动清除所有用户的购物车信息
     */
    @Scheduled(cron = "#{@timeConfigServiceImpl.getClearCartCron()}")
    public void clearShopCart(){
        LambdaQueryWrapper<ShopCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.or();
        shopCartService.remove(queryWrapper);
        log.info("本周已结束，自动清除所有用户购物车");
    }

}
