package com.bilbil.storage;

import cn.hutool.core.util.StrUtil;
import com.bilbil.config.BilbilProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * FastDFS 分布式文件存储实现
 * <p>当 application.yml 中 bilbil.storage.type=fastdfs 时生效。
 * 视频/封面/头像存储在 FastDFS，通过 Nginx 反向代理对外提供访问。</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "bilbil.storage", name = "type", havingValue = "fastdfs")
public class FastDfsFileStorageService implements FileStorageService {

    private final FastDfsClient client;
    private final BilbilProperties properties;

    public FastDfsFileStorageService(BilbilProperties properties) {
        this.properties = properties;
        BilbilProperties.FastDfs cfg = properties.getFastdfs();
        this.client = new FastDfsClient(cfg.getTrackerServers(), cfg.getConnectTimeout(), cfg.getNetworkTimeout());
    }

    @Override
    public String upload(byte[] bytes, String ext) {
        String fileId = client.upload(bytes, ext);
        log.info("FastDFS 上传成功：fileId={}, size={}", fileId, bytes.length);
        return fileId;
    }

    @Override
    public byte[] download(String fileId) {
        return client.download(fileId);
    }

    @Override
    public void delete(String fileId) {
        if (StrUtil.isBlank(fileId)) {
            return;
        }
        client.delete(fileId);
    }

    @Override
    public String getAccessUrl(String fileId) {
        if (StrUtil.isBlank(fileId)) {
            return null;
        }
        // 访问域名指向 Nginx，Nginx 将 /group1/... 反向代理到 FastDFS storage
        String domain = StrUtil.removeSuffix(properties.getStorage().getAccessDomain(), "/");
        return domain + "/" + fileId;
    }
}
