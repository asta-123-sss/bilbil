<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { formatCount, formatDuration } from '../utils/format'

const props = defineProps({
  video: { type: Object, required: true }
})

const router = useRouter()

function goPlay() {
  router.push(`/video/${props.video.id}`)
}

// 去掉标题里已带的【xxx】前缀，避免和卡片前缀重复
const cleanTitle = computed(() => (props.video.title || '').replace(/^【[^】]*】\s*/, ''))

// 未上传封面时，用 <video> 自动取视频首帧作为封面（preload=metadata 只拉取元数据与首帧）
function seekFirstFrame(e) {
  const v = e.target
  try {
    if (v.readyState >= 1 && v.currentTime === 0) v.currentTime = 0.1
  } catch (err) {
    /* 忽略 seek 受限的情况 */
  }
}
</script>

<template>
  <div class="video-card" @click="goPlay">
    <div class="cover">
      <img v-if="video.coverUrl" :src="video.coverUrl" loading="lazy" alt="" />
      <video
        v-else-if="video.videoUrl"
        :src="video.videoUrl"
        preload="metadata"
        muted
        playsinline
        @loadedmetadata="seekFirstFrame"
      ></video>
      <div v-else class="no-cover">🎬</div>

      <div class="mask"></div>

      <span v-if="video.duration" class="badge duration">
        <el-icon><CaretRight /></el-icon>{{ formatDuration(video.duration) }}
      </span>
    </div>

    <div class="title" :title="video.title">
      <span v-if="video.categoryName" class="cat-prefix">【{{ video.categoryName }}】</span>{{ cleanTitle }}
    </div>

    <div class="author">
      <el-avatar :size="22" :src="video.authorAvatar">
        {{ (video.authorName || '?')[0] }}
      </el-avatar>
      <span class="aname">{{ video.authorName || '未知UP主' }}</span>
    </div>

    <div class="stats">
      <span class="stat"><el-icon><VideoPlay /></el-icon>{{ formatCount(video.playCount) }}</span>
      <span class="stat"><el-icon><ChatDotRound /></el-icon>{{ formatCount(video.danmakuCount) }}</span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
$pink: #fb7299;
$text: #212121;
$muted: #9499a0;

.video-card {
  background: #fff;
  border-radius: 8px;
  padding: 8px;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: box-shadow 0.2s ease, transform 0.2s ease;

  &:hover {
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    transform: translateY(-2px);
  }
}

.cover {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 8px;
  overflow: hidden;
  background: #e5e9ef;

  img,
  video {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.no-cover {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  background: linear-gradient(135deg, #ffd9e2, #ffe7ef);
}

.mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 40%;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.35), rgba(0, 0, 0, 0));
  pointer-events: none;
}

.badge {
  position: absolute;
  bottom: 6px;
  left: 6px;
  display: flex;
  align-items: center;
  gap: 2px;
  background: rgba(0, 0, 0, 0.65);
  color: #fff;
  font-size: 12px;
  line-height: 1;
  padding: 3px 6px;
  border-radius: 4px;
}

.title {
  margin-top: 8px;
  font-size: 14px;
  line-height: 20px;
  height: 40px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  color: $text;
  word-break: break-all;

  .cat-prefix {
    color: $pink;
  }
}

.author {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 6px;

  .aname {
    font-size: 12px;
    color: $muted;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.stats {
  margin-top: 4px;
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: $muted;

  .stat {
    display: flex;
    align-items: center;
    gap: 3px;
  }
}
</style>
