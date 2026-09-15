package com.quanzhou.mall.bean;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("product")
public class Product {
    private Integer id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private String image;
    private Integer sort;
    private Integer categoryId;
    @TableField(exist = false)
    private String categoryName;
    /** 0=下架, 1=在售 */
    private Integer status;
    private String createTime;
}
