package com.bilbil.storage;

/**
 * 文件存储抽象接口
 * <p>屏蔽底层存储实现（本地磁盘 / FastDFS），业务层只依赖该接口。
 * 通过 application.yml 中 bilbil.storage.type 切换实现：
 * - local   : 本地磁盘存储（默认，开发环境开箱即用）
 * - fastdfs : FastDFS 分布式存储（生产环境）</p>
 */
public interface FileStorageService {

    /**
     * 上传文件
     *
     * @param bytes 文件字节
     * @param ext   文件扩展名（如 mp4、jpg、png）
     * @return 存储后的文件ID（FastDFS 返回 group/name，本地返回相对路径）
     */
    String upload(byte[] bytes, String ext);

    /**
     * 下载文件字节
     *
     * @param fileId 文件ID
     * @return 文件字节
     */
    byte[] download(String fileId);

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     */
    void delete(String fileId);

    /**
     * 获取文件可访问 URL（视频/图片通过 Nginx 或本地静态资源访问）
     *
     * @param fileId 文件ID
     * @return 完整访问地址
     */
    String getAccessUrl(String fileId);
}
