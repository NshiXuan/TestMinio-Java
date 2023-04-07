package com.minio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minio.exception.Code;
import com.minio.exception.MyException;
import com.minio.mapper.MediaFilesMapper;
import com.minio.model.dto.UploadFileParamsDto;
import com.minio.model.po.MediaFiles;
import com.minio.service.MediaFilesService;
import com.minio.utils.Utils;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;

@Slf4j
@Service
public class MediaFilesServiceImpl implements MediaFilesService {
    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MinioClient minioClient;

    //存储普通文件
    @Value("${minio.bucket.files}")
    private String bucket_files;

    //存储视频
    @Value("${minio.bucket.video}")
    private String bucket_video;

    @Override
    public List<MediaFiles> getFiles() {
        LambdaQueryWrapper<MediaFiles> wrapper = new LambdaQueryWrapper<>();
        return mediaFilesMapper.selectList(wrapper);
    }

    /**
     * 上传文件
     *
     * @param uploadFileParamsDto 文件信息
     * @param localFilePath       本地文件路径
     */
    @Override
    public MediaFiles uploadFile(UploadFileParamsDto uploadFileParamsDto, String localFilePath) {
        // 1.获取文件名
        String filename = uploadFileParamsDto.getFilename();

        // 2.获取扩展名
        // lastIndexOf() 方法查找字符串中最后一个出现指定字符的位置，并返回该位置到字符串结尾的子字符串
        String extension = filename.substring(filename.lastIndexOf("."));

        // 3.通过扩展名拿到mimeType
        String mimeType = Utils.getMimeType(extension);

        // 4.配置存储文件在minio的目录 年/月/日
        String formatTime = Utils.getFormatTime();

        // 5.获取文件的md5值作为存储在minio的文件名称
        String fileMd5 = Utils.getFileMd5(new File(localFilePath));

        // 6.配置文件存储路径 目录(年/月/日)+名称+后缀名
        String filePath = formatTime + fileMd5 + extension;

        // 7.上传到minio
        boolean result = uploadFilesToMinIO(localFilePath, mimeType, bucket_files, filePath);
        if (!result) {
            MyException.cast(Code.UPLOAD_ERROR);
        }

        // 8.将数据保存到数据库
        MediaFiles mediaFiles = addFilesToDb(fileMd5, uploadFileParamsDto, bucket_files, filePath);
        if (mediaFiles == null) {
            MyException.cast(Code.SERVICE_BUSY);
        }

        return mediaFiles;
    }

    /**
     * 将文件上传到minio
     *
     * @param localFilePath 文件本地路径
     * @param mimeType      媒体类型
     * @param bucket        桶
     * @param filePath      文件路径
     * @return
     */
    public boolean uploadFilesToMinIO(String localFilePath, String mimeType, String bucket, String filePath) {
        try {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket(bucket)//桶
                    .filename(localFilePath) //指定本地文件路径
                    .object(filePath)// 文件名 放在子目录下
                    .contentType(mimeType)//设置媒体文件类型
                    .build();

            //上传文件
            minioClient.uploadObject(uploadObjectArgs);
            log.debug("上传文件到minio成功,bucket:{},objectName:{},错误信息:{}", bucket, filePath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件出错,bucket:{},objectName:{},错误信息:{}", bucket, filePath, e.getMessage());
        }
        return false;
    }

    /**
     * 添加文件到数据库
     *
     * @param fileMd5             文件md5值
     * @param uploadFileParamsDto 上传文件的信息
     * @param bucket              桶
     * @param filePath            文件路径
     */
    @Transactional
    public MediaFiles addFilesToDb(String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String filePath) {
        //将文件信息保存到数据库
        LambdaQueryWrapper<MediaFiles> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MediaFiles::getFileId, fileMd5);

        MediaFiles mediaFiles = mediaFilesMapper.selectOne(wrapper);
        if (mediaFiles == null) {
            mediaFiles = new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            // 文件路径
            mediaFiles.setFilePath(filePath);
            // 文件id
            mediaFiles.setFileId(fileMd5);
            // 文件地址
            mediaFiles.setUrl("http://192.168.31.32:9000" + "/" + bucket + "/" + filePath);
            //插入数据库
            mediaFiles.setId(null);

            int insert = mediaFilesMapper.insert(mediaFiles);
            if (insert <= 0) {
                log.debug("向数据库保存文件失败,bucket:{},objectName:{}", bucket, filePath);
                return null;
            }
            return mediaFiles;
        }
        return mediaFiles;
    }
}
