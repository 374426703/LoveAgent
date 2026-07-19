package com.jiege.jieaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;

public class TerminateTool {

    @Tool(description = "所有任务完成后调用此工具结束工作")
    public String doTerminate() {
        return "任务结束";
    }
}
