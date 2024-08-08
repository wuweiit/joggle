package com.wuweibi.bullet.business.impl;


import cn.hutool.core.date.DateUtil;
import com.wuweibi.bullet.business.OrderPayBiz;
import com.wuweibi.bullet.business.domain.OrderPayInfo;
import com.wuweibi.bullet.device.entity.ServerTunnel;
import com.wuweibi.bullet.device.service.ServerTunnelService;
import com.wuweibi.bullet.domain2.domain.DomainDetail;
import com.wuweibi.bullet.domain2.entity.Domain;
import com.wuweibi.bullet.entity.api.R;
import com.wuweibi.bullet.exception.BaseException;
import com.wuweibi.bullet.exception.type.SystemErrorType;
import com.wuweibi.bullet.flow.service.UserFlowService;
import com.wuweibi.bullet.orders.domain.OrdersDTO;
import com.wuweibi.bullet.orders.entity.Orders;
import com.wuweibi.bullet.orders.enums.OrdersStatusEnum;
import com.wuweibi.bullet.orders.enums.PayTypeEnum;
import com.wuweibi.bullet.orders.enums.ResourceTypeEnum;
import com.wuweibi.bullet.orders.service.OrdersService;
import com.wuweibi.bullet.res.domain.UserPackageRightsDTO;
import com.wuweibi.bullet.res.entity.ResourcePackage;
import com.wuweibi.bullet.res.entity.UserPackage;
import com.wuweibi.bullet.res.manager.UserPackageLimitEnum;
import com.wuweibi.bullet.res.manager.UserPackageManager;
import com.wuweibi.bullet.res.service.ResourcePackageService;
import com.wuweibi.bullet.res.service.UserPackageRightsService;
import com.wuweibi.bullet.res.service.UserPackageService;
import com.wuweibi.bullet.service.DeviceMappingService;
import com.wuweibi.bullet.service.DomainService;
import com.wuweibi.bullet.service.UserService;
import com.wuweibi.bullet.utils.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 *
 * 订单支付的
 *
 * @author marker
 *
 */
@Slf4j
@Service
public class OrderPayBizImpl implements OrderPayBiz {

    // 服务器到期时间的偏移量  1天
    static final long SERVER_DIFF_TIME_MS = 24*60*60*1000;
    // 最大购买天数
    static final long MAX_BUY_DAYS = 360l;

    /** 域名管理 */
    @Resource
    private DomainService domainService;


    @Resource
    private UserService userService;

    /**
     * 服务对象
     */
    @Resource
    private OrdersService ordersService;

    @Resource
    private UserFlowService userFlowService;

    @Resource
    private ServerTunnelService serverTunnelService;


