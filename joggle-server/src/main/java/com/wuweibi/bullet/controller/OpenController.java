package com.wuweibi.bullet.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wuweibi.bullet.alias.State;
import com.wuweibi.bullet.annotation.JwtUser;
import com.wuweibi.bullet.common.exception.RException;
import com.wuweibi.bullet.config.properties.JoggleProperties;
import com.wuweibi.bullet.config.swagger.annotation.WebApi;
import com.wuweibi.bullet.conn.WebsocketPool;
import com.wuweibi.bullet.controller.validator.LoginParamValidator;
import com.wuweibi.bullet.controller.validator.RegisterValidator;
import com.wuweibi.bullet.dashboard.domain.DeviceDateItemVO;
import com.wuweibi.bullet.device.entity.Device;
import com.wuweibi.bullet.domain.domain.session.Session;
import com.wuweibi.bullet.domain.dto.ClientInfoDTO;
import com.wuweibi.bullet.domain.vo.ReleaseDetail;
import com.wuweibi.bullet.domain.vo.ReleaseInfo;
import com.wuweibi.bullet.domain2.entity.Domain;
import com.wuweibi.bullet.entity.api.R;
import com.wuweibi.bullet.exception.type.SystemErrorType;
import com.wuweibi.bullet.flow.entity.UserFlow;
import com.wuweibi.bullet.flow.service.UserFlowService;
import com.wuweibi.bullet.metrics.service.DataMetricsService;
import com.wuweibi.bullet.res.entity.UserPackage;
import com.wuweibi.bullet.res.manager.UserPackageManager;
import com.wuweibi.bullet.service.*;
import com.wuweibi.bullet.system.client.entity.ClientVersion;
import com.wuweibi.bullet.system.client.service.ClientVersionService;
import com.wuweibi.bullet.system.entity.User;
import com.wuweibi.bullet.utils.CodeHelper;
import com.wuweibi.bullet.utils.HttpUtils;
import com.wuweibi.bullet.utils.StringUtil;
import com.wuweibi.bullet.websocket.Bullet3Annotation;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * 开放接口
 *
 * @author marker
 * @version 1.0
 */
@WebApi
@Api(tags = "开放接口")
@RestController
@RequestMapping("/api/open")
public class OpenController {

    /**
     * 消息通知
     */
    @Resource
    private UserService userService;

    /**
     * 密码加密器
     */
    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private MailService mailService;

    @Resource
    private DomainService domainService;

    @Resource
    private DeviceService deviceService;
    @Resource
    private ClientVersionService clientVersionService;

