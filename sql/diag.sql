-- 诊断：列出所有账号、以及所有视频和它们的投稿人（只读）
SELECT '===== 用户列表 =====' AS info;
SELECT id, username, nickname FROM user ORDER BY id;

SELECT '===== 视频列表 =====' AS info;
SELECT v.id, v.title, v.status, v.create_time, u.id AS uid, u.username, u.nickname
FROM video v
JOIN user u ON u.id = v.user_id
ORDER BY v.id DESC;
