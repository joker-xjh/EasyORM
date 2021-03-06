package org.suntao.easyorm.proxy;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.suntao.easyorm.executor.Executor;
import org.suntao.easyorm.map.MapStatement;

public class MapperProxyBuilder {
	private static Logger logger = Logger.getLogger(MapperProxyBuilder.class
			.getName());
	/**
	 * 代理缓存
	 * <p>
	 * key为dao接口类Name <br>
	 * value为代理
	 */
	private static Map<String, Object> proxyCache = new HashMap<String, Object>();

	/**
	 * 获取代理对象
	 * <p>
	 * 通过代理对象,可以通过接口,调用相应的方法
	 * 
	 * @param <T>
	 * 
	 * @return 代理对象
	 */
	public static <T> Object getMapperProxy(Class<T> mapperClass,
			Executor executor, Map<String, MapStatement> mapStatments) {
		Object result = null;
		// 暂存代理对象
		Object tempProxy = proxyCache.get(mapperClass.getName());
		if (proxyCache.containsKey(mapperClass.getName())) {
			tempProxy = proxyCache.get(mapperClass);
		} else {
			logger.info(String.format("代理缓存中不存在%s的代理,动态创建并缓存",
					mapperClass.getName()));
			tempProxy = Proxy.newProxyInstance(mapperClass.getClassLoader(),
					new Class[] { mapperClass }, new MapperProxyHandler(
							executor, mapStatments, mapperClass));
			proxyCache.put(mapperClass.getName(), tempProxy);
			result = tempProxy;
		}
		return result;
	}

}
