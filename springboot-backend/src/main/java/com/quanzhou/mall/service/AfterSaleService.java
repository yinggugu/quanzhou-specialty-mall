package com.quanzhou.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quanzhou.mall.bean.*;
import com.quanzhou.mall.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AfterSaleService {
    @Autowired private AfterSaleMapper mapper;
    @Autowired private OrderMapper orderMapper;
    @Autowired private OrderItemMapper orderItemMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private ProductMapper productMapper;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<Map<String,Object>> logStore = Collections.synchronizedList(new ArrayList<>());

    public void addLog(int afterSaleId, String operateType, String userType, int userId, String content, String img) {
        Map<String,Object> l = new LinkedHashMap<>(); l.put("afterSaleId", afterSaleId); l.put("type", operateType);
        l.put("userType", userType); l.put("content", content); l.put("time", LocalDateTime.now().format(DTF)); logStore.add(l);
    }

    public Map<String,Object> apply(Integer userId, Integer orderId, String bindItemIds, String productIds, String returnReason, String descText, String imgList) {
        Map<String,Object> r = new HashMap<>();
        Order o = orderMapper.selectById(orderId);
        if (o == null || !o.getUserId().equals(userId)) { r.put("code",400); r.put("msg","订单不存在"); return r; }
        if (o.getStatus() != 3) { r.put("code",400); r.put("msg","仅已完成订单可申请售后"); return r; }
        if (bindItemIds == null || bindItemIds.trim().isEmpty()) { r.put("code",400); r.put("msg","请选择售后商品"); return r; }
        LambdaQueryWrapper<AfterSale> qw = new LambdaQueryWrapper<>();
        qw.eq(AfterSale::getOrderId, orderId); qw.notIn(AfterSale::getStatus, 4, 5, 7);
        if (mapper.selectCount(qw) > 0) { r.put("code",400); r.put("msg","该商品已有处理中售后"); return r; }
        if (Arrays.asList("质量瑕疵","实物与描述不符","漏发破损").contains(returnReason) && (imgList == null || imgList.trim().isEmpty())) {
            r.put("code",400); r.put("msg","此退货原因必须上传凭证图片"); return r; }
        double estimate = 0;
        List<String> verifiedProductIds = new ArrayList<>();
        for (String itemId : bindItemIds.split(",")) {
            OrderItem oi;
            try { oi = orderItemMapper.selectById(Integer.parseInt(itemId.trim())); }
            catch (NumberFormatException e) { r.put("code",400); r.put("msg","售后商品参数错误"); return r; }
            if (oi == null || !orderId.equals(oi.getOrderId())) {
                r.put("code",403); r.put("msg","售后商品不属于该订单"); return r;
            }
            estimate += oi.getTotalPrice();
            verifiedProductIds.add(String.valueOf(oi.getProductId()));
        }
        AfterSale a = new AfterSale(); a.setOrderId(orderId); a.setUserId(userId);
        a.setBindItemIds(bindItemIds); a.setProductIds(String.join(",", verifiedProductIds)); a.setReturnReason(returnReason);
        a.setDescText(descText); a.setImgList(imgList != null ? imgList : "");
        a.setRefundEstimate(new java.math.BigDecimal(estimate));
        a.setRefundAmount(new java.math.BigDecimal(estimate));
        mapper.insert(a); addLog(a.getId(), "申请", "user", userId, "", "");
        r.put("code",200); r.put("msg","售后申请已提交"); return r;
    }

    public Map<String,Object> cancel(int id, Integer userId) {
        Map<String,Object> r = new HashMap<>(); AfterSale a = mapper.selectById(id);
        if (a == null || !a.getUserId().equals(userId)) { r.put("code",400); r.put("msg","售后单不存在"); return r; }
        if (a.getStatus() != 0 && a.getStatus() != 1) { r.put("code",400); r.put("msg","当前状态不可撤销"); return r; }
        a.setStatus(5); mapper.updateById(a); addLog(id, "撤销", "user", userId, "", ""); r.put("code",200); r.put("msg","已撤销"); return r;
    }

    public List<Map<String,Object>> userList(Integer userId, int page, int pageSize, String keyword) {
        LambdaQueryWrapper<AfterSale> q = new LambdaQueryWrapper<AfterSale>().eq(AfterSale::getUserId, userId).orderByDesc(AfterSale::getCreateTime);
        Page<AfterSale> p = mapper.selectPage(new Page<>(page, pageSize), q);
        List<Map<String,Object>> list = new ArrayList<>();
        for (AfterSale a : p.getRecords()) { Map<String,Object> m = toMap(a); Order o = orderMapper.selectById(a.getOrderId()); m.put("orderNo", o != null ? o.getOrderNo() : ""); list.add(m); }
        return list;
    }

    public Map<String,Object> detail(int id) { AfterSale a = mapper.selectById(id); return a != null ? enrichDetail(a) : null; }
    public Map<String,Object> detailForUser(int id, Integer userId) {
        AfterSale a = mapper.selectById(id);
        return a != null && a.getUserId().equals(userId) ? enrichDetail(a) : null;
    }
    public Map<String,Object> detailByOrderId(int orderId) {
        AfterSale a = mapper.selectOne(new LambdaQueryWrapper<AfterSale>().eq(AfterSale::getOrderId, orderId).orderByDesc(AfterSale::getCreateTime).last("LIMIT 1"));
        return a != null ? enrichDetail(a) : null;
    }
    public Map<String,Object> detailByOrderIdForUser(int orderId, Integer userId) {
        AfterSale a = mapper.selectOne(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getOrderId, orderId).eq(AfterSale::getUserId, userId)
                .orderByDesc(AfterSale::getCreateTime).last("LIMIT 1"));
        return a != null ? enrichDetail(a) : null;
    }

    private Map<String,Object> enrichDetail(AfterSale a) {
        Map<String,Object> m = toMap(a); Order o = orderMapper.selectById(a.getOrderId());
        m.put("orderNo", o != null ? o.getOrderNo() : "");
        List<Map<String,Object>> items = new ArrayList<>();
        if (a.getBindItemIds() != null) for (String itemId : a.getBindItemIds().split(",")) {
            OrderItem oi = orderItemMapper.selectById(Integer.parseInt(itemId.trim()));
            if (oi != null) {
                Map<String,Object> im = new LinkedHashMap<>();
                im.put("id", oi.getId());
                im.put("productName", oi.getProductName());
                im.put("productPrice", oi.getProductPrice());
                im.put("quantity", oi.getQuantity());
                im.put("totalPrice", oi.getTotalPrice());
                Product p = productMapper.selectById(oi.getProductId());
                im.put("productImage", p != null ? p.getImage() : "");
                items.add(im);
            }
        }
        List<Map<String,Object>> logs = new ArrayList<>();
        synchronized (logStore) {
            for (Map<String,Object> log : logStore) {
                if (Integer.valueOf(a.getId()).equals(log.get("afterSaleId"))) logs.add(new LinkedHashMap<>(log));
            }
        }
        m.put("items", items); m.put("logs", logs); m.put("confirmStatus", "商家暂未确认收到退货"); return m;
    }

    public Map<String,Object> saveExpress(int id, Integer userId, String expressNo) {
        Map<String,Object> r = new HashMap<>(); AfterSale a = mapper.selectById(id);
        if (a == null || !a.getUserId().equals(userId)) { r.put("code",404); r.put("msg","售后单不存在"); return r; }
        if (a.getStatus() != 1) { r.put("code",400); r.put("msg","当前状态不可填写退货物流"); return r; }
        a.setExpressNo(expressNo); a.setStatus(2); mapper.updateById(a);
        addLog(id, "填写物流", "user", userId, expressNo, "");
        r.put("code",200); r.put("msg","快递单号已提交"); return r;
    }
    public void delete(int id) { mapper.deleteById(id); }
    public void batchDelete(List<Integer> ids) { for (int id : ids) delete(id); }
    public void examine(int id, boolean approve, String reject, int adminId) { AfterSale a = mapper.selectById(id); if (a == null) return; a.setStatus(approve ? 1 : 5); if (!approve) a.setRejectContent(reject); mapper.updateById(a); }
    public void receiveGoods(int id, String imgList, int adminId) { AfterSale a = mapper.selectById(id); if (a != null) { a.setStatus(3); a.setReceiveImgList(imgList); mapper.updateById(a); } }
    public Map<String,Object> refund(int id, double amount, int adminId, String refundImgs, String refundDesc) {
        Map<String,Object> r = new HashMap<>(); AfterSale a = mapper.selectById(id); if (a == null || a.getStatus() != 3) { r.put("code",400); r.put("msg","状态异常"); return r; }
        a.setStatus(4);
        a.setRefundActual(new java.math.BigDecimal(amount));
        if (refundImgs != null) a.setRefundImgs(refundImgs);
        if (refundDesc != null) a.setRefundDesc(refundDesc);
        a.setFinishTime(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        mapper.updateById(a); r.put("code",200); r.put("msg","退款成功"); return r;
    }
    public Map<String,Object> adminList(String orderNo, String username, String expressNo, Integer status, int page, int pageSize) {
        LambdaQueryWrapper<AfterSale> q = new LambdaQueryWrapper<>();
        if (status != null) q.eq(AfterSale::getStatus, status);
        // 按用户名搜索：先查匹配的用户ID
        if (username != null && !username.trim().isEmpty()) {
            List<User> matchedUsers = userMapper.selectList(new LambdaQueryWrapper<User>().like(User::getUsername, username.trim()));
            if (!matchedUsers.isEmpty()) {
                List<Integer> userIds = new ArrayList<>();
                for (User u : matchedUsers) userIds.add(u.getId());
                q.in(AfterSale::getUserId, userIds);
            } else { q.eq(AfterSale::getUserId, -1); }
        }
        q.orderByDesc(AfterSale::getCreateTime);
        Page<AfterSale> p = mapper.selectPage(new Page<>(page, pageSize), q); Map<String,Object> r = new HashMap<>(); r.put("total", (int)p.getTotal());
        List<Map<String,Object>> list = new ArrayList<>();
        for (AfterSale a : p.getRecords()) {
            Map<String,Object> m = toMap(a);
            Order o = orderMapper.selectById(a.getOrderId());
            m.put("orderNo", o != null ? o.getOrderNo() : "");
            m.put("username", o != null ? o.getUsername() : "");
            list.add(m);
        }
        r.put("list", list); return r;
    }
    public Map<String,Object> stats() { Map<String,Object> s = new HashMap<>(); s.put("pending", 0); s.put("monthTotal", 0); s.put("finished", 0); return s; }
    public int autoAuditExpired() { return 0; }

    private Map<String,Object> toMap(AfterSale a) {
        Map<String,Object> m = new LinkedHashMap<>(); m.put("id", a.getId()); m.put("orderId", a.getOrderId()); m.put("userId", a.getUserId());
        m.put("bindItemIds", a.getBindItemIds()); m.put("productIds", a.getProductIds()); m.put("status", a.getStatus());
        m.put("returnReason", a.getReturnReason()); m.put("descText", a.getDescText()); m.put("imgList", a.getImgList());
        m.put("expressNo", a.getExpressNo()); m.put("rejectContent", a.getRejectContent());
        m.put("refundEstimate", a.getRefundEstimate() != null ? a.getRefundEstimate() : 0);
        m.put("refundAmount", a.getRefundAmount() != null ? a.getRefundAmount() : (a.getRefundEstimate() != null ? a.getRefundEstimate() : 0));
        m.put("createTime", a.getCreateTime()); m.put("finishTime", a.getFinishTime());
        return m;
    }
}
