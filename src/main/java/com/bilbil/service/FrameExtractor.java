package com.bilbil.service;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 视频抽帧：调用系统 ffmpeg 从视频中抽取若干帧 JPEG 字节，供视觉审核使用。
 * <p>ffmpeg 不存在或抽取失败时返回空列表，调用方据此跳过「视频帧」这一环审核。</p>
 */
@Slf4j
@Component
public class FrameExtractor {

    /**
     * 从视频字节里抽取至多 count 帧（约每秒一帧后均匀取样）
     */
    public List<byte[]> extract(byte[] videoBytes, int count) {
        List<byte[]> frames = new ArrayList<>();
        if (videoBytes == null || videoBytes.length == 0 || count <= 0) {
            return frames;
        }
        File tmpDir = null;
        try {
            tmpDir = Files.createTempDirectory("bilbil-frames").toFile();
            File videoFile = new File(tmpDir, "src.mp4");
            FileUtil.writeBytes(videoBytes, videoFile);

            String outPattern = new File(tmpDir, "f_%03d.jpg").getAbsolutePath();
            Process process = new ProcessBuilder(
                    "ffmpeg", "-y", "-i", videoFile.getAbsolutePath(),
                    "-vf", "fps=1", outPattern)
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.warn("ffmpeg 抽帧超时，跳过视频帧审核");
                return frames;
            }

            File[] files = tmpDir.listFiles(f -> f.getName().startsWith("f_") && f.getName().endsWith(".jpg"));
            if (files == null || files.length == 0) {
                return frames;
            }
            Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));

            // 均匀取样 count 帧（视频很短时有多少取多少）
            int total = files.length;
            int n = Math.min(count, total);
            for (int i = 0; i < n; i++) {
                int idx = total * i / n;
                frames.add(FileUtil.readBytes(files[idx]));
            }
        } catch (Exception e) {
            log.warn("ffmpeg 抽帧失败（可能未安装 ffmpeg），跳过视频帧审核: {}", e.getMessage());
        } finally {
            if (tmpDir != null) {
                FileUtil.del(tmpDir);
            }
        }
        return frames;
    }
}
