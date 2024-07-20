package com.wuweibi.bullet.alias;



/**
 * 缓存块 别名
 * @author marker
 * @version 1.0
 */
public interface CacheBlock {
	
	
	/**
	 * 周期缓存30分钟
	 */
	String  CACHE_SERVICE_PERIOD = "ServicePeriodCache";

	/**
	 * 系统配置缓存
	 */
	String  CACHE_SYSTEM_CONFIG = "sysconfig";

	/**
	 * 设备详情缓存
	 */
	String  CACHE_DEVICE_DETAIL = "deviceDetail";
	String  CACHE_HOME_TREND_HOUR = "cacheHomeTrendHour";
	String  CACHE_HOME_TREND = "cacheHomeTrend";

	
	
}
