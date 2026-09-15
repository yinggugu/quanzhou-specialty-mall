package com.quanzhou.mall.bean;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("chat_message")
public class ChatMessage {
    private Integer id; private Integer sessionId; private Integer userId;
    private Integer isAdmin; private String msgType; private String content;
    private String orderInfo; private String contentFileUrl;
    private String createTime; private Integer isDel;
}
