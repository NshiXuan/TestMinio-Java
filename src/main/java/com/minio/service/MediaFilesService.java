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

    /**
     * 上传分块
     *
     * @param fileMd5            原始文件的md5
     * @param chunkIndex         分块序号
     * @param localChunkFIlePath 分块文件本地路径
     * @return
     */
    Boolean uploadChunk(String fileMd5, int chunkIndex, String localChunkFIlePath);

    /**
     * 检查分块
     *
     * @param fileMd5    原始文件md5
     * @param chunkIndex 分块序号
     */
    Boolean checkChunk(String fileMd5, int chunkIndex);

    /**
     * 合并分块
     *
     * @param fileMd5             原始文件md5 用来获取分块目录
     * @param chunkTotal          分块总数
     * @param uploadFileParamsDto 文件参数
     * @return
     */
    Boolean mergeChunk(String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto);

    /**
     * 文件信息入库
     *
     * @param fileMd5             原始文件md5 用来做文件的id
     * @param uploadFileParamsDto 文件参数
     * @param bucket              存储的桶
     * @param filePath            文件存储路径
     * @return
     */
    public MediaFiles addFilesToDb(String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String filePath);
}