    @Override
    public R<OrderPayInfo> calculate(OrdersDTO ordersDTO) {
        Long resId = ordersDTO.getResId();
        Long amount  = ordersDTO.getAmount();
        Long userId  = ordersDTO.getUserId();
        Integer resourceType  = ordersDTO.getResourceType();


        if (amount.compareTo(0l) <= 0) {
            return R.fail("购买数量参数错误");
        }
        Date dueTime = null;

        OrderPayInfo orderPayInfo = new OrderPayInfo();
        orderPayInfo.setPayType(ordersDTO.getPayType());
        orderPayInfo.setResourceType(resourceType);
        switch (resourceType){
            case 1: // 端口
            case 2: // 域名
                if (ordersDTO.getPayType() != PayTypeEnum.VIP.getType()) { // 非VIP权益支付
                    if (amount.compareTo(MAX_BUY_DAYS) > 0) {
                        return R.fail("最大支持购买360天");
                    }
                }
                DomainDetail domain = domainService.getDetail(resId);
                // 校验域名是否存在
                if(domain == null){
                    return R.fail(SystemErrorType.DOMAIN_NOT_FOUND);
                }
                if(domain.getUserId() != null && !ordersDTO.getUserId().equals(domain.getUserId())){
                    return R.fail(SystemErrorType.DOMAIN_IS_OTHER_BIND);
                }

                // 校验服务器到期时间
                ServerTunnel serverTunnel = serverTunnelService.getById(domain.getServerTunnelId());
                if (serverTunnel == null) {
                    return R.fail("通道信息错误，请联系管理员");
                }
                orderPayInfo.setServerEndTime(serverTunnel.getServerEndTime());

                String text = "续费";
                Date nowTime = new Date();
                if (domain.getDueTime() == null) {
                    domain.setDueTime(nowTime);
                    text = "购买";
                }
                // 断档过的处理为购买，且时间标记为当前时间
                if (domain.getDueTime().compareTo(nowTime) < 0){
                    domain.setDueTime(nowTime);
                    text = "购买";
                }
                // 资源的购买 结束时间
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(domain.getDueTime());
                calendar.add(Calendar.DATE, amount.intValue());
                dueTime = calendar.getTime();
                if (ordersDTO.getPayType() == PayTypeEnum.VIP.getType()) { // VIP权益支付
                    UserPackage userPackage = userPackageService.getById(userId);
                    dueTime = userPackage.getEndTime();
                    Date startTime = domain.getDueTime()==null?new Date():domain.getDueTime();
                    long size = DateUtil.betweenDay(startTime, dueTime, true); // 计算两个时间的天数
                    amount = size == 0 ? 0 : new Long(size  -1);
                }
//                else {
//                    if (serverTunnel.getServerEndTime().getTime() - dueTime.getTime() < SERVER_DIFF_TIME_MS) {
//                        long size = DateUtil.betweenDay(domain.getDueTime(), serverTunnel.getServerEndTime(), true);
//                        dueTime = DateUtils.addDays(domain.getDueTime(), (int) size);
//                        amount = new Long(size);
////                    return R.fail("该通道服务器到期时间：\n" + DateFormatUtils.format(serverTunnel.getServerEndTime(), "yyyy-MM-dd HH:mm:ss"));
//                    }
//                }

                // 计算价格 单价 * 天数
                BigDecimal price = domain.getSalesPrice();
//                if (amount < 7) { // 低于7天的按原价8折
//                    price = domain.getOriginalPrice().multiply(BigDecimal.valueOf(0.35)).setScale(BigDecimal.ROUND_HALF_UP);
//                }
                BigDecimal payAmount = price.multiply(BigDecimal.valueOf(amount));
                BigDecimal originalAmount = domain.getOriginalPrice().multiply(BigDecimal.valueOf(amount));

                String name = ResourceTypeEnum.toName(resourceType);
                orderPayInfo.setName(String.format("%s%s:%s", text, name , domain.getDomainFull()));


                orderPayInfo.setDueTime(dueTime.getTime()); // 计算到期
                orderPayInfo.setPrice(price);
                orderPayInfo.setAmount(amount);
                orderPayInfo.setRealAmount(amount*24*60*60l);
                orderPayInfo.setDiscountAmount(originalAmount.subtract(payAmount));
                orderPayInfo.setPriceAmount(originalAmount);
                orderPayInfo.setPayAmount(payAmount);
                orderPayInfo.setResourceType(domain.getType());
                break;
            case 3:
                if (ordersDTO.getPayType() == PayTypeEnum.VIP.getType()){
                    return R.fail("支付方式不支持");
                }
                // 查询流量价格套餐 1.6元
                BigDecimal priceAmount = BigDecimal.valueOf(1.6);
                payAmount = priceAmount.multiply(BigDecimal.valueOf(amount));

                orderPayInfo.setName(String.format("购买流量:%dGB", amount));
                orderPayInfo.setPriceAmount(payAmount);
                orderPayInfo.setPayAmount(payAmount);
                orderPayInfo.setDiscountAmount(BigDecimal.ZERO);
                orderPayInfo.setAmount(amount);
                orderPayInfo.setRealAmount(amount);
                break;
            case 5: // 套餐

                if (amount.compareTo(12l) > 0) {
                    return R.fail("时间限制最多12个月");
                }
                if (amount.compareTo(0l) <= 0) {
                    return R.fail("购买时间错误");
                }
                if (orderPayInfo.getPayType() != PayTypeEnum.ALIPAY.getType()) {
                    return R.fail("仅支持支付宝购买");
                }
                ResourcePackage resourcePackage =  resourcePackageService.getById(resId);
                if (resourcePackage == null) {
                    return R.fail("套餐不存在");
                }
                if (resourcePackage.getStatus() != 1) {
                    return R.fail("套餐状态不允许购买");
                }
                UserPackage userPackage = userPackageService.getByUserId(ordersDTO.getUserId());
                if (userPackage != null && userPackage.getLevel() > resourcePackage.getLevel()) {
                    return R.fail("请重新选择套餐，原因不支持降级。");
                }
                if (resourcePackage.getId() != userPackage.getResourcePackageId() && userPackage.getLevel() != 0) {
                    return R.fail("暂不支持切换购买套餐,敬请期待！");
                }
                if (userPackage.getEndTime() == null) {
                    userPackage.setEndTime(new Date());
                }

                BigDecimal totalAmount = resourcePackage.getPrice().multiply(BigDecimal.valueOf(amount));
                orderPayInfo.setName(String.format("购买套餐:%s", resourcePackage.getName()));
                orderPayInfo.setPriceAmount(totalAmount);
                orderPayInfo.setPayAmount(totalAmount);
                orderPayInfo.setDiscountAmount(BigDecimal.ZERO);
                orderPayInfo.setAmount(amount);
                orderPayInfo.setRealAmount(amount);

                calendar = Calendar.getInstance();
                calendar.setTime(userPackage.getEndTime());
                calendar.add(Calendar.MONTH, amount.intValue());
                dueTime = calendar.getTime();

                orderPayInfo.setDueTime(dueTime.getTime());
                break;
        }
        return R.success(orderPayInfo);
    }


    @Resource
    private UserPackageService userPackageService;

    @Resource
    private ResourcePackageService resourcePackageService;

