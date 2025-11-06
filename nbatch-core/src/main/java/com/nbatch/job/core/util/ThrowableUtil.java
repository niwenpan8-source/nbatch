package com.nbatch.job.core.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author Mr.ni
 */
public class ThrowableUtil {

    /**
     * parse error to string
     *
     * @param e  error
     */
    public static String toString(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

}
