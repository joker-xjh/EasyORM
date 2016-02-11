package org.suntao.easyorm.scan;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.suntao.easyorm.annotation.Param;
import org.suntao.easyorm.annotation.SQL;
import org.suntao.easyorm.map.MapStatment;
import org.suntao.easyorm.map.ResultMapConfig;
import org.suntao.easyorm.map.ResultMappingType;
import org.suntao.easyorm.xmlparse.EasyormConfig;

/**
 * 简化扫描器
 * <p>
 * 只扫描所在接口明确定义的方法,不扫描父类的方法
 * 
 * @author suntao
 *
 */
public class SimpleScanner implements Scanner {

	private List<Class<?>> daoClasses;
	private String daoPath;
	private EasyormConfig easyormConfig;
	private Map<String, MapStatment> scannedMapStatment;
	private Map<String, ResultMapConfig<?>> scannedResultMap;

	public SimpleScanner() {
		super();
	}

	public SimpleScanner(EasyormConfig easyormConfig) {
		super();
		this.easyormConfig = easyormConfig;
		this.daoPath = easyormConfig.getDaoPath();
		scanDaoClasses();
	}

	public SimpleScanner(List<Class<?>> daoClasses) {
		super();
		this.daoClasses = daoClasses;
	}

	public List<Class<?>> getDaoClasses() {
		return daoClasses;
	}

	public String getDaoPath() {
		return daoPath;
	}

	@Override
	public Map<String, ResultMapConfig<?>> getScanedResultMap() {
		return scannedResultMap;
	}

	@Override
	public Map<String, MapStatment> getScannedMapStatment() {
		return scannedMapStatment;
	}

	@Override
	public Boolean scan() {
		Boolean result = false;
		Map<String, MapStatment> scanedMapStatment = new HashMap<String, MapStatment>();
		Map<String, ResultMapConfig<?>> scanedResultMap = new HashMap<String, ResultMapConfig<?>>();
		try {
			for (Class<?> currentClass : daoClasses) {
				for (Method currentMethod : currentClass.getDeclaredMethods()) {
					String id = null;
					id = String.format("%s.%s", currentClass.getName(),
							currentMethod.getName());
					ResultMapConfig<?> currentScanningResultMap = scanResultMapConfigOfMethod(currentMethod);
					MapStatment currentScanningMapStatment = scanMapStatmentOfMethod(currentMethod);
					currentScanningMapStatment
							.setResultMap(currentScanningResultMap);
					currentScanningMapStatment.setId(id);
					currentScanningResultMap.setResultMapID(id);
					scanedMapStatment.put(id, currentScanningMapStatment);
					scanedResultMap.put(id, currentScanningResultMap);
				}
			}
			this.scannedResultMap = scanedResultMap;
			this.scannedMapStatment = scanedMapStatment;
			result = true;
		} catch (Exception e) {
			result = false;
		}
		return result;
	}

	/**
	 * 根据daopath寻找DAO的各个class并存储在daoclasses中
	 */
	public void scanDaoClasses() {
		// TO DO
	}

	public List<MapStatment> scanMapStatment() {
		List<MapStatment> result = new ArrayList<MapStatment>();
		for (Class<?> currentClass : daoClasses) {
			for (Method currentMethod : currentClass.getDeclaredMethods()) {
				MapStatment mapStatment;
				String id = null;
				mapStatment = scanMapStatmentOfMethod(currentMethod);
				id = String.format("%s.%s", currentClass.getName(),
						currentMethod.getName());
				mapStatment.setId(id);
				result.add(mapStatment);
			}
		}
		return result;
	}

