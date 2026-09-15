package com.quanzhou.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quanzhou.mall.bean.ChatSession;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
    @Select("SELECT id FROM chat_session WHERE user_id=#{userId} AND status!='已完成' ORDER BY id DESC LIMIT 1")
    Integer findActiveByUser(@Param("userId") int userId);
    @Select("SELECT user_id FROM chat_session WHERE id=#{id}")
    Integer getUserId(@Param("id") int id);
    @Select("SELECT username FROM chat_session WHERE id=#{id}")
    String getUsername(@Param("id") int id);
    @Update("UPDATE chat_session SET last_message=#{msg},last_time=NOW(),admin_del=0,is_force_unread=0 WHERE id=#{id}")
    void updateLastMsg(@Param("id") int id, @Param("msg") String msg);
    @Update("UPDATE chat_session SET unread_count=0,is_force_unread=0 WHERE id=#{id}")
    void markRead(@Param("id") int id);
    @Update("UPDATE chat_session SET unread_count=unread_count+1 WHERE id=#{id}")
    void incrUnread(@Param("id") int id);
    @Update("UPDATE chat_session SET unread_count=1,is_force_unread=1,status='待回复' WHERE id=#{id}")
    void markUnread(@Param("id") int id);
    @Update("UPDATE chat_session SET admin_del=1 WHERE id=#{id}")
    void adminDel(@Param("id") int id);
}
