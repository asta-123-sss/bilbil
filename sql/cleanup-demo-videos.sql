-- 彻底删除演示UP主账号（uid 4/5）的全部视频及关联数据（不删账号本身）
DELETE FROM video_like    WHERE video_id IN (SELECT id FROM video WHERE user_id IN (4, 5));
DELETE FROM video_collect WHERE video_id IN (SELECT id FROM video WHERE user_id IN (4, 5));
DELETE FROM video_coin    WHERE video_id IN (SELECT id FROM video WHERE user_id IN (4, 5));
DELETE FROM comment       WHERE video_id IN (SELECT id FROM video WHERE user_id IN (4, 5));
DELETE FROM danmaku       WHERE video_id IN (SELECT id FROM video WHERE user_id IN (4, 5));
DELETE FROM watch_history WHERE video_id IN (SELECT id FROM video WHERE user_id IN (4, 5));
DELETE FROM message_notify WHERE video_id IN (SELECT id FROM video WHERE user_id IN (4, 5));
DELETE FROM video_chunk   WHERE video_id IN (SELECT id FROM video WHERE user_id IN (4, 5));
DELETE FROM video         WHERE user_id IN (4, 5);
