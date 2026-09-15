package com.quanzhou.mall.bean;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("chat_session")
public class ChatSession {
    private Integer id; private Integer userId; private String username;
    private String orderNo; private String goodsName; private String consultType;
    private String status; private String remark;
    private Integer unreadCount; private String lastMessage; private String lastTime;
    private Integer adminDel; private Integer isForceUnread;
    private String createTime; private String updateTime;
}
