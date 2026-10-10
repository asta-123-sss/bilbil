# -*- coding: utf-8 -*-
# 把最新前端产物(dist)打进现有 Spring Boot fat jar 的 BOOT-INF/classes/static/，
# 无需重新跑 maven（后端 Java 代码没变，只有前端静态资源变了）。
# 用法：python patch_jar_static.py
import os
import sys
import zipfile

JAR = r"E:/SpringBoot-bilbil/target/bilbil-1.0.0.jar"
DIST = r"E:/SpringBoot-bilbil/bilbil-web/dist"
PREFIX = "BOOT-INF/classes/static/"
OUT = JAR + ".new"

if not os.path.exists(JAR):
    sys.exit("找不到 jar: " + JAR)
if not os.path.exists(DIST):
    sys.exit("找不到 dist: " + DIST)

removed = 0
added = 0

with zipfile.ZipFile(JAR, "r") as src, zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as dst:
    for item in src.infolist():
        if item.filename.startswith(PREFIX):
            removed += 1
            continue
        # 保留其它条目的原始属性/压缩方式
        dst.writestr(item, src.read(item.filename))

    for root, _dirs, files in os.walk(DIST):
        for f in files:
            full = os.path.join(root, f)
            rel = os.path.relpath(full, DIST).replace("\\", "/")
            dst.write(full, PREFIX + rel)
            added += 1

os.replace(OUT, JAR)
print("完成：移除旧静态条目 %d 个，写入新静态条目 %d 个" % (removed, added))
