package com.nbatch.job.admin.core.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Jackson util
 * 1、obj need private and set/get；
 * 2、do not support inner class；
 * 
 * @author Mr.ni 2015-9-25 18:02:56
 */
@Slf4j
public class JacksonUtil {

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static ObjectMapper getInstance() {
        return OBJECT_MAPPER;
    }

    /**
     * bean、array、List、Map --> json
     * 
     * @param obj 源对象
     * @return json string
     */
    public static String writeValueAsString(Object obj) {
    	try {
			return getInstance().writeValueAsString(obj);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
        return null;
    }

    /**
     * string --> bean、Map、List(array)
     * 
     * @param jsonStr json string
     * @param clazz  class
     * @return obj
     */
    public static <T> T readValue(String jsonStr, Class<T> clazz) {
    	try {
			return getInstance().readValue(jsonStr, clazz);
		} catch (JsonParseException e) {
			log.error(e.getMessage(), e);
		} catch (JsonMappingException e) {
			log.error(e.getMessage(), e);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
    	return null;
    }

	/**
	 * string --> List<Bean>...
	 *
	 * @param jsonStr  json
	 * @param parametrized  List<Bean>
	 * @param parameterClasses  Bean.class
	 * @param <T>  List<Bean>
	 */
	public static <T> T readValue(String jsonStr, Class<?> parametrized, Class<?>... parameterClasses) {
		try {
			JavaType javaType = getInstance().getTypeFactory().constructParametricType(parametrized, parameterClasses);
			return getInstance().readValue(jsonStr, javaType);
		} catch (JsonParseException e) {
			log.error(e.getMessage(), e);
		} catch (JsonMappingException e) {
			log.error(e.getMessage(), e);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}
}
