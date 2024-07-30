package com.wuweibi.bullet.system.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuweibi.bullet.alias.State;
import com.wuweibi.bullet.annotation.JwtUser;
import com.wuweibi.bullet.annotation.ResponseMessage;
import com.wuweibi.bullet.common.domain.PageParam;
import com.wuweibi.bullet.config.properties.JoggleProperties;
import com.wuweibi.bullet.config.swagger.annotation.AdminApi;
import com.wuweibi.bullet.conn.WebsocketPool;
import com.wuweibi.bullet.domain.domain.session.Session;
import com.wuweibi.bullet.domain.message.FormFieldMessage;
import com.wuweibi.bullet.domain.params.PasswordParam;
import com.wuweibi.bullet.entity.api.R;
import com.wuweibi.bullet.exception.type.AuthErrorType;
import com.wuweibi.bullet.exception.type.SystemErrorType;
import com.wuweibi.bullet.flow.entity.UserFlow;
import com.wuweibi.bullet.flow.service.UserFlowService;
import com.wuweibi.bullet.oauth2.service.AuthenticationService;
import com.wuweibi.bullet.res.entity.UserPackage;
import com.wuweibi.bullet.res.service.UserPackageService;
import com.wuweibi.bullet.service.UserForgetService;
import com.wuweibi.bullet.service.UserService;
import com.wuweibi.bullet.system.domain.UserPassForgetApplyDTO;
import com.wuweibi.bullet.system.domain.dto.UserAdminParam;
import com.wuweibi.bullet.system.domain.vo.UserAdminDetailVO;
import com.wuweibi.bullet.system.domain.vo.UserListVO;
import com.wuweibi.bullet.system.entity.User;
import com.wuweibi.bullet.utils.HttpUtils;
import com.wuweibi.bullet.utils.SpringUtils;
import com.wuweibi.bullet.utils.StringUtil;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.provider.token.ConsumerTokenServices;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;


/**
 * @author marker
 * @version 1.0
 */
@AdminApi
@Slf4j
@RestController
@RequestMapping("/admin/user")
public class UserAdminController {

    @Resource
    private UserService userService;

    @Resource
    private WebsocketPool websocketPool;

    @Resource
    private UserPackageService userPackageService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // 添加一个日期类型编辑器，也就是需要日期类型的时候，怎么把字符串转化为日期类型
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }

    @Resource
    private UserFlowService userFlowService;


    /**
     * 分页查询所有数据2
     *
     * @param page   分页对象
     * @param params 查询实体
     * @return 所有数据
     */
    @ApiOperation("用户分页查询")
    @GetMapping("/list")
    public R<Page<UserListVO>> getPageList(PageParam page, UserAdminParam params) {
        return R.ok(this.userService.getList(page.toMybatisPlusPage(), params));
    }

    /**
     * 用户详情
     */
    @GetMapping("/detail")
    public R<UserAdminDetailVO> userDetail(@RequestParam Long userId) {
        User user = this.userService.getById(userId);
        if (Objects.isNull(user)) {
            return R.fail("用户不存在");
        }
        UserPackage userPackage = this.userPackageService.getById(userId);
        if (Objects.isNull(userPackage)) {
            log.error("用户套餐数据不存在 userId={}", userId);
            return R.fail("用户套餐不存在");
        }
        UserAdminDetailVO userAdminDetailVO = new UserAdminDetailVO();
        BeanUtils.copyProperties(user, userAdminDetailVO);
        userAdminDetailVO.setEnabled(user.isEnabled() ? 1 : 0);
        userAdminDetailVO.setUserPackageName(userPackage.getName());
        return R.ok(userAdminDetailVO);
    }


    /**
     * 获取登录的用户信息
     */
    @RequestMapping(value = "/login/info", method = RequestMethod.GET)
    public R loginInfo(@JwtUser Session session) {

        if (session.isNotLogin()) {
            return R.fail(AuthErrorType.INVALID_LOGIN);
        }

        Long userId = session.getUserId();

        // 验证邮箱正确性
        User user = userService.getById(userId);
        UserFlow userFlow = userFlowService.getUserFlow(userId);
        user.setPassword(null);

        JSONObject result = (JSONObject) JSON.toJSON(user);

        result.put("connNums", websocketPool.count());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        result.put("loginTime", sdf.format(user.getLoginTime()));
        result.put("balance", StringUtil.roundHalfUp(user.getBalance()));
        result.put("userFlow", userFlow.getFlow() / 1024); //MB


        return R.success(result);
    }


    @Resource()
    ConsumerTokenServices consumerTokenServices;


    /**
     * 注销操作
     */
    @RequestMapping(value = "/loginout", method = RequestMethod.POST)
    public R loginout(HttpServletRequest request) {
        String authentication = request.getHeader(HttpHeaders.AUTHORIZATION);
        String tokenValue = StringUtils.substring(authentication, AuthenticationService.BEARER_BEGIN_INDEX);
        if (consumerTokenServices.revokeToken(tokenValue)) {
            return R.success();
        } else {
            return R.fail(AuthErrorType.INVALID_REQUEST);
        }
    }


    /**
     * 修改密码
     */
    @ResponseMessage
    @PostMapping(value = "/password")
    public R password(@Valid PasswordParam dto, @JwtUser Session session) {
        if (!session.isLogin()) {
            return R.fail(SystemErrorType.LOGIN_INVALID);
        }
        Long userId = session.getUserId();
        boolean status = userService.updatePassword(userId, dto);
        if (status) {
            return R.success();
        }
        return R.fail();
    }


    @Resource
    private JoggleProperties joggleProperties;

    @Resource
    private UserForgetService userForgetService;

    /**
     * 忘记密码
     *
     * @return
     */
    @ApiOperation(value = "重置密码", notes = "重置密码将收到修改密码的邮件")
    @PostMapping(value = "/password/reset")
    public R forget(@RequestBody UserPassForgetApplyDTO dto,
                    HttpServletRequest request) {
        String email = dto.getEmail();
        // 验证邮箱正确性
        if (email.indexOf("@") == -1 && !SpringUtils.emailFormat(email)) {// 邮箱格式不正确
            FormFieldMessage ffm = new FormFieldMessage();
            ffm.setField("email");
            ffm.setStatus(State.RegEmailError);
            return R.fail(ffm);
        }
        String ip = HttpUtils.getRemoteHost(request);
        String url = joggleProperties.getServerUrl();

        // 验证最近5分钟是否申请过
        boolean applyStatus = userForgetService.checkApply(email, 5 * 60);
        if (applyStatus) {
            return R.fail("最近申请过忘记密码，请查收等待一段时间再次操作");
        }
        userService.applyChangePass(email, url, ip);
        return R.ok();
    }

}
