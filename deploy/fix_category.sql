-- 修复：MySQL 首次初始化时中文被错误编码，这里按正确值重刷分类名与描述
USE bilbil;

UPDATE `video_category` SET `name`='游戏', `description`='单机、网游、手游、电竞等游戏相关内容' WHERE `sort`=1;
UPDATE `video_category` SET `name`='生活', `description`='日常记录、美食、Vlog 等生活内容' WHERE `sort`=2;
UPDATE `video_category` SET `name`='科技', `description`='数码评测、编程、前沿技术分享' WHERE `sort`=3;
UPDATE `video_category` SET `name`='动画', `description`='番剧、手书、动画短片' WHERE `sort`=4;
UPDATE `video_category` SET `name`='音乐', `description`='翻唱、原创、MV' WHERE `sort`=5;
UPDATE `video_category` SET `name`='知识', `description`='课程、科普、纪录片' WHERE `sort`=6;
