package com.minio.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("media_files")
public class MediaFiles implements Serializable {
    /**
     * 主键
     */
    private String id;

    /**
     * 文件名称
     */
    private String filename;

    /**
     * 文件类型（文档，音频，视频）
     */
    private String fileType;

    /**
     * 存储路径
     */
    private String filePath;


    /**
     * 文件标识
     */
    private String fileId;

    /**
     * 媒资文件访问地址
     */
    private String url;

    /**
     * 文件大小
     */
    private Long fileSize;

}
