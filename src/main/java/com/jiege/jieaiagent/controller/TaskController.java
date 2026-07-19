package com.jiege.jieaiagent.controller;

import com.jiege.jieaiagent.model.User;
import com.jiege.jieaiagent.service.TaskOrchestratorService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/task")
public class TaskController {

    @Resource
    private TaskOrchestratorService taskOrchestratorService;

    @PostMapping(value = "/execute", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeTask(HttpServletRequest request, @RequestBody java.util.Map<String, String> body) {
        User user = (User) request.getAttribute("currentUser");
        String message = body.get("message");

        if (message == null || message.isBlank()) {
            SseEmitter err = new SseEmitter();
            err.completeWithError(new IllegalArgumentException("任务内容不能为空"));
            return err;
        }

        return taskOrchestratorService.execute(message);
    }
}
