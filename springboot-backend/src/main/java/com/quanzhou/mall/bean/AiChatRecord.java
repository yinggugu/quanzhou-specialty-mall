package com.quanzhou.mall.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_chat_record")
public class AiChatRecord {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Integer userId;
    private String sessionId;
    private String role;
    private String content;
    private LocalDateTime createTime;
}