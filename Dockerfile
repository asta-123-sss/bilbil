# =====================================================================
# 仿B站视频平台 后端 Docker 镜像（直接运行本地打包好的 jar，服务器无需 Maven/Java 编译）
# 用法：把本地 target/bilbil-1.0.0.jar 上传到与 Dockerfile 同目录（名字保持 bilbil-1.0.0.jar）
# =====================================================================
FROM eclipse-temurin:17-jre
# 视频抽帧审核依赖 ffmpeg
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY bilbil-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
