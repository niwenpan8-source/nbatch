package com.nbatch.job.handler.handler.impl;

import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.files.RunNodeFilePath;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ScriptHandlerIntegrationTest {

    private Path dataPath;

    @Before
    public void setUp() throws IOException {
        Path testDataPath = Files.createDirectories(Paths.get("target", "script-handler-test-data"));
        dataPath = Files.createTempDirectory(testDataPath, "nbatch-script-test-");
        RunNodeFilePath.initDataPath(dataPath.toString());
    }

    @After
    public void tearDown() throws IOException {
        if (dataPath != null) {
            try (Stream<Path> paths = Files.walk(dataPath)) {
                paths.sorted((left, right) -> right.compareTo(left))
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    @Test
    public void shouldRunNodeScriptWithMultilineUtf8Content() throws Exception {
        Assume.assumeTrue(isCommandAvailable("node", "--version"));
        ExecuteNodeParam param = scriptParam("nodejs", "console.log('第一行');\nconsole.log(process.argv[2]);\n", "参数值");

        new ScriptHandler().execute(param);

        Path script = findSingleScriptFile();
        assertEquals("console.log('第一行');\nconsole.log(process.argv[2]);\n",
                new String(Files.readAllBytes(script), StandardCharsets.UTF_8));
    }

    @Test
    public void shouldRunPythonScriptWithMultilineUtf8Content() throws Exception {
        Assume.assumeTrue(isCommandAvailable("python", "--version"));
        ExecuteNodeParam param = scriptParam("python", "import sys\nprint('第一行')\nprint(sys.argv[1])\n", "参数值");

        new ScriptHandler().execute(param);

        Path script = findSingleScriptFile();
        assertEquals("import sys\nprint('第一行')\nprint(sys.argv[1])\n",
                new String(Files.readAllBytes(script), StandardCharsets.UTF_8));
    }

    @Test
    public void shouldRunPowerShellScriptWithMultilineUtf8Content() throws Exception {
        Assume.assumeTrue(isCommandAvailable("powershell", "-?"));
        ExecuteNodeParam param = scriptParam("powershell", "Write-Output '第一行'\nWrite-Output $args[0]\n", "参数值");

        new ScriptHandler().execute(param);

        Path script = findSingleScriptFile();
        byte[] scriptBytes = Files.readAllBytes(script);
        assertEquals((byte) 0xEF, scriptBytes[0]);
        assertEquals((byte) 0xBB, scriptBytes[1]);
        assertEquals((byte) 0xBF, scriptBytes[2]);
        assertEquals("Write-Output '第一行'\nWrite-Output $args[0]\n",
                new String(scriptBytes, 3, scriptBytes.length - 3, StandardCharsets.UTF_8));
    }

    private ExecuteNodeParam scriptParam(String scriptType, String content, String argument) {
        return new ExecuteNodeParam()
                .setWorkId("script-test-work")
                .setNodeId("script-test-node-" + scriptType)
                .setNodeLogId("script-test-log-" + scriptType)
                .setNodeType("script")
                .setScriptType(scriptType)
                .setExecuteContent(content)
                .setExecuteContentParam(argument)
                .setUpdateTime(new Date());
    }

    private Path findSingleScriptFile() throws IOException {
        try (java.util.stream.Stream<Path> files = Files.list(new File(RunNodeFilePath.getScriptPath()).toPath())) {
            Path script = files.findFirst().orElseThrow(() -> new AssertionError("未生成脚本文件"));
            assertTrue(Files.size(script) > 0);
            return script;
        }
    }

    private boolean isCommandAvailable(String command, String argument) {
        try {
            Process process = new ProcessBuilder(command, argument).start();
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
            }
            return completed;
        } catch (IOException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
