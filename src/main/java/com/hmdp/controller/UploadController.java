package com.hmdp.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.Result;
import com.hmdp.utils.SystemConstants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * <p>
 * 图片上传相关接口
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("upload")
@Api(tags = "07-文件上传模块")
public class UploadController {

    @PostMapping("blog")
    @ApiOperation(value = "上传笔记图片", notes = "用 form-data 方式传，参数名必须是 file。返回的是服务器上的相对路径，发布笔记时把这个路径填进 images 字段。")
    public Result uploadImage(
            @ApiParam(value = "图片文件，参数名填 file", required = true)
            @RequestParam("file") MultipartFile image) {
        try {
            String originalFilename = image.getOriginalFilename();
            String fileName = createNewFileName(originalFilename);
            image.transferTo(new File(SystemConstants.IMAGE_UPLOAD_DIR, fileName));
            log.debug("文件上传成功，{}", fileName);
            return Result.ok(fileName);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @GetMapping("/blog/delete")
    @ApiOperation(value = "删除笔记图片", notes = "参数 name 传上传接口返回的那个文件名。注意用的是 GET 请求，不是 DELETE。")
    public Result deleteBlogImg(
            @ApiParam(value = "文件名，即上传接口返回的值", required = true)
            @RequestParam("name") String filename) {
        File file = new File(SystemConstants.IMAGE_UPLOAD_DIR, filename);
        if (file.isDirectory()) {
            return Result.fail("错误的文件名称");
        }
        FileUtil.del(file);
        return Result.ok();
    }

    private String createNewFileName(String originalFilename) {
        String suffix = StrUtil.subAfter(originalFilename, ".", true);
        String name = UUID.randomUUID().toString();
        int hash = name.hashCode();
        int d1 = hash & 0xF;
        int d2 = (hash >> 4) & 0xF;
        File dir = new File(SystemConstants.IMAGE_UPLOAD_DIR, StrUtil.format("/blogs/{}/{}", d1, d2));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return StrUtil.format("/blogs/{}/{}/{}.{}", d1, d2, name, suffix);
    }
}
