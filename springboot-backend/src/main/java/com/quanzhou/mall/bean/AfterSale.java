package com.quanzhou.mall.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("after_sale")
public class AfterSale {
    @TableId(type = IdType.AUTO)
    private Integer id; private Integer orderId; private Integer userId;
    private String bindItemIds; private String productIds;
    private Integer status;
    private String returnReason; private String descText; private String imgList;
    private String expressNo; private String rejectContent;
    private String receiveImgList;
    private BigDecimal refundEstimate; private BigDecimal refundActual; private BigDecimal refundAmount;
    private String auditDeadline; private String returnDeadline;
    private String merchantConfirmTime; private String checkRefundDeadline;
    private BigDecimal originMoney; private BigDecimal refundMoney;
    private String refundImgs; private String refundDesc;
    private String createTime; private String finishTime;
    @TableField(exist = false)
    private String orderNo; @TableField(exist = false)
    private String username;
}
