package com.quanzhou.mall.bean;

import lombok.Data;

@Data
public class AiChatWsMsg {
    private String type;
    private String sessionId;
    private String content;
}