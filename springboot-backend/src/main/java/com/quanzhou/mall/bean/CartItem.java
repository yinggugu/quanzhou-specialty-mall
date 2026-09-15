package com.quanzhou.mall.bean;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("cart")
public class CartItem {
    private Integer id; private Integer userId; private Integer productId;
    @TableField(exist = false) private String productName;
    @TableField(exist = false) private Double productPrice;
    @TableField(exist = false) private String productImage;
    private Integer quantity;
    @TableField(exist = false) private Double totalPrice;
}
