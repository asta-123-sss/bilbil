package com.bilbil.storage;

import lombok.extern.slf4j.Slf4j;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 轻量 FastDFS 客户端（纯 Java Socket 实现，无第三方依赖）
 * <p>自研目的：官方 fastdfs-client-java 多为 SNAPSHOT，第三方 tobato 客户端对 Spring Boot 3
 * 兼容性差，故此处按 FastDFS 二进制协议手写最小可用客户端，覆盖上传/下载/删除三个核心操作。</p>
 *
 * <p>FastDFS 协议说明：请求/响应头固定 10 字节 = pkg_len(8字节大端) + cmd(1字节) + status(1字节)。</p>
 */
@Slf4j
public class FastDfsClient {

    // ---------------- 协议命令常量 ----------------
    /** tracker：查询可上传的 storage（不带 group） */
    private static final byte TRACKER_PROTO_CMD_SERVICE_QUERY_STORE_WITHOUT_GROUP_ONE = 101;
    /** tracker：查询文件所在 storage（下载/删除用，携带 group） */
    private static final byte TRACKER_PROTO_CMD_SERVICE_QUERY_FETCH_ONE = 102;
    /** storage：上传文件 */
    private static final byte STORAGE_PROTO_CMD_UPLOAD_FILE = 11;
    /** storage：删除文件 */
    private static final byte STORAGE_PROTO_CMD_DELETE_FILE = 12;
    /** storage：下载文件 */
    private static final byte STORAGE_PROTO_CMD_DOWNLOAD_FILE = 14;

    // ---------------- 协议长度常量 ----------------
    private static final int HEADER_LEN = 10;
    private static final int GROUP_NAME_LEN = 16;
    private static final int IP_ADDR_LEN = 16;
    private static final int EXT_NAME_LEN = 6;

    private final List<String> trackerServers;
    private final int connectTimeoutSeconds;
    private final int networkTimeoutSeconds;

    public FastDfsClient(String trackerServers, int connectTimeoutSeconds, int networkTimeoutSeconds) {
        this.trackerServers = new ArrayList<>(Arrays.asList(trackerServers.split(",")));
        this.connectTimeoutSeconds = connectTimeoutSeconds;
        this.networkTimeoutSeconds = networkTimeoutSeconds;
    }

    /**
     * 上传文件
     *
     * @param content 文件内容
     * @param ext     扩展名（不带点，最长 6 字符）
     * @return 文件ID，格式 group/remoteFileName
     */
    public String upload(byte[] content, String ext) {
        StorageServer storage = queryTracker(TRACKER_PROTO_CMD_SERVICE_QUERY_STORE_WITHOUT_GROUP_ONE, null);
        try (Socket socket = newSocket(storage.ip, storage.port)) {
            OutputStream out = socket.getOutputStream();

            // 请求体 = store_path_index(1) + file_size(8) + file_ext_name(6) + 文件内容
            byte[] extBytes = padBytes(ext, EXT_NAME_LEN);
            long bodyLen = 1 + 8 + EXT_NAME_LEN + content.length;
            out.write(buildHeader(bodyLen, STORAGE_PROTO_CMD_UPLOAD_FILE));
            out.write(storage.storePathIndex);
            writeLong(out, content.length);
            out.write(extBytes);
            out.write(content);
            out.flush();

            Response resp = readResponse(socket.getInputStream());
            checkStatus(resp, "上传文件失败");

            // 响应体 = group_name(16) + remote_file_name(剩余)
            String group = readFixedString(resp.body, 0, GROUP_NAME_LEN);
            String remoteFile = new String(resp.body, GROUP_NAME_LEN, resp.body.length - GROUP_NAME_LEN, StandardCharsets.UTF_8);
            return group + "/" + remoteFile;
        } catch (IOException e) {
            throw new IllegalStateException("FastDFS 上传失败", e);
        }
    }

    /**
     * 下载文件
     *
     * @param fileId 文件ID，格式 group/remoteFileName
     * @return 文件内容
     */
    public byte[] download(String fileId) {
        String[] parts = splitGroupName(fileId);
        StorageServer storage = queryTracker(TRACKER_PROTO_CMD_SERVICE_QUERY_FETCH_ONE, padBytes(parts[0], GROUP_NAME_LEN));
        try (Socket socket = newSocket(storage.ip, storage.port)) {
            OutputStream out = socket.getOutputStream();

            // 请求体 = file_offset(8) + download_bytes(8) + group_name(16) + remote_file_name
            byte[] nameBytes = parts[1].getBytes(StandardCharsets.UTF_8);
            long bodyLen = 8 + 8 + GROUP_NAME_LEN + nameBytes.length;
            out.write(buildHeader(bodyLen, STORAGE_PROTO_CMD_DOWNLOAD_FILE));
            writeLong(out, 0);                          // 偏移量
            writeLong(out, 0);                          // 0 表示下载整个文件
            out.write(padBytes(parts[0], GROUP_NAME_LEN));
            out.write(nameBytes);
            out.flush();

            Response resp = readResponse(socket.getInputStream());
            checkStatus(resp, "下载文件失败");
            return resp.body;
        } catch (IOException e) {
            throw new IllegalStateException("FastDFS 下载失败", e);
        }
    }

