package com.review.controller;

import com.review.common.BusinessException;
import com.review.common.Result;
import com.review.dto.FileExistsResponse;
import com.review.dto.FileInfoResponse;
import com.review.dto.FileUploadResponse;
import com.review.service.FileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 文件管理接口。
 */
@Api(tags = "文件管理")
@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @ApiOperation("检查文件是否已存在")
    @GetMapping("/check-exists")
    public Result<FileExistsResponse> checkFileExists(@RequestParam("md5") String md5) {
        return Result.success(fileService.checkFileExists(md5));
    }

    @ApiOperation("上传文件（含秒传）")
    @PostMapping("/upload")
    public Result<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file,
                                                 @RequestParam("md5") String md5,
                                                 @RequestParam("versionId") Long versionId,
                                                 @RequestParam("fileName") String fileName) {
        return Result.success(fileService.uploadFile(file, md5, versionId, fileName));
    }

    @ApiOperation("查询版本文件列表")
    @GetMapping("/list/{versionId}")
    public Result<List<FileInfoResponse>> listFiles(@PathVariable Long versionId) {
        return Result.success(fileService.listFiles(versionId));
    }

    @ApiOperation("生成预览链接")
    @GetMapping("/preview/{fileId}")
    public Result<Map<String, String>> preview(@PathVariable Long fileId) {
        String url = fileService.generatePreviewUrl(fileId);
        return Result.success(Collections.singletonMap("previewUrl", url));
    }

    @ApiOperation("下载文件")
    @GetMapping("/download/{fileId}")
    public void download(@PathVariable Long fileId, HttpServletResponse response) {
        Map<String, Object> downloadInfo = fileService.downloadFile(fileId);
        try (InputStream stream = (InputStream) downloadInfo.get("stream");
             ServletOutputStream outputStream = response.getOutputStream()) {
            response.reset();
            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename*=UTF-8''" + downloadInfo.get("encodedName"));
            Assert.notNull(stream, "文件流不存在");
            byte[] buffer = new byte[8192];
            int len;
            while ((len = stream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
        } catch (Exception e) {
            throw new BusinessException("文件下载失败，请稍后重试");
        }
    }

    @ApiOperation("删除文件")
    @DeleteMapping("/{fileId}")
    public Result<Void> deleteFile(@PathVariable Long fileId) {
        fileService.deleteFile(fileId);
        return Result.success();
    }
}
