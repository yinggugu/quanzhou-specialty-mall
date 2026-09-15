package com.quanzhou.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quanzhou.mall.bean.AiChatRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface AiChatRecordMapper extends BaseMapper<AiChatRecord> {
    // 查询当前用户会话最近N条历史消息，按时间正序（最早在前）
    List<AiChatRecord> selectLimitHistory(@Param("userId") Integer userId,
                                          @Param("sessionId") String sessionId,
                                          @Param("limit") Integer limit);
}
