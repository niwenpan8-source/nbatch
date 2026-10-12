package com.nbatch.job.core.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author Mr.ni
 */
public class ThrowableUtil {

    /**
     * 将异常转换为字符串
     *
     * @param e 异常对象
     */
    public static String toString(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

}
