package com.wuweibi.bullet.orders.controller;


import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import com.alipay.easysdk.kernel.util.ResponseChecker;
import com.alipay.easysdk.payment.common.models.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuweibi.bullet.annotation.JwtUser;
import com.wuweibi.bullet.business.OrderPayBiz;
import com.wuweibi.bullet.business.domain.OrderPayInfo;
import com.wuweibi.bullet.config.swagger.annotation.WebApi;
import com.wuweibi.bullet.domain.domain.session.Session;
import com.wuweibi.bullet.domain2.entity.Domain;
import com.wuweibi.bullet.domain2.enums.DomainStatusEnum;
import com.wuweibi.bullet.entity.api.R;
import com.wuweibi.bullet.exception.BaseException;
import com.wuweibi.bullet.exception.type.SystemErrorType;
import com.wuweibi.bullet.oauth2.utils.SecurityUtils;
import com.wuweibi.bullet.orders.domain.OrdersConfirmDTO;
import com.wuweibi.bullet.orders.domain.OrdersDTO;
import com.wuweibi.bullet.orders.domain.OrdersListVO;
import com.wuweibi.bullet.orders.domain.OrdersParam;
import com.wuweibi.bullet.orders.entity.Orders;
import com.wuweibi.bullet.orders.enums.OrdersStatusEnum;
import com.wuweibi.bullet.orders.enums.PayTypeEnum;
import com.wuweibi.bullet.orders.service.OrdersService;
import com.wuweibi.bullet.service.DomainService;
import com.wuweibi.bullet.utils.CodeHelper;
import com.wuweibi.bullet.utils.HttpUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * (Orders)表控制层
 *
 * @author makejava
 * @since 2022-07-21 16:16:05
 */
@Slf4j
@WebApi
@Api(tags = "订单管理")
@RestController
@RequestMapping("/api/orders")
public class OrdersAppController {
    /**
     * 服务对象
     */
    @Resource
    private OrdersService ordersService;

    @Resource
    private OrderPayBiz orderPayBiz;

    @Resource
    private DomainService domainService;

    /**
     * 分页查询订单
     *
     * @return
     */
    @GetMapping(value = "/list")
    public Object list(Page pageParams, OrdersParam params) {
        params.setUserId(SecurityUtils.getUserId());
        Page<OrdersListVO> page = ordersService.getListPage(pageParams, params);
        return R.success(page);
    }

    /**
     * 计算价格
     * @return
     */
    @ApiOperation("订单计算价格")
    @RequestMapping(value = "/calculate", method = RequestMethod.POST)
    public Object calculate( @RequestBody @Valid OrdersDTO ordersDTO) {
        Long userId = SecurityUtils.getUserId();
        ordersDTO.setUserId(userId);

        return orderPayBiz.calculate(ordersDTO);
    }

    @Resource
    private Config alipayConfig;


    /**
     * 下单接口
     *
     * @param session
     * @return
     */
    @ApiOperation("下单接口")
    @PostMapping(value = "/create")
    @Transactional
    public R createOrder(@JwtUser Session session, HttpServletRequest request, @RequestBody @Valid OrdersDTO ordersDTO) throws Exception {
        Long userId = session.getUserId();
        ordersDTO.setUserId(userId);
        // 校验数据

        // 计算价格
        R<OrderPayInfo> r = orderPayBiz.calculate(ordersDTO);
        if (r.isFail()) {
            return r;
        }
        OrderPayInfo orderPayInfo = r.getData();

        if (ordersDTO.getAmount().compareTo(orderPayInfo.getAmount()) != 0) {
            return R.fail("数量错误，下单失败！");
        }

        Orders orders = new Orders();

        // 订单号生成
        long id = CodeHelper.getIdWorker().nextId();
        orders.setOrderNo(String.valueOf(id));
        orders.setName(orderPayInfo.getName());
        orders.setAmount(orderPayInfo.getRealAmount()); // 单位 秒 GB
        orders.setPayType(ordersDTO.getPayType());
        orders.setUserId(userId);
        orders.setResourceType(ordersDTO.getResourceType());
        orders.setDomainId(ordersDTO.getResId());
        orders.setPayAmount(orderPayInfo.getPayAmount());
        orders.setDiscountAmount(orderPayInfo.getDiscountAmount());
        orders.setPriceAmount(orderPayInfo.getPriceAmount());
        orders.setStatus(OrdersStatusEnum.WAIT_PAY.getStatus());
        orders.setCreateTime(new Date());
        orders.setUpdateTime(orders.getCreateTime());
        orders.setRefundAmount(0l);
        orders.setRefundMoney(BigDecimal.ZERO);
        orders.setUserIP(HttpUtils.getRemoteIP(request));

        ordersService.save(orders);

        // 余额支付方式 扣钱
        if (orderPayInfo.getPayType() == PayTypeEnum.BALANCE.getType()) {
            R r2 = orderPayBiz.balancePay(userId, orders.getPayAmount(), orders.getOrderNo());
            if (r2.isFail()) {
                throw new BaseException(r2);
            }
        }
        // 套餐权益支付
        if (orderPayInfo.getPayType() == PayTypeEnum.VIP.getType()) {
            R r2 = orderPayBiz.packagePay(userId, orders.getPayAmount(), orders);
            if (r2.isFail()) {
                throw new BaseException(r2);
            }
        }

        switch (orderPayInfo.getResourceType()) {
            case 1: // 域名
            case 2: // 端口
                Domain domain = domainService.getById(orders.getDomainId());
                // 校验域名是否存在
                if (domain == null) {
                    return R.fail(SystemErrorType.DOMAIN_NOT_FOUND);
                }
                domainService.updateUserId(orders.getDomainId(), orders.getUserId(), DomainStatusEnum.SALE.getStatus());
                break;
            case 5: // 套餐

                break;
        }


        return R.success(orders.getId());
    }


