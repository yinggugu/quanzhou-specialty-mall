package com.quanzhou.mall.bean;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
@TableName("orders")
public class Order {
    @TableField(exist = false)
    private List<OrderItem> orderItems;
    @TableField(exist = false)
    private Integer afterSaleId;
    @TableField(exist = false)
    private Integer afterSaleStatus;
    private Integer id;
    private String orderNo;
    private Integer userId;
    private String username;
    private String phone;
    private String address;
    private Double totalPrice;
    /** 0=待支付, 1=已支付, 2=已发货, 3=已完成, 4=已取消 */
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date createTime;
}
