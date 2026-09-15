package com.quanzhou.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quanzhou.mall.bean.WordFilter;
import org.apache.ibatis.annotations.*;

@Mapper
public interface WordFilterMapper extends BaseMapper<WordFilter> {
    @Select("SELECT word FROM word_filter")
    java.util.List<String> getAllWords();
    @Insert("INSERT INTO word_filter (word) VALUES (#{word})")
    void addWord(String word);
    @Update("UPDATE word_filter SET word=#{word} WHERE id=#{id}")
    void updateWord(@Param("id") int id, @Param("word") String word);
    @Delete("DELETE FROM word_filter WHERE id=#{id}")
    void deleteWord(int id);
}
