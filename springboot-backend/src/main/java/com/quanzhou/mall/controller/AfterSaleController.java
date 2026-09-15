package com.quanzhou.mall.controller;

import com.quanzhou.mall.config.ApiResponse;
import com.quanzhou.mall.service.AfterSaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/afterSale")
public class AfterSaleController {
    @Autowired private AfterSaleService service;

    @PostMapping("/apply")
    public ApiResponse<?> apply(@RequestBody Map<String,Object> body, @RequestAttribute(name="userId",required=false) Integer userId){
        if(userId==null)return ApiResponse.error(401,"请先登录");
        Map<String,Object> r=service.apply(userId, toInt(body.get("orderId")),(String)body.get("bindItemIds"),
            (String)body.get("productIds"),(String)body.get("returnReason"),(String)body.get("descText"),(String)body.get("imgList"));
        int code=(int)r.get("code");
        return code==200?ApiResponse.ok(r.get("msg")):ApiResponse.error(code,(String)r.get("msg"));
    }

    @GetMapping("/userList")
    public ApiResponse<?> userList(@RequestAttribute(name="userId",required=false) Integer userId,
        @RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int pageSize,@RequestParam(defaultValue="")String keyword){
        if(userId==null)return ApiResponse.error(401,"请先登录");
        return ApiResponse.ok(service.userList(userId,page,pageSize,keyword));
    }

    @GetMapping("/userDetail")
    public ApiResponse<?> userDetail(@RequestParam(required=false) Integer id, @RequestParam(required=false) Integer orderId,
        @RequestAttribute(name="userId",required=false) Integer userId){
        if(userId==null)return ApiResponse.error(401,"请先登录");
        Map<String,Object> detail;
        if(orderId!=null&&orderId>0) detail=service.detailByOrderIdForUser(orderId,userId);
        else if(id!=null&&id>0) detail=service.detailForUser(id,userId);
        else return ApiResponse.error("缺少参数");
        return detail!=null?ApiResponse.ok(detail):ApiResponse.error(404,"售后单不存在");
    }

    @PostMapping("/cancel")
    public ApiResponse<?> cancel(@RequestBody Map<String,Object> body, @RequestAttribute(name="userId",required=false) Integer userId){
        if(userId==null)return ApiResponse.error(401,"请先登录");
        Map<String,Object> r=service.cancel(toInt(body.get("id")),userId);
        int code=(int)r.get("code");
        return code==200?ApiResponse.ok(r.get("msg")):ApiResponse.error(code,(String)r.get("msg"));
    }

    @PostMapping("/saveExpress")
    public ApiResponse<?> saveExpress(@RequestBody Map<String,Object> body,
        @RequestAttribute(name="userId",required=false) Integer userId){
        if(userId==null)return ApiResponse.error(401,"请先登录");
        String no=(String)body.get("expressNo");
        if(no==null||!no.matches("^[a-zA-Z0-9]+$"))return ApiResponse.error("快递单号仅允许数字和字母");
        Map<String,Object> r=service.saveExpress(toInt(body.get("id")),userId,no);
        int code=(int)r.get("code");
        return code==200?ApiResponse.ok(r.get("msg")):ApiResponse.error(code,(String)r.get("msg"));
    }

    private int toInt(Object o){if(o==null)return 0;if(o instanceof Number)return ((Number)o).intValue();return Integer.parseInt(o.toString());}
}
