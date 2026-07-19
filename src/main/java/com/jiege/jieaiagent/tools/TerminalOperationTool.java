package com.jiege.jieaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class TerminalOperationTool {

    private static final int TIMEOUT_SEC = 30;

    @Tool(description = "执行终端命令（限时30秒）")
    public String executeTerminalCommand(@ToolParam(description = "要执行的命令") String command) {
        if (command == null || command.isBlank()) return "错误：命令为空";

        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                output.append("\n[命令超时，已强制终止]");
            } else if (process.exitValue() != 0) {
                output.append("\n[退出码: ").append(process.exitValue()).append("]");
            }
        } catch (Exception e) {
            output.append("执行失败：").append(e.getMessage());
        }
        return output.toString().trim();
    }
}
