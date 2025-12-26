package com.javaweb.canteen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.javaweb.canteen.entity.TimeConfig;
import com.javaweb.canteen.mapper.TimeConfigMapper;
import com.javaweb.canteen.service.TimeConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * 时间配置服务实现类
 * 修复：补充导入、替换线程不安全的SimpleDateFormat、优化查询逻辑
 */
@Slf4j
@Service
public class TimeConfigServiceImpl extends ServiceImpl<TimeConfigMapper, TimeConfig> implements TimeConfigService {

    // 兼容一位/两位小时、可选秒的时间正则（HH:mm[:ss] 或 H:mm[:ss]）
    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]?\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$");
    // 线程安全的时间格式化器（替代SimpleDateFormat）
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter TIME_PARSER = DateTimeFormatter.ofPattern("H:mm:ss");

    // 默认清除购物车cron（原固定规则：每周一0点）
    private static final String DEFAULT_CLEAR_CART_CRON = "0 0 0 ? * 2";
    // 默认历史菜单统计cron（原固定规则：每天23点）
    private static final String DEFAULT_HISTORY_MENU_CRON = "0 0 23 * * ?";

    @Override
    public TimeConfig getCurrentConfig() {
        // 优化：使用LambdaQueryWrapper（即使无条件，也建议保留规范写法）
        LambdaQueryWrapper<TimeConfig> queryWrapper = new LambdaQueryWrapper<>();
        // 假设系统中只有一条配置，取第一条（若有多个，可按id降序取最新）
        TimeConfig config = baseMapper.selectOne(queryWrapper.last("LIMIT 1"));

        // 无配置时初始化默认配置（并保存到数据库，避免每次都返回新对象）
        if (config == null) {
            config = new TimeConfig();
            config.setOrderDeadline("09:00:00");
            config.setMealStartTime("11:30:00");
            config.setUpdateTime(new Date());
            // 保存默认配置到数据库
            baseMapper.insert(config);
            log.info("系统无时间配置，已初始化默认配置：{}", config);
        }
        return config;
    }

    @Override
    public boolean updateConfig(TimeConfig timeConfig) {
        // 更新时自动更新updateTime（若数据库未设置自动更新，手动设置）
        timeConfig.setUpdateTime(new Date());
        // 调用MyBatis-Plus的updateById（底层也是baseMapper.updateById）
        return updateById(timeConfig);
    }

    /**
     * 从数据库orderDeadline生成清除购物车的cron表达式
     * 规则：每周一 + 数据库配置的HH:mm:ss（兼容一位/两位小时）
     * cron格式：秒 分 时 ? 月 周几（2代表周一）
     */
    @Override
    public String getClearCartCron() {
        TimeConfig config = getCurrentConfig();
        String orderDeadline = normalizeTime(config.getOrderDeadline());

        // 1. 校验时间格式（兼容一位/两位小时）
        if (!TIME_PATTERN.matcher(orderDeadline).matches()) {
            log.warn("数据库orderDeadline格式错误（{}），使用默认cron：{}", orderDeadline, DEFAULT_CLEAR_CART_CRON);
            return DEFAULT_CLEAR_CART_CRON;
        }

        // 2. 补全一位小时为两位（使用线程安全的LocalTime替代SimpleDateFormat）
        String standardTime;
        try {
            // 解析为LocalTime（兼容H:mm:ss和HH:mm:ss）
            LocalTime localTime = LocalTime.parse(orderDeadline, TIME_PARSER);
            // 格式化为HH:mm:ss（补全一位小时）
            standardTime = localTime.format(TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            log.error("时间格式解析失败：{}", orderDeadline, e);
            return DEFAULT_CLEAR_CART_CRON;
        }

        // 3. 拆分标准格式的时分秒：HH:mm:ss → 时、分、秒
        String[] timeParts = standardTime.split(":");
        String second = timeParts[2];   // 秒
        String minute = timeParts[1];   // 分
        String hour = timeParts[0];     // 时（已补全为两位）

        // 4. 生成cron表达式（每周一执行）
        String cron = String.format("%s %s %s ? * 2", second, minute, hour);
        log.info("从数据库生成清除购物车cron：{}（原始orderDeadline：{}，标准格式：{}）", cron, orderDeadline, standardTime);
        return cron;
    }

    /**
     * 从数据库orderDeadline生成历史菜单统计的cron表达式
     * 规则：每天 + 数据库配置的HH:mm:ss（兼容一位/两位小时）
     * cron格式：秒 分 时 * * ?（每天执行）
     */
    @Override
    public String getHistoryMenuCron() {
        TimeConfig config = getCurrentConfig();
        String orderDeadline = normalizeTime(config.getOrderDeadline());

        // 1. 校验时间格式（兼容一位/两位小时）
        if (!TIME_PATTERN.matcher(orderDeadline).matches()) {
            log.warn("数据库orderDeadline格式错误（{}），使用默认cron：{}", orderDeadline, DEFAULT_HISTORY_MENU_CRON);
            return DEFAULT_HISTORY_MENU_CRON;
        }

        // 2. 补全一位小时为两位（使用线程安全的LocalTime替代SimpleDateFormat）
        String standardTime;
        try {
            // 解析为LocalTime（兼容H:mm:ss和HH:mm:ss）
            LocalTime localTime = LocalTime.parse(orderDeadline, TIME_PARSER);
            // 格式化为HH:mm:ss（补全一位小时）
            standardTime = localTime.format(TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            log.error("时间格式解析失败：{}", orderDeadline, e);
            return DEFAULT_HISTORY_MENU_CRON;
        }

        // 3. 拆分标准格式的时分秒：HH:mm:ss → 时、分、秒
        String[] timeParts = standardTime.split(":");
        String second = timeParts[2];   // 秒
        String minute = timeParts[1];   // 分
        String hour = timeParts[0];     // 时（已补全为两位）

        // 4. 生成cron表达式（每天执行）
        String cron = String.format("%s %s %s * * ?", second, minute, hour);
        log.info("从数据库生成历史菜单统计cron：{}（原始orderDeadline：{}，标准格式：{}）", cron, orderDeadline, standardTime);
        return cron;
    }

    private String normalizeTime(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() == 5) {
            return trimmed + ":00";
        }
        return trimmed;
    }
}
