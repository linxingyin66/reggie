package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.dto.OrdersDto;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.OrderDetail;
import com.itheima.reggie.entity.Orders;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.OrderDetailService;
import com.itheima.reggie.service.OrderService;
import com.itheima.reggie.service.ShoppingCartService;
import com.itheima.reggie.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单
 */
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private UserService userService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 用户下单
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
        log.info("订单数据：{}",orders);
        orderService.submit(orders);
        return R.success("下单成功");
    }

    //客户端展示最新订单详情
    @GetMapping("/userPage")
    public R<Page> userPage(int page, int pageSize) {
        //获取当前id
        Long userId = BaseContext.getCurrentId();
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>(page, pageSize);
        //条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //查询当前用户id订单数据
        queryWrapper.eq(userId != null, Orders::getUserId, userId);
        //按时间降序排序
        queryWrapper.orderByDesc(Orders::getOrderTime);
        orderService.page(pageInfo, queryWrapper);
        BeanUtils.copyProperties(pageInfo, ordersDtoPage, "records");
        List<OrdersDto> list = pageInfo.getRecords().stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(item, ordersDto);
            //获取orderId,然后根据这个id，去orderDetail表中查数据
            Long orderId = item.getId();
            LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrderDetail::getOrderId, orderId);
            List<OrderDetail> details = orderDetailService.list(wrapper);
            //之后set一下属性
            ordersDto.setOrderDetails(details);
            return ordersDto;
        }).collect(Collectors.toList());

        ordersDtoPage.setRecords(list);
        //日志输出看一下
//        log.info("list:{}", list);
        return R.success(ordersDtoPage);
    }


    //客户端再来一份最新订单
    @PostMapping("/again")
    public R<String> again(@RequestBody Map<String, String> map) {
        Long orderId = Long.valueOf(map.get("id"));
        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> details = orderDetailService.list(queryWrapper);
        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> shoppingCarts = details.stream().map((item) -> {
            ShoppingCart shoppingCart = new ShoppingCart();
//            BeanUtils.copyProperties(item, shoppingCart);
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());

        shoppingCartService.saveBatch(shoppingCarts);
        return R.success("喜欢吃就再来一单吖~");
    }

    //订单详情展示
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, Long number, String beginTime, String endTime) {
        //获取当前id
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>(page, pageSize);
        //条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //按时间降序排序
        queryWrapper.orderByDesc(Orders::getOrderTime);
        //订单号
        queryWrapper.eq(number != null, Orders::getId, number);
        //时间段，大于开始，小于结束
        queryWrapper.gt(!StringUtils.isEmpty(beginTime), Orders::getOrderTime, beginTime)
                .lt(!StringUtils.isEmpty(endTime), Orders::getOrderTime, endTime);
        orderService.page(pageInfo, queryWrapper);
        BeanUtils.copyProperties(pageInfo, ordersDtoPage, "records");
        List<OrdersDto> list = pageInfo.getRecords().stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(item,ordersDto);
            Long userId = item.getUserId();
            ordersDto.setUserName(userId+"");

            //获取orderId,然后根据这个id，去orderDetail表中查数据
//            Long orderId = item.getId();
//            LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
//            wrapper.eq(OrderDetail::getOrderId, orderId);
//            List<OrderDetail> details = orderDetailService.list(wrapper);
//            BeanUtils.copyProperties(item, ordersDto);
            //之后set一下属性
//            ordersDto.setOrderDetails(details);
            return ordersDto;
        }).collect(Collectors.toList());
        ordersDtoPage.setRecords(list);
        //日志输出看一下
//        log.info("list:{}", list);
        return R.success(ordersDtoPage);
//        return R.success(pageInfo);
    }

//    @PutMapping
//    public R<String> changeStatus(@RequestBody Map<String, String> map) {
//        int status = Integer.parseInt(map.get("status"));
//        Long orderId = Long.valueOf(map.get("id"));
//        log.info("修改订单状态:status={status},id={id}", status, orderId);
//        LambdaUpdateWrapper<Orders> updateWrapper = new LambdaUpdateWrapper<>();
//        updateWrapper.eq(Orders::getId, orderId);
//        updateWrapper.set(Orders::getStatus, status);
//        orderService.update(updateWrapper);
//        return R.success("订单状态修改成功");
//    }
    //状态修改
    @PutMapping
    public R<String> changeStatus(@RequestBody Orders order) {
        LambdaUpdateWrapper<Orders> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Orders::getId,order.getId());
        updateWrapper.set(Orders::getStatus,order.getStatus());
        orderService.update(updateWrapper);
        return  R.success("订单状态修改成功");
    }
}