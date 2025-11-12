package com.nbatch.job.core.log;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.LogResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * store trigger log in each log-file
 * @author Mr.ni
 */
@Slf4j
public class JobFileAppender {

	/**
	 * log base path
	 * strut like:
	 * 	---/
	 * 	---/gluesource/
	 * 	---/gluesource/10_1514171108000.js
	 * 	---/gluesource/10_1514171108000.js
	 * 	---/2017-12-25/
	 * 	---/2017-12-25/639.log
	 * 	---/2017-12-25/821.log
	 *
	 */
	@Getter
	private static String logBasePath = "/data/applogs/job/jobhandler";

	@Getter
	private static String glueSrcPath = logBasePath.concat("/gluesource");
	public static void initLogPath(String logPath){
		logPath = System.getProperty("user.dir") + logPath;
		// init
		if (StrUtil.isNotBlank(logPath)) {
			logBasePath = logPath;
		}
		// mk base dir
		File logPathDir = new File(logBasePath);
		if (!logPathDir.exists()) {
			FileUtil.mkdir(logPathDir);
		}
		logBasePath = logPathDir.getPath();

		// mk glue dir
		File glueBaseDir = new File(logPathDir, "gluesource");
		if (!glueBaseDir.exists()) {
			FileUtil.mkdir(glueBaseDir);
		}
		glueSrcPath = glueBaseDir.getPath();
	}

	public static String getLogPath() {
		return logBasePath;
	}


	/**
	 * log filename, like "logPath/yyyy-MM-dd/9999.log"
	 *
	 * @param triggerDate 任务调度时间
	 * @param logId 日志ID
	 */
	public static String makeLogFileName(Date triggerDate, String logId) {

		// filePath/yyyy-MM-dd
		// avoid concurrent problem, can not be static
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		File logFilePath = new File(getLogPath(), sdf.format(triggerDate));
		if (!logFilePath.exists()) {
			FileUtil.mkdir(logFilePath);
		}

		// filePath/yyyy-MM-dd/9999.log
        return logFilePath.getPath()
                .concat(File.separator)
                .concat(logId)
                .concat(".log");
	}

	/**
	 * append log
	 *
	 * @param logFileName log file name
	 * @param appendLog log content
	 */
	public static void appendLog(String logFileName, String appendLog) {

		// log file
		if (StrUtil.isBlank(logFileName)) {
			return;
		}
		File logFile = new File(logFileName);

		if (!FileUtil.exist(logFile)) {
            FileUtil.touch(logFile);
        }

		// log
		if (appendLog == null) {
			appendLog = "";
		}
		appendLog += "\r\n";
		
		// append file content
        try (FileOutputStream fos = new FileOutputStream(logFile, true)) {
            fos.write(appendLog.getBytes(StandardCharsets.UTF_8));
            fos.flush();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
		
	}

	/**
	 * support read log-file
	 *
	 * @param logFileName log-file name
	 * @return log content
	 */
	public static LogResult readLog(String logFileName, int fromLineNum){

		// valid log file
		if (StrUtil.isBlank(logFileName)) {
            return new LogResult(fromLineNum, 0, "readLog fail, logFile not found", true);
		}
		File logFile = new File(logFileName);

		if (!logFile.exists()) {
            return new LogResult(fromLineNum, 0, "readLog fail, logFile not exists", true);
		}

		// read file
		StringBuilder logContentBuffer = new StringBuilder();
		int toLineNum = 0;
        try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(Files.newInputStream(logFile.toPath()), StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {
                // [from, to], start as 1
                toLineNum = reader.getLineNumber();
                if (toLineNum >= fromLineNum) {
                    logContentBuffer.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

		// result
        return new LogResult(fromLineNum, toLineNum, logContentBuffer.toString(), false);

	}

	/**
	 * read log data
	 * @param logFile log file
	 * @return log line content
	 */
	public static String readLines(File logFile){
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(logFile.toPath()), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
		return null;
	}

}
