package com.nbatch.job.handler.constant;

/**
 * job handler 常量
 */
public interface JobHandlerConstant {

    /**
     * 文件名替换字符-日期
     */
    String FILE_NAME_REPLACE_CHAR_PREFIX = "#{";

    /**
     * 文件名替换字符-节点id
     */
    String FILE_NAME_REPLACE_CHAR_SUFFIX = "}";

    /**
     * 文件名后缀-csv
     */
    String FILE_TYPE_SUFFIX_CSV = ".csv";

    /**
     * 文件名后缀-execute，主要用于文件在生成的过程当中的命名
     */
    String FILE_TYPE_SUFFIX_EXECUTE = ".execute";

    /**
     * 今天表后缀
     */
    String TODAY_TABLE_SUFFIX = "_today";

    /**
     * SQL 字段替换正则表达式
     */
    String SQL_FIELD_REPLACE_REGEX = "#\\{([^}]+)\\}";

    /**
     * SQL 字段占位符
     */
    String SQL_FIELD_REPLACE_CHAR = "?";

}
