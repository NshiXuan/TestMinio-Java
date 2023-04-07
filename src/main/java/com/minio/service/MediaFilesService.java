package com.minio.service;

import com.minio.model.dto.UploadFileParamsDto;
import com.minio.model.po.MediaFiles;

import java.util.List;

public interface MediaFilesService {

    /**
     * 获取全部文件信息
     */
    List<MediaFiles> getFiles();

    /**
     * 上传文件
     *
     * @param uploadFileParamsDto 文件信息
     * @param localFilePath       本地文件路径
     */
    MediaFiles uploadFile(UploadFileParamsDto uploadFileParamsDto, String localFilePath);
}
