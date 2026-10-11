#!/bin/bash
# 清理演示UP主（uid 4/5）的视频源文件（mp4），删库前执行
PASS="root123456"

docker exec bilbil-mysql mysql -uroot -p"$PASS" -N -e \
  "SELECT file_id FROM video WHERE user_id IN (4, 5) AND file_id IS NOT NULL" bilbil \
| while IFS= read -r f; do
    [ -z "$f" ] && continue
    echo "删除文件: $f"
    docker exec bilbil-app rm -f "/data/upload/$f"
  done

echo "文件清理完成"