    /**
     * 订单确认支付接口
     *
     * @param session
     * @return
     */
    @PostMapping(value = "/confirm")
    public R confirm(@JwtUser Session session, @RequestBody @Valid OrdersConfirmDTO dto) throws Exception {
        Long userId = session.getUserId();
        // 校验数据
        Orders orders;
        if (dto.getOrderId() != null) {
            orders = ordersService.getById(dto.getOrderId());
        } else {
            orders = ordersService.getByOrderNo(dto.getOrderNo());
        }

        if (orders == null) {
            return R.fail("订单不存在");
        }
        if (orders.getStatus() == OrdersStatusEnum.WAIT_PAY.getStatus()) {
            // 主动查询订单支付状态
            Factory.setOptions(alipayConfig);
            AlipayTradeQueryResponse result =
                    Factory.Payment.Common().query(orders.getOrderNo());

            if (ResponseChecker.success(result)) {
                // {"alipay_trade_query_response":{"code":"10000","msg":"Success","buyer_logon_id":"gln***@sandbox.com","buyer_pay_amount":"0.00","buyer_user_id":"2088622987384692","buyer_user_type":"PRIVATE","invoice_amount":"0.00","out_trade_no":"999809380105","point_amount":"0.00","receipt_amount":"0.00","send_pay_date":"2022-07-21 23:23:03","total_amount":"70.00","trade_no":"2022072122001484690502290245","trade_status":"TRADE_SUCCESS"},"sign":"OP2QmZfPi25jL9ShqZWvssnzTbptPUFSWXiM5SanfetE7uruZyJDW+rR8Jyw/71OWdDRoUVF9fmKJsTak9lu2UHcLg8rLBpYfJ1Qlex2lUgINHfN2X+dBGFMwQ+y3hk0SfnzY34wNAnbuaeGPPzgmbv7KE16Rjw3mzVICBJUiPO+M5g4dgXw+lbEdRK30wGEYi4gbcN4lGYqUrOAN5DJ8Sl7ovaQ9yce2oE/gTu/NPHElYihUK8Ln1M4KDdK/mtTgEH2tiwtV7QsMaYD9ydkx5rCw78KsPnZPAdVmMPbVfg28cZI0ialk0EAjgPM/Of9LXfAFpVn86/wdhiHtvz2eA=="}
                if ("TRADE_SUCCESS".equals(result.getTradeStatus())) {// 支付成功
                    Map<String, Object> params = new HashMap<>(3);
                    params.put("out_trade_no", result.getOutTradeNo());
                    params.put("trade_no", result.getTradeNo());
                    params.put("trade_status", result.getTradeStatus());

                    boolean payResult = orderPayBiz.aliPayNotify(params);
                    if (!payResult) {
                        return R.fail("还收到支付通知");
                    }
                }
            } else {
                log.error("调用失败，原因：" + result.getBody());
            }
            return R.fail("订单未查询到支付信息");
        }
        return R.success(orders);
    }


}

