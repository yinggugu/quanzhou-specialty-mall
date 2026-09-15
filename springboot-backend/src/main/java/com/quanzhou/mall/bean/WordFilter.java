package com.quanzhou.mall.bean;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("word_filter")
public class WordFilter {
    private Integer id;
    private String word;
    private String createTime;
}
