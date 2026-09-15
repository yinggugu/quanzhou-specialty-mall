package com.quanzhou.mall.bean;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("order_item")
public class OrderItem {
    private Integer id; private Integer orderId; private Integer productId;
    private String productName; private Double productPrice;
    private Integer quantity; private Double totalPrice;
    @TableField(exist = false)
    private String productImage;
}
