package com.jiege.jieaiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.jiege.jieaiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FileOperationTool {

    private final String FILE_DIR = FileConstant.FILE_SAVE_DIR + "/file";

    @Tool(description = "读取文件内容")
    public String readFile(@ToolParam(description = "要读取的文件名") String fileName) {
        String filePath = resolvePath(fileName);
        if (filePath == null) return "错误：文件名不合法";
        try {
            return FileUtil.readUtf8String(filePath);
        } catch (Exception e) {
            return "读取文件失败：" + e.getMessage();
        }
    }

    @Tool(description = "将内容写入文件")
    public String writeFile(
            @ToolParam(description = "文件名") String fileName,
            @ToolParam(description = "要写入的内容") String content) {

        String filePath = resolvePath(fileName);
        if (filePath == null) return "错误：文件名不合法，不能包含 .. 或路径分隔符";

        try {
            FileUtil.mkdir(FILE_DIR);
            FileUtil.writeUtf8String(content, filePath);
            return "File written successfully to: " + filePath;
        } catch (Exception e) {
            return "写入文件失败：" + e.getMessage();
        }
    }

    private String resolvePath(String fileName) {
        if (fileName == null || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return null;
        }
        return FILE_DIR + "/" + fileName.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fff]", "_");
    }
}
