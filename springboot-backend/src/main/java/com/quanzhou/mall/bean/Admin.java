package com.quanzhou.mall.bean;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@TableName("admin")
public class Admin {
    private Integer id;
    private String username;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String name;
    private String email;
    private String phone;
    private String createTime;
    private String updateTime;
    /** 0=禁用, 1=启用 */
    private Integer status;
}
