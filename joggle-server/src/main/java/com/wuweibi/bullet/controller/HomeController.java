package com.wuweibi.bullet.controller;

import com.wuweibi.bullet.config.properties.JoggleProperties;
import com.wuweibi.bullet.conn.WebsocketPool;
import com.wuweibi.bullet.domain.vo.CountVO;
import com.wuweibi.bullet.entity.api.R;
import com.wuweibi.bullet.oauth2.manager.ResourceManager;
import com.wuweibi.bullet.service.CountService;
import com.wuweibi.bullet.system.client.domain.NgrokVersionVO;
import com.wuweibi.bullet.system.client.service.ClientVersionService;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;


/**
 * 主页控制器
 *
 *
 * @author marker
 * @version 1.0
 */
@Controller
public class HomeController {

	/**
	 * 接口资源管理器
	 */
	@Resource
	private ResourceManager resourceManager;

	@Resource
	private JoggleProperties joggleProperties;


	@Resource
	private ClientVersionService clientVersionService;


	@Resource
	private WebsocketPool websocketPool;

	/**
     * 初始化接口
     * @return
     */
	@GetMapping("/api/open/init")
	@ResponseBody
	public R init(){
		String websiteUrl = joggleProperties.getServerUrl();
		String website = websiteUrl.substring(websiteUrl.indexOf("://")+3);
		NgrokVersionVO ngrokVersionVO = clientVersionService.getMaxVersion();
        Map map = new HashMap(7);
        map.put("website", website);
        map.put("websiteUrl", websiteUrl);
        map.put("clientVersion", ngrokVersionVO.getClientVersion());
        map.put("serverVersion", ngrokVersionVO.getServerVersion());
        map.put("dockerClientVersion", "0.0.14");
        map.put("apkClientVersion", "1.3.3");
        map.put("websocketConn", websocketPool.getInfo());
		resourceManager.loadResource();
		return R.success(map);
	}


	@Resource
	private CountService countService;

	/**
	 * 统计数据接口
	 * @return
	 */
	@GetMapping("/api/open/statistics")
	@ResponseBody
	public R statistics(){
		CountVO countVO = countService.getCountInfo();
		return R.success(countVO);
	}



	/**
	 * OAuth登录
	 * @return
	 */
	@ApiOperation("OAuth登录")
	@PostMapping("/oauth/token1")
	public R<Boolean> login(@RequestHeader("Authorization") String authorization, LoginForm loginForm){
		return null;
	}


	@Data
	public static class LoginForm {
		@ApiModelProperty(value = "账号")
		private String username;

		@ApiModelProperty(value = "密码")
		private String password;

		@ApiModelProperty(value = "授权类型", example="password" )
		private String grant_type = "password";
	}

}
