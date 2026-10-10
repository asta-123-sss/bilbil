package com.bilbil.storage;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.bilbil.common.exception.BusinessException;
import com.bilbil.common.result.ResultCode;
import com.bilbil.config.BilbilProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.util.Date;

/**
 * 本地磁盘文件存储实现
 * <p>默认实现，开发环境无需额外部署 FastDFS。
 * 文件保存在 {local-path}/{yyyyMMdd}/{uuid}.{ext}，通过 /upload/** 静态资源映射访问。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bilbil.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final BilbilProperties properties;

    /** 存储根目录（绝对路径） */
    private String rootPath() {
        return Paths.get(properties.getStorage().getLocalPath()).toAbsolutePath().normalize().toString();
    }

    @Override
    public String upload(byte[] bytes, String ext) {
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR, "上传内容为空");
        }
        // 规范化扩展名：去除前导点，统一小写
        String suffix = StrUtil.isBlank(ext) ? "" : "." + ext.replace(".", "").toLowerCase();
        // 按日期分目录存储，避免单目录文件过多
        String dateDir = DateUtil.format(new Date(), "yyyyMMdd");
        // 相对文件ID：yyyyMMdd/uuid.ext（同时也是访问路径 /upload/ 后的部分）
        String fileId = dateDir + "/" + IdUtil.fastSimpleUUID() + suffix;

        File target = new File(rootPath(), fileId);
        FileUtil.writeBytes(bytes, target);
        log.info("本地存储上传成功：fileId={}, size={}", fileId, bytes.length);
        return fileId;
    }

    @Override
    public byte[] download(String fileId) {
        File file = new File(rootPath(), fileId);
        if (!file.exists()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在：" + fileId);
        }
        return FileUtil.readBytes(file);
    }

    @Override
    public void delete(String fileId) {
        if (StrUtil.isBlank(fileId)) {
            return;
        }
        boolean deleted = FileUtil.del(new File(rootPath(), fileId));
        log.info("本地存储删除：fileId={}, result={}", fileId, deleted);
    }

    @Override
    public String getAccessUrl(String fileId) {
        if (StrUtil.isBlank(fileId)) {
            return null;
        }
        // 本地开发直接走 /upload/** 静态资源；生产环境该域名指向 Nginx
        String domain = StrUtil.removeSuffix(properties.getStorage().getAccessDomain(), "/");
        return domain + "/upload/" + fileId;
    }
}
