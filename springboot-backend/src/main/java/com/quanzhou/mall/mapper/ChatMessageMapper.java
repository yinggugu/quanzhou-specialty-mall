package com.quanzhou.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quanzhou.mall.bean.ChatMessage;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
    @Select("SELECT COUNT(*) FROM chat_message WHERE session_id=#{sid}")
    int countBySession(@Param("sid") int sessionId);
    @Select("<script>SELECT * FROM chat_message WHERE session_id=#{sid} <if test='filterDeleted'>AND is_del=0</if> ORDER BY create_time ASC</script>")
    java.util.List<ChatMessage> findBySession(@Param("sid") int sessionId, @Param("filterDeleted") boolean filterDeleted);
    @Update("UPDATE chat_message SET is_del=1 WHERE session_id=#{sid}")
    int softDeleteBySession(@Param("sid") int sessionId);
}
