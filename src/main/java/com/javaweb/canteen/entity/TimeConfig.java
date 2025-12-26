package com.javaweb.canteen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("time_config")
public class TimeConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_deadline") // 明确指定指定数据库指定数据库字段名
    private String orderDeadline; // 订餐截止时间，格式HH:mm:ss

    @TableField("meal_start_time") // 明确指定数据库字段名
    private String mealStartTime; // 配餐开始时间，格式HH:mm:ss

    @TableField("update_time") // 明确指定数据库字段名
    private Date updateTime; // 配置更新时间
}