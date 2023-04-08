package com.minio.controller;

import com.minio.model.dto.R;
import com.minio.model.dto.UploadFileParamsDto;
import com.minio.model.po.MediaFiles;
import com.minio.service.MediaFilesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
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
    public R<List<MediaFiles>> getFiles() {
        List<MediaFiles> files = mediaFilesService.getFiles();
        return R.success(files);
    }

    /**
     * 上传文件
     *
     * @param file
     * @return
     * @throws Exception
     */
    @PostMapping("/upload/files")
    // @RequestPart("file") 注解表示将 HTTP 请求中的一个文件作为请求参数传递给方法，并绑定到对应的方法参数上。
    public R<MediaFiles> uplaodFile(@RequestPart("file") MultipartFile file) throws Exception {
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

    /**
     * 上传分块
     *
     * @param fileMd5    原始文件的md5
     * @param chunk      分块文件
     * @param chunkIndex 分块序号
     * @return
     * @throws Exception
     */
    @PostMapping("/upload/uploadchunk")
    public R<Boolean> uploadChunk(@RequestParam("fileMd5") String fileMd5, @RequestParam("chunk") MultipartFile chunk, @RequestParam("chunkIndex") int chunkIndex) throws Exception {
        // 1.创建临时文件获取文件路径
        File tempFile = File.createTempFile("minio", "temp");
        chunk.transferTo(tempFile);
        // 取出文件的绝对路径
        String localFilePath = tempFile.getAbsolutePath();


        // 3.调用service上传分块
        Boolean result = mediaFilesService.uploadChunk(fileMd5, chunkIndex, localFilePath);
        return R.success(result);
    }

    /**
     * 检查分块
     *
     * @param fileMd5    原始文件的md5
     * @param chunkIndex 分块序号
     * @return
     */
    @PostMapping("/upload/checkchunk")
    public R<Boolean> checkChunk(@RequestParam("fileMd5") String fileMd5, @RequestParam("chunkIndex") int chunkIndex) {
        Boolean result = mediaFilesService.checkChunk(fileMd5, chunkIndex);
        return R.success(result);
    }

    @PostMapping("/upload/mergechunks")
    public R<Boolean> mergeChunk(@RequestParam("fileMd5") String fileMd5, @RequestParam("fileName") String fileName, @RequestParam("chunkTotal") int chunkTotal) throws Exception {
        // 1.映射文件参数信息
        UploadFileParamsDto uploadFileParamsDto = new UploadFileParamsDto();
        uploadFileParamsDto.setFilename(fileName);
        uploadFileParamsDto.setFileType("视频");

        Boolean res = mediaFilesService.mergeChunk(fileMd5, chunkTotal, uploadFileParamsDto);
        return R.success(res);
    }
}
