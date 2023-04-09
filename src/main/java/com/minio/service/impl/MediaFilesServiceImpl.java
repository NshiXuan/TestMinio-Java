package com.minio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minio.exception.Code;
import com.minio.exception.MyException;
import com.minio.mapper.MediaFilesMapper;
import com.minio.model.dto.UploadFileParamsDto;
import com.minio.model.po.MediaFiles;
import com.minio.service.MediaFilesService;
import com.minio.utils.Utils;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FilterInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class MediaFilesServiceImpl implements MediaFilesService {
    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MinioClient minioClient;

    // 这里会形成依赖循环注入
//    @Resource
//    MediaFilesService currentProxy;

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
     * 上传分块
     *
     * @param fileMd5            原始文件的md5
     * @param chunkIndex         分块序号
     * @param localChunkFIlePath 分块文件本地路径
     * @return
     */
    @Override
    public Boolean uploadChunk(String fileMd5, int chunkIndex, String localChunkFIlePath) {
        // 1.根据md5获取文件存储路径 MD5的前面两个字符作为目录 如 a/b/abcxxxxxxxx/chunk/i
        String chunkFilePath = getChunkFilePath(fileMd5) + chunkIndex;

        // 2.获取mimeType
        String mimeType = Utils.getMimeType(null);

        // 3.将分块文件上传到minio
        boolean b = uploadFilesToMinIO(localChunkFIlePath, mimeType, bucket_video, chunkFilePath);
        if (!b) {
            MyException.cast(Code.UPLOAD_ERROR);
            return false;
        }

        return true;
    }

    /**
     * 检查分块
     *
     * @param fileMd5    原始文件md5
     * @param chunkIndex 分块序号
     * @return
     */
    @Override
    public Boolean checkChunk(String fileMd5, int chunkIndex) {
        // 1.获取minio存储分块的目录路径
        String chunkFilePath = getChunkFilePath(fileMd5);

        // 2.获取文件参数信息
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucket_video)
                .object(chunkFilePath + chunkIndex)
                .build();

        // 3.检查minio中分块是否存在
        try {
            FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
            if (inputStream != null) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 合并分块
     *
     * @param fileMd5             原始文件md5 用来获取分块目录
     * @param chunkTotal          分块总数
     * @param uploadFileParamsDto 文件参数
     * @return
     */
    @Override
    public Boolean mergeChunk(String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
        // 1.获取全部分块文件的信息
        List<ComposeSource> sources = new ArrayList<>();
        // 获取分块目录
        String chunkFilePath = getChunkFilePath(fileMd5);
        for (int i = 0; i < chunkTotal; i++) {
            // 指定分块文件的信息
            ComposeSource composeSource = ComposeSource.builder()
                    .bucket(bucket_video)
                    .object(chunkFilePath + i)
                    .build();
            sources.add(composeSource);
        }

        // 2.合并分块
        // 2.1 获取源文件名称作为
        String filename = uploadFileParamsDto.getFilename();
        // 2.2 获取扩展名
        String extension = filename.substring(filename.lastIndexOf("."));
        // 2.3 获取合并后的文件路径
        String objectName = fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + extension;

        // 2.4 定义合并参数
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket(bucket_video)
                .object(objectName) // 合并后文件名称
                .sources(sources) // 指定源文件
                .build();

        // 2.5 合并文件 minio默认的分块大小为5M
        try {
            minioClient.composeObject(composeObjectArgs);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("合并文件出错，bucket:{},mergeName:{},错误信息:{}", bucket_video, objectName, e.getMessage());
            MyException.cast(Code.SERVICE_BUSY);
            return false;
        }

        // todo: 3.校验合并后的源文件是否一致，视频上传才成功 如何校验

        // 4.将文件信息入库
        MediaFiles mediaFiles = addFilesToDb(fileMd5, uploadFileParamsDto, bucket_video, objectName);
        if (mediaFiles == null) {
            log.error("文件信息插入数据库失败 mergeChunk failed");
            MyException.cast(Code.SERVICE_BUSY);
            return false;
        }

        // 5.清理分块文件
        clearChunkFiles(chunkFilePath, chunkTotal);

        return true;
    }

    /**
     * 清除分块文件
     *
     * @param chunkFilePath 分块文件路径
     * @param chunkTotal    分块文件总数
     */
    private void clearChunkFiles(String chunkFilePath, int chunkTotal) {
        Iterable<DeleteObject> objects = Stream.iterate(0, i -> ++i).limit(chunkTotal).map(i -> new DeleteObject(chunkFilePath + i)).collect(Collectors.toList());

        RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket(bucket_video).objects(objects).build();
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
        //要想真正删除
        results.forEach(f -> {
            try {
                DeleteError deleteError = f.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 得到分块文件的目录
     *
     * @param fileMd5 文件md5
     * @return
     */
    private String getChunkFilePath(String fileMd5) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";
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
     * @param filePath            文件存储路径
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
