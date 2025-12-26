package com.javaweb.canteen.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.math.BigDecimal;

@Data
@TableName("blanketOrder")
public class BlanketOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long mealId;

    private String name;

    private String unit;

    private BigDecimal weight;

    private BigDecimal price;

    private BigDecimal totalPrice;

    private Long orderId;

    private Date createTime;

    private Date mealDate;
}
