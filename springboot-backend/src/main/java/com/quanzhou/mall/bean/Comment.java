package com.quanzhou.mall.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer productId;
    private Integer userId;
    private Integer orderItemId;
    /** 1-5 星级 */
    private Integer score;
    private String content;
    /** 图片路径逗号分隔 */
    private String imgList;
    /** 0=实名 1=匿名 */
    private Integer isAnonymous;
    private String createTime;
    /** 1=展示 0=隐藏 */
    private Integer isShow;
    /** 管理员回复 */
    private String replyContent;
    /** 点赞数 */
    private Integer likeCount;
}
