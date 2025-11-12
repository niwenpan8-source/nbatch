package com.nbatch.job.admin.core.util;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.TemplateHashModel;
import lombok.extern.slf4j.Slf4j;

/**
 * ftl util
 *
 * @author Mr.ni
 */
@Slf4j
public class FtlUtil {

    private static final BeansWrapper WRAPPER = new BeansWrapperBuilder(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS).build();

    public static TemplateHashModel generateStaticModel(String packageName) {
        try {
            TemplateHashModel staticModels = WRAPPER.getStaticModels();
            return (TemplateHashModel) staticModels.get(packageName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

}