    /**
     * 支付宝 通知
     * @param params 回调参数
     * @return
     */
    @Transactional
    public boolean aliPayNotify(Map<String, Object> params) {

        // TODO 加锁，事务控制

        // 成功
        String outTradeNo = (String) params.get("out_trade_no");
        String tradeNo = (String) params.get("trade_no");

        Orders orders = ordersService.getByOrderNo(outTradeNo);
        if (orders == null) {
            return false;
        }

        orders.setTradeNo(tradeNo);
        orders.setPayTime(new Date());
        orders.setStatus(OrdersStatusEnum.PAYED.getStatus());
        orders.setUpdateTime(orders.getPayTime());

        ordersService.updateById(orders);

        // 发放资源；
        switch (orders.getResourceType()) {
            case 1: // 域名
            case 2: // 端口
                Domain domain = domainService.getById(orders.getDomainId());
                // 校验域名是否存在
                if (domain == null) {
                    throw new RuntimeException("校验域名是否存在");
                }
                if (domain.getDueTime() == null) {
                    domain.setDueTime(new Date());
                }
                domain.setBuyTime(new Date());

                // 计算到期
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(domain.getDueTime());
                calendar.add(Calendar.SECOND, orders.getAmount().intValue());
                Date packageEndTime = calendar.getTime();
                if (orders.getPayType() == PayTypeEnum.VIP.getType()) { // 如果是VIP支付
                    UserPackage userPackage = userPackageService.getById(orders.getUserId());
                    packageEndTime = userPackage.getEndTime();
                }

                domainService.updateDueTime(orders.getDomainId(), packageEndTime.getTime());

                break;
            case 3: // 流量
                long flow = orders.getAmount() * 1024 * 1024;
                userFlowService.updateFLow(orders.getUserId(), flow);
                break;

            case 5: // 套餐
                Integer packageId = orders.getDomainId().intValue();
                Integer amount =  orders.getAmount() .intValue();
                R<UserPackage> r2 = userPackageManager.openService(orders.getUserId(), packageId, amount);
                if (r2.isFail()){
                    log.error("userPackageManager.openService() {}", r2.getMsg());
                }
                UserPackage userPackage = r2.getData();
                Date endTime = userPackage.getEndTime();
                Integer bandwidth = userPackage.getBroadbandRate();

                // 批量更新用户的权益内资源结束时间 & 套餐网速
                domainService.updateUserDueTime(orders.getUserId(), bandwidth, endTime);
                break;
        }
        return true;
    }

    @Resource
    private DeviceMappingService deviceMappingService;

    @Resource
    private UserPackageManager userPackageManager;
    @Resource
    private UserPackageRightsService userPackageRightsService;



    @Override
    @Transactional
    public R balancePay(Long userId, BigDecimal payMoney, String orderNo) {
        if (payMoney == null) {
            return R.fail(SystemErrorType.PAY_MONEY_NOT_NULL);
        }

        // 扣减余额
        boolean status = userService.updateBalance(userId, payMoney.negate());
        if (status) {
            OrderPayBiz orderPayBiz = SpringUtils.getBean(OrderPayBiz.class);
            Map<String, Object> params = new HashMap<>(3);
            params.put("out_trade_no", orderNo);
            params.put("trade_no", "余额流水号");
            params.put("trade_status", "TRADE_SUCCESS");
            boolean payStatus = orderPayBiz.aliPayNotify(params);
            if (!payStatus) {
                throw new BaseException("产品发放失败!");
            }
            return R.success();
        } else {
            return R.fail(SystemErrorType.PAY_MONEY_BALANCE_NOT_ENOUGH);
        }
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public R packagePay(Long userId, BigDecimal payMoney, Orders orders) {
        if (payMoney == null){
            return R.fail(SystemErrorType.PAY_MONEY_NOT_NULL);
        }
        String orderNo = orders.getOrderNo();

        UserPackageLimitEnum enumObj = transToPackageEnum(orders.getResourceType());

        // 校验资源不在套餐权益内
        if (!userPackageRightsService.checkResource(orders.getResourceType(), orders.getDomainId())) {
            R r1 = userPackageManager.usePackageAdd(userId, enumObj, 1);
            if (r1.isFail()) {
                return r1;
            }
            // 权益使用增加
            UserPackageRightsDTO packageRightsDTO = new UserPackageRightsDTO();
            packageRightsDTO.setUserId(userId);
            packageRightsDTO.setResourceType(orders.getResourceType());
            packageRightsDTO.setResourceId(orders.getDomainId());
            packageRightsDTO.setStatus(1);
            userPackageRightsService.addPackageRights(packageRightsDTO);
        }
        OrderPayBiz orderPayBiz = SpringUtils.getBean(OrderPayBiz.class);
        Map<String, Object> params = new HashMap<>(3);
        params.put("out_trade_no", orderNo);
        params.put("trade_no", "权益流水号");
        params.put("trade_status", "TRADE_SUCCESS");
        boolean payStatus = orderPayBiz.aliPayNotify(params);
        if (!payStatus) {
            throw new BaseException("产品发放失败!");
        }
        return R.success();

    }

    private UserPackageLimitEnum transToPackageEnum(Integer resourceType) {
        switch (resourceType){
            case 1:
                return UserPackageLimitEnum.PortNum;
            case 2:
                return UserPackageLimitEnum.DomainNum;
        }
        return null;
    }
}
