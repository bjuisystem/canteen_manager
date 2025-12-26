package com.javaweb.canteen.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("shopCart")
public class ShopCart implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Integer scId;

    private Long menuId;

    private String name;

    private String unit;

    private BigDecimal weight;

    private BigDecimal price;

    private BigDecimal totalPrice;

    private String picture;

    private Long userId;
}
