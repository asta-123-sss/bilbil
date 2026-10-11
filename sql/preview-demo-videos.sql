-- 预览：演示UP主账号（uid 4/5）及其视频（只读）
SELECT id, username, nickname FROM user WHERE id IN (4, 5);

SELECT id, title, status, user_id, create_time
FROM video
WHERE user_id IN (4, 5)
ORDER BY id;