    @Resource
    private JoggleProperties joggleProperties;


    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // 添加一个日期类型编辑器，也就是需要日期类型的时候，怎么把字符串转化为日期类型
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));

        binder.setValidator(new LoginParamValidator()); //添加一个spring自带的validator
    }


    @Resource
    private UserFlowService userFlowService;
    @Resource
    private UserPackageManager userPackageManager;

    /**
     * 注册账号
     *
     * @return
     */
    @PostMapping("/register")
    @Transactional
    public Object register(HttpServletRequest request, @ModelAttribute User user, Errors errors) {
        // 验证邮箱正确性
        new RegisterValidator().validate(user, errors);
        if (errors.hasErrors()) {
            return R.fail(errors);
        }

        String email = user.getEmail();
        String inviteCode = request.getParameter("inviteCode"); // 邀请码


        // 验证是否存在
        User u = userService.getByEmail(email);
        if (u != null) {
            errors.rejectValue("email", String.valueOf(State.RegEmailExist));
            return R.fail(errors);
        }
        // 用户名设置为邮箱
        user.setUsername(email);
        user.setEnabled(false); // 必须通过邮箱的邮件地址激活账号

        // 密码加密处理
        String password = user.getPassword();
        user.setPassword(passwordEncoder.encode(password));

        // 生成修改密码验证串,生命周期为30分钟.
        String code = CodeHelper.makeNewCode();
        user.setActivateCode(code);

        boolean status = userService.save(user);

        // userflow
        UserFlow userFlow = new UserFlow();
        userFlow.setUserId(user.getId());
        userFlow.setFlow(0L);// kb
        userFlow.setUpdatedTime(new Date());
        userFlowService.save(userFlow);

        // 开通普通用户永久服务
        R<UserPackage> r1 = userPackageManager.openService(user.getId(), 1, null);
        if (r1.isFail()) {
            throw new RException(r1);
        }
        UserPackage userPackage = r1.getData();


        if (status) {
            Long userId = user.getId();
            // 赋权用户端
            userService.newAuthRole(userId, "Consumer");

            // 注册成功后发送激活邮件
            Map<String, Object> params = new HashMap<>(2);

            String url = HttpUtils.getRequestURL(request);
            if (StringUtil.isNotBlank(joggleProperties.getServerUrl())) {
                url = joggleProperties.getServerUrl();
            }
            String activateUrl = url + "#/user/activate?code=" + code + "&ic=" + inviteCode;
            params.put("url", url);
            params.put("activateUrl", activateUrl);

            mailService.send(email, "Bullet账号激活", params, "register_mail.ftl");

            // 赠送域名
            Domain domain = new Domain();

            Calendar calendar = Calendar.getInstance();
            Date time = calendar.getTime();
            calendar.add(Calendar.DATE, 7); // 7天使用权
            Date dueTime = calendar.getTime();

            // 生成域名
            domain.setDomain(CodeHelper.makeNewCode());
            domain.setServerTunnelId(1);// 默认通道
            domain.setUserId(userId);
            domain.setCreateTime(time);
            domain.setBuyTime(time);
            domain.setDueTime(dueTime);
            domain.setOriginalPrice(BigDecimal.valueOf(1));
            domain.setSalesPrice(BigDecimal.valueOf(0.25));
            domain.setStatus(1);
            domain.setType(2);
            domain.setBandwidth(userPackage.getBroadbandRate()); // 宽带速率mbps

            domainService.save(domain);

        }

        return R.success();
    }

    /**
     * 激活用户
     */
    @PostMapping(value = "/user/activate" )
    public R activate(
            @RequestParam String code,// 激活码
            @RequestParam(required = false, name = "ic") String inviteCode, // 邀请码
            HttpServletRequest request) {
        return userService.activate(code, inviteCode);
    }


    /**
     * 设备秘钥校验
     * TODO 安全性升级
     *
     * @return
     */
    @Deprecated
    @ApiOperation("设备秘钥校验【服务端调用校验】")
    @PostMapping(value = "/device/secret" )
    public R devicesecret(@RequestParam String clientNo,
                          @RequestParam String secret,
                          HttpServletRequest request) {

        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("deviceId", clientNo);
        Device device = deviceService.getOne(queryWrapper);
        if (device == null) {
            return R.fail(SystemErrorType.DEVICE_NOT_EXIST);
        }

        if (secret.equals(device.getDeviceSecret())) {
            return R.success();
        }
        return R.fail(SystemErrorType.DEVICE_SECRET_ERROR);
    }


    /**
     * 检查客户端更新
     *
     * @return
     */
    @ApiOperation("客户端检查更新接口")
    @PostMapping(value = "/checkUpdate")
    public ReleaseDetail checkUpdate(@RequestBody ClientInfoDTO clientInfoDTO) {

        ClientVersion clientVersion = clientVersionService.getNewVersion(clientInfoDTO);

        ReleaseDetail releaseDetail = new ReleaseDetail();
        ReleaseInfo releaseInfo = new ReleaseInfo();
        releaseInfo.setDescription(clientVersion.getDescription());
        releaseInfo.setTitle(clientVersion.getTitle());
        releaseInfo.setVersion(clientVersion.getVersion());
        String createTime = DateFormatUtils.ISO_DATE_FORMAT.format(clientVersion.getUpdateTime());
        releaseInfo.setCreateDate(createTime);
        releaseDetail.setRelease(releaseInfo);

        releaseDetail.setDownload_url(clientVersion.getDownloadUrl());
        releaseDetail.setChecksum(clientVersion.getChecksum());
//        releaseDetail.setSignature(null);
        releaseDetail.setPatch_type(null);
        releaseDetail.setAvailable(true);
        return releaseDetail;
    }


    /**
     * 检查请求头
     *
     * @return
     */
    @Profile("dev")
    @PostMapping(value = "/package/release")
    public R packageRelease(HttpServletRequest request) {
        userPackageManager.expireFree();
        return R.success( );
    }

    @Profile("dev")
    @PostMapping(value = "/domain/release")
    public R domainRelease(HttpServletRequest request) {
        domainService.resourceDueTimeRelease();
        return R.success( );
    }

    @Profile("dev")
    @PostMapping(value = "/package/flow-reset")
    public R resetPackageFlow(HttpServletRequest request) {
        userPackageManager.resetPackageFlow();
        return R.success( );
    }
    @Profile("dev")
    @PostMapping(value = "/package/exp")
    public R userPackageExp(HttpServletRequest request) {
        userPackageManager.taskUserPackageExpirationReminder();
        return R.success( );
    }

    @Profile("dev")
    @PostMapping(value = "/test")
    public R checkUpdate(HttpServletRequest request) {
        Enumeration<String> e = request.getHeaderNames();
        HashMap<String, String> data = new HashMap<>();
        while (e.hasMoreElements()) {
            String headerName = e.nextElement();
            String val = request.getHeader(headerName);
            data.put(headerName, val);
        }
        return R.success(data);
    }

    @Resource
    private WebsocketPool websocketPool;


    @GetMapping(value = "/ws")
    public R ws(HttpServletRequest request) {
        HashMap<String, String> data = new HashMap<>();
        for(Map.Entry<String, Bullet3Annotation> k :websocketPool.clientConnections.entrySet()){
            data.put(k.getKey(), k.getValue().toString());
        }
        return R.success(data);
    }

    @Resource
    private DataMetricsService dataMetricsService;

    @Resource
    private CountService countService;


    /**
     * 近30日流量情况
     * @return
     */
    @ApiOperation("近30日流量情况")
    @GetMapping("/all/flow/trend")
    @ResponseBody
    public R<List<DeviceDateItemVO>> getFlowTrend(
            @JwtUser Session session){
        int day = 30;
        List<DeviceDateItemVO> list = countService.getAllFlowTrend(day);
        return R.success(list);
    }

    /**
     * 近24小时流量情况
     * @return
     */
    @ApiOperation("近30日流量情况")
    @GetMapping("/all/flow/trend/hour")
    public R<List<DeviceDateItemVO>> getHourFlowTrend(
            @JwtUser Session session){
        int hour = 24;
        List<DeviceDateItemVO> list = countService.getAllFlowTrendHour(hour);
        return R.success(list);
    }


    /**
     * 初始化数据
     *
     * @param request
     * @return
     */
//    @RequestMapping(value = "/update-count/init")
    public R updateCount(HttpServletRequest request) {

        String startTime = "2021-11-09";
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DATE, 8);
        calendar.set(Calendar.MONTH, 10);
        calendar.set(Calendar.YEAR, 2021);

        while (true) {
            Date time = calendar.getTime();
            boolean status = dataMetricsService.generateDayByTime(time);
            calendar.add(Calendar.DATE, 1);
            if (DateUtils.truncatedCompareTo(time, new Date(), Calendar.DATE) == 0) {
                break;
            }
        }

        return R.success();
    }


}
