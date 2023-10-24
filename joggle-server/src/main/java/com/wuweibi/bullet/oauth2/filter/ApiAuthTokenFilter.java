package com.wuweibi.bullet.oauth2.filter;

import cn.hutool.core.net.NetUtil;
import com.wuweibi.bullet.oauth2.consts.ClientScope;
import com.wuweibi.bullet.utils.SpringUtils;
import de.codecentric.boot.admin.server.config.AdminServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;


/**
 * 用于过滤Token整合后的交叉访问情况
 *
 * @author marker
 */
@Slf4j
public class ApiAuthTokenFilter implements Filter, InitializingBean {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2Authentication) {
            OAuth2Authentication auth2Authentication = (OAuth2Authentication) authentication;
            boolean isAdminToken = auth2Authentication.getOAuth2Request().getScope().contains(ClientScope.SCOPE_ADMIN);


            String uri = request.getRequestURI();
            if (uri.startsWith("/error")) { // 当请求出现404时转发到这个路径的
                return;
            }
            // 内部接口直接放开
            if (uri.startsWith("/inner")) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
            if (isAdminToken && uri.startsWith("/admin")) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
            if (isAdminToken && !uri.startsWith("/admin")) {
                responseUnAuth(authentication, response, uri);
                return;
            }
            if (!isAdminToken && uri.startsWith("/admin")) {
                responseUnAuth(authentication, response, uri);
                return;
            }

        }
        // 如果是匿名用户，且访问的monitor地址 ，校验session是否是有monitor标识
        if (authentication instanceof AnonymousAuthenticationToken) {
            AdminServerProperties adminServerProperties = SpringUtils.getBean(AdminServerProperties.class);
            if (request.getRequestURI().startsWith(adminServerProperties.getContextPath())) {
                // 静态资源放行
                if (request.getRequestURI().startsWith(adminServerProperties.path("/assets"))) {
                    filterChain.doFilter(servletRequest, servletResponse);
                    return;
                }
                if (request.getRequestURI().startsWith(adminServerProperties.path("/login"))) {// 登录地址放行
                    filterChain.doFilter(servletRequest, servletResponse);
                    return;
                }
                if (NetUtil.isInnerIP(request.getRemoteHost()) &&
                        request.getMethod().equalsIgnoreCase("POST") && request.getRequestURI().startsWith(adminServerProperties.path("/instances"))
                ) {
                    filterChain.doFilter(servletRequest, servletResponse);
                    return;
                }
                // 判断是内网ip才能访问
                if (NetUtil.isInnerIP(request.getRemoteHost()) && request.getRequestURI().startsWith(adminServerProperties.path("/actuator"))) {
                    filterChain.doFilter(servletRequest, servletResponse);
                    return;
                }

                HttpSession httpSession = request.getSession(false);
                if (httpSession != null && "true".equals(httpSession.getAttribute("monitor"))) {
                    filterChain.doFilter(servletRequest, servletResponse);
                    return;
                }else{
                    response.sendRedirect(adminServerProperties.path("/login"));
                    return;
                }
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }


    /**
     * 相应无权限
     *
     * @param authentication
     * @param response
     * @param uri
     * @throws IOException
     */
    private void responseUnAuth(Authentication authentication, HttpServletResponse response, String uri) throws IOException {
        log.warn("user:{} 无权限访问: {}", authentication.getName(), uri);
        response.setCharacterEncoding("utf-8");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().write("无权限访问");
    }

    @Override
    public void afterPropertiesSet() {

    }
}
