package com.javaweb.canteen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.javaweb.canteen.entity.TimeConfig;

// 移除错误的 import com.mysql.cj.x.protobuf.MysqlxDatatypes;
public interface TimeConfigService extends IService<TimeConfig> {
    // 获取当前时间配置（系统中通常只有一条配置记录）
    TimeConfig getCurrentConfig();

    // 更新时间配置
    boolean updateConfig(TimeConfig timeConfig);

    // 新增：获取清除购物车的动态cron表达式（返回String即可）
    String getClearCartCron();

    // 新增：获取历史菜单统计的动态cron表达式（基于orderDeadline）
    String getHistoryMenuCron();
}