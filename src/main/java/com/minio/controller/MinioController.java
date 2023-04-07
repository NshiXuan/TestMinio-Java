package com.minio.controller;

import com.minio.model.dto.R;
import com.minio.model.dto.UploadFileParamsDto;
import com.minio.model.po.MediaFiles;
import com.minio.service.MediaFilesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@RestController
public class MinioController {
    @Autowired
    MediaFilesService mediaFilesService;

    /**
     * 获取全部文件
     */
    @GetMapping("/files")
    public R getFiles() {
        List<MediaFiles> files = mediaFilesService.getFiles();
        return R.success(files);
    }

    @PostMapping("/upload/files")
    // @RequestPart("file") 注解表示将 HTTP 请求中的一个文件作为请求参数传递给方法，并绑定到对应的方法参数上。
    public R uplaodFile(@RequestPart("file") MultipartFile file) throws Exception {
        // 1.映射dto
        UploadFileParamsDto uploadFileParamsDto = new UploadFileParamsDto();
        // 文件名称
        uploadFileParamsDto.setFilename(file.getOriginalFilename());
        // 文件大小
        uploadFileParamsDto.setFileSize(file.getSize());
        // 文件类型
        uploadFileParamsDto.setFileType("图片");

        // 2.创建一个临时文件 以获取文件路径
        File tempFile = File.createTempFile("minio", "temp");
        file.transferTo(tempFile);
        // 取出文件的绝对路径
        String absolutePath = tempFile.getAbsolutePath();

        // 3.调用service
        MediaFiles mediaFiles = mediaFilesService.uploadFile(uploadFileParamsDto, absolutePath);

        return R.success(mediaFiles);
    }
}