	public MapStatment scanMapStatmentOfMethod(Method method) {
		MapStatment result = new MapStatment();
		SQL sql;
		String statmentSQL = null;
		sql = method.getAnnotation(SQL.class);
		if (sql != null && !sql.value().isEmpty())
			statmentSQL = sql.value();
		Parameter[] parameters = method.getParameters();
		Map<String, Integer> paramLocation = new HashMap<String, Integer>();
		if (parameters != null && parameters.length > 0) {
			for (int i = 0; i < parameters.length; i++) {
				Parameter currentParameter = parameters[i];
				Integer currentIndexOfParameter = i;
				Param paramAnno = currentParameter.getAnnotation(Param.class);
				String paramName = null;
				if (paramAnno != null && !paramAnno.paramname().isEmpty()) {
					paramName = paramAnno.paramname();
				} else {
					paramName = currentParameter.getName();
				}
				paramLocation.put(paramName, currentIndexOfParameter);
			}

		}
		result.setStatmentSQL(statmentSQL);
		result.setParamLocation(paramLocation);
		return result;
	}

	@SuppressWarnings("rawtypes")
	public List<ResultMapConfig> scanResultMap() {
		List<ResultMapConfig> result = new ArrayList<ResultMapConfig>();
		// 遍历所有daoClass
		for (Class<?> daoClass : daoClasses) {

			Method[] methods = daoClass.getDeclaredMethods();
			// 遍历daoClass的每一个方法
			for (Method m : methods) {
				ResultMapConfig resultMap = scanResultMapConfigOfMethod(m);
				resultMap.setResultMapID(String.format("%s.%s",
						daoClass.getName(), m.getName()));
				result.add(resultMap);
			}
			// TO DO

		}

		return result;
	}

	/**
	 * 针对你方法的扫描ResultMapConfig
	 * 
	 * @param method
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public ResultMapConfig scanResultMapConfigOfMethod(Method method) {
		ResultMapConfig resultMap = new ResultMapConfig();
		/**
		 * DAO方法返回类型
		 * <p>
		 * 返回的是List,Boolean还是其他
		 */
		Class<?> returnClass = method.getReturnType();
		/**
		 * 返回实体类型
		 * <p>
		 * 自定义的实体类<br>
		 * 返回列表和单个实体需要使用
		 */
		Class<?> modelClass = null;
		String resultMapId = null;// 此resultMap的ID
		ResultMappingType resultMappingType = null;
		/**
		 * 按返回类型分别处理
		 * <p>
		 * 这部分code是不完备的<br>
		 * 重构的时候需要仔细测试
		 */
		if (returnClass.equals(Integer.class) || returnClass.equals(int.class)) {
			resultMappingType = ResultMappingType.INTEGER;
		} else if (returnClass.equals(Boolean.class)
				|| returnClass.equals(boolean.class)) {
			resultMappingType = ResultMappingType.BOOLEAN;
		} else if (returnClass.equals(List.class)) {
			// 强制转换以获取泛型参数
			ParameterizedType parameterizedType = (ParameterizedType) method
					.getGenericReturnType();
			Type[] types = parameterizedType.getActualTypeArguments();
			if (types.length != 1) {

				// 如果List的参数过多或者过少= =,这应该不会发生
				resultMappingType = ResultMappingType.OTHER;
			}
			try {
				modelClass = Class.forName(types[0].getTypeName());
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			resultMappingType = ResultMappingType.MODELLIST;
		} else if (returnClass.equals(Void.class)) {
			// log4j提示不应有返回为空的值
		} else if (!returnClass.isPrimitive()) {
			modelClass = method.getReturnType();
			resultMappingType = ResultMappingType.ONEMODEL;
		} else {
			// log4j需要进行提示,无法进行处理
			resultMappingType = ResultMappingType.OTHER;
		}
		resultMap.setResultMapID(resultMapId);
		resultMap.setModelClass(modelClass);
		resultMap.setResultType(resultMappingType);
		return resultMap;
	}

	public void setDaoClasses(List<Class<?>> daoClasses) {
		this.daoClasses = daoClasses;
	}

	public void setDaoPath(String daoPath) {
		this.daoPath = daoPath;
	}

	public void setEasyormConfig(EasyormConfig easyormConfig) {
		this.easyormConfig = easyormConfig;
	}

}