    /**
     * 删除文件
     *
     * @param fileId 文件ID，格式 group/remoteFileName
     */
    public void delete(String fileId) {
        String[] parts = splitGroupName(fileId);
        StorageServer storage = queryTracker(TRACKER_PROTO_CMD_SERVICE_QUERY_FETCH_ONE, padBytes(parts[0], GROUP_NAME_LEN));
        try (Socket socket = newSocket(storage.ip, storage.port)) {
            OutputStream out = socket.getOutputStream();

            // 请求体 = group_name(16) + remote_file_name
            byte[] nameBytes = parts[1].getBytes(StandardCharsets.UTF_8);
            long bodyLen = GROUP_NAME_LEN + nameBytes.length;
            out.write(buildHeader(bodyLen, STORAGE_PROTO_CMD_DELETE_FILE));
            out.write(padBytes(parts[0], GROUP_NAME_LEN));
            out.write(nameBytes);
            out.flush();

            Response resp = readResponse(socket.getInputStream());
            checkStatus(resp, "删除文件失败");
        } catch (IOException e) {
            throw new IllegalStateException("FastDFS 删除失败", e);
        }
    }

    // ------------------------------------------------------------------
    // 内部辅助
    // ------------------------------------------------------------------

    /** 查询 tracker 获取 storage 服务器 */
    private StorageServer queryTracker(byte cmd, byte[] body) {
        for (String server : trackerServers) {
            String[] hp = server.trim().split(":");
            if (hp.length != 2) {
                log.warn("非法 tracker 地址：{}", server);
                continue;
            }
            try (Socket socket = newSocket(hp[0].trim(), Integer.parseInt(hp[1].trim()))) {
                OutputStream out = socket.getOutputStream();
                byte[] bodyBytes = body == null ? new byte[0] : body;
                out.write(buildHeader(bodyBytes.length, cmd));
                out.write(bodyBytes);
                out.flush();

                Response resp = readResponse(socket.getInputStream());
                if (resp.status != 0) {
                    continue;
                }
                // 响应体 = group_name(16) + ip(16) + port(8) + store_path_index(1)
                String ip = readFixedString(resp.body, GROUP_NAME_LEN, IP_ADDR_LEN);
                long port = readLong(resp.body, GROUP_NAME_LEN + IP_ADDR_LEN);
                byte storePathIndex = resp.body[GROUP_NAME_LEN + IP_ADDR_LEN + 8];
                return new StorageServer(ip, (int) port, storePathIndex);
            } catch (Exception e) {
                log.warn("连接 tracker {} 失败：{}", server, e.getMessage());
            }
        }
        throw new IllegalStateException("无可用 tracker 服务器：" + trackerServers);
    }

    /** 读取 FastDFS 响应（10 字节头 + 响应体） */
    private Response readResponse(InputStream in) throws IOException {
        byte[] header = readFully(in, HEADER_LEN);
        long pkgLen = readLong(header, 0);
        byte cmd = header[8];
        byte status = header[9];
        byte[] body = new byte[(int) pkgLen];
        if (pkgLen > 0) {
            body = readFully(in, body.length);
        }
        return new Response(cmd, status, body);
    }

    /** 校验响应状态，非 0 抛异常 */
    private void checkStatus(Response resp, String op) {
        if (resp.status != 0) {
            throw new IllegalStateException(op + "，FastDFS 返回错误码：" + resp.status);
        }
    }

    /** 构造 10 字节请求头 */
    private byte[] buildHeader(long pkgLen, byte cmd) {
        byte[] header = new byte[HEADER_LEN];
        writeLong(header, 0, pkgLen);
        header[8] = cmd;
        header[9] = 0;
        return header;
    }

    /** 创建 socket（带连接/读写超时） */
    private Socket newSocket(String host, int port) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), connectTimeoutSeconds * 1000);
        socket.setSoTimeout(networkTimeoutSeconds * 1000);
        return socket;
    }

    /** 读取指定长度字节（阻塞直到读满） */
    private byte[] readFully(InputStream in, int len) throws IOException {
        byte[] buf = new byte[len];
        int read = 0;
        while (read < len) {
            int n = in.read(buf, read, len - read);
            if (n < 0) {
                throw new EOFException("连接意外关闭");
            }
            read += n;
        }
        return buf;
    }

    /** 字符串按固定长度补齐（不足补 0，超长截断） */
    private byte[] padBytes(String s, int len) {
        byte[] result = new byte[len];
        if (s != null) {
            byte[] src = s.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(src, 0, result, 0, Math.min(src.length, len));
        }
        return result;
    }

    /** 从字节数组读取定长字符串（去掉尾部 0 和空白） */
    private String readFixedString(byte[] data, int offset, int len) {
        int end = offset + len;
        while (end > offset && data[end - 1] == 0) {
            end--;
        }
        return new String(data, offset, end - offset, StandardCharsets.UTF_8).trim();
    }

    /** 大端写入 long（到流） */
    private void writeLong(OutputStream out, long value) throws IOException {
        byte[] buf = new byte[8];
        writeLong(buf, 0, value);
        out.write(buf);
    }

    /** 大端写入 long（到字节数组） */
    private void writeLong(byte[] buf, int offset, long value) {
        for (int i = 7; i >= 0; i--) {
            buf[offset + i] = (byte) (value & 0xFF);
            value >>= 8;
        }
    }

    /** 大端读取 long */
    private long readLong(byte[] buf, int offset) {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value = (value << 8) | (buf[offset + i] & 0xFF);
        }
        return value;
    }

    /** 拆分 fileId 为 group 和 remoteFileName */
    private String[] splitGroupName(String fileId) {
        int idx = fileId.indexOf('/');
        if (idx <= 0) {
            throw new IllegalArgumentException("非法文件ID（应为 group/name）：" + fileId);
        }
        return new String[]{fileId.substring(0, idx), fileId.substring(idx + 1)};
    }

    // ---------------- 内部数据结构 ----------------

    /** 响应体 */
    private record Response(byte cmd, byte status, byte[] body) {
    }

    /** storage 服务器信息 */
    private record StorageServer(String ip, int port, byte storePathIndex) {
    }
}
