package com.quanzhou.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quanzhou.mall.bean.AfterSale;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AfterSaleMapper extends BaseMapper<AfterSale> {
    @Select("SELECT COUNT(*) FROM after_sale WHERE order_id=#{orderId} AND bind_item_ids=#{itemIds} AND status NOT IN(4,5,7)")
    int countActiveByItem(@Param("orderId") int orderId, @Param("itemIds") String itemIds);
    @Select("SELECT COUNT(*) FROM after_sale WHERE order_id=#{orderId} AND status IN(4,7)")
    int countSuccessByOrder(@Param("orderId") int orderId);
    @Select("SELECT COUNT(*) FROM after_sale WHERE order_id=#{orderId} AND status IN(0,1,2,3)")
    int countActiveByOrder(@Param("orderId") int orderId);
    // 审核超时自动同意：status 0→1（待寄回），不设 finish_time
    @Update("UPDATE after_sale SET status=1 WHERE status=0 AND audit_deadline IS NOT NULL AND audit_deadline<=NOW()")
    int autoAuditExpired();
    @Update("UPDATE after_sale SET status=7,finish_time=NOW() WHERE status=1 AND return_deadline IS NOT NULL AND return_deadline<=NOW()")
    int autoReturnExpired();
    @Update("UPDATE after_sale SET status=4,refund_actual=refund_estimate,finish_time=NOW() WHERE status=3 AND check_refund_deadline IS NOT NULL AND check_refund_deadline<=NOW()")
    int autoRefundExpired();
}
