<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { clickAd } from '../api/ad'

const props = defineProps({
  ads: { type: Array, default: () => [] },
  videos: { type: Array, default: () => [] }
})

const router = useRouter()

// 没有广告和热门视频时的占位渐变图（兜底）
const FALLBACK_SLIDES = [
  {
    title: '欢迎来到映界',
    subtitle: '发现你感兴趣的优质内容',
    bg: 'linear-gradient(120deg, #fb7299 0%, #ff8fb0 55%, #ffb3c8 100%)',
    icon: '📺'
  },
  {
    title: '直播专区',
    subtitle: '与主播实时互动，不错过精彩瞬间',
    bg: 'linear-gradient(120deg, #4f7cff 0%, #6a5cff 60%, #9b7bff 100%)',
    icon: '📡'
  },
  {
    title: '番剧上新',
    subtitle: '热门动画持续更新中',
    bg: 'linear-gradient(120deg, #00b5b8 0%, #2ec6a0 60%, #6fd6b0 100%)',
    icon: '🎬'
  },
  {
    title: '投稿赢好礼',
    subtitle: '上传你的第一个视频吧',
    bg: 'linear-gradient(120deg, #ff8f3f 0%, #ffb64f 60%, #ffd28a 100%)',
    icon: '🎁'
  }
]

// 广告在前、热门视频在后，统一成轮播项（广告需封面；视频有封面用封面，没有则用视频首帧）
const slides = computed(() => {
  const adItems = (props.ads || [])
    .filter((a) => a && a.coverUrl)
    .map((a) => ({ type: 'ad', id: a.id, title: a.title, coverUrl: a.coverUrl, targetUrl: a.targetUrl }))
  const videoItems = (props.videos || [])
    .filter((v) => v && (v.coverUrl || v.videoUrl))
    .map((v) => ({ type: 'video', id: v.id, title: v.title, coverUrl: v.coverUrl, videoUrl: v.videoUrl }))
  const list = [...adItems, ...videoItems]
  return list.length ? list : FALLBACK_SLIDES
})

function onAdClick(ad) {
  clickAd(ad.id).catch(() => {})
  if (ad.targetUrl) window.open(ad.targetUrl, '_blank', 'noopener')
}

function onVideoClick(v) {
  router.push(`/video/${v.id}`)
}

// 没传封面的视频，用 <video> 自动取首帧当封面（preload=metadata 只拉元数据与首帧）
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
  <el-carousel class="banner" height="220px" :interval="5000" arrow="always">
    <el-carousel-item v-for="(s, i) in slides" :key="i">
      <!-- 广告大图 -->
      <div v-if="s.type === 'ad'" class="cover-slide" @click="onAdClick(s)">
        <img class="cover-img" :src="s.coverUrl" :alt="s.title" />
        <div class="cover-mask"></div>
        <div class="cover-info">
          <span class="cover-tag ad-tag">广告</span>
          <span class="cover-title">{{ s.title }}</span>
        </div>
      </div>
      <!-- 热门视频封面 -->
      <div v-else-if="s.type === 'video'" class="cover-slide" @click="onVideoClick(s)">
        <img v-if="s.coverUrl" class="cover-img" :src="s.coverUrl" :alt="s.title" />
        <video
          v-else-if="s.videoUrl"
          class="cover-img"
          :src="s.videoUrl"
          preload="metadata"
          muted
          playsinline
          @loadedmetadata="seekFirstFrame"
        ></video>
        <div class="cover-mask"></div>
        <div class="cover-info">
          <span class="cover-tag hot-tag">热门</span>
          <span class="cover-title">{{ s.title }}</span>
        </div>
      </div>
      <!-- 占位渐变 -->
      <div v-else class="slide" :style="{ background: s.bg }">
        <span class="slide-icon">{{ s.icon }}</span>
        <div class="slide-text">
          <div class="slide-title">{{ s.title }}</div>
          <div class="slide-subtitle">{{ s.subtitle }}</div>
        </div>
        <span class="deco deco-1"></span>
        <span class="deco deco-2"></span>
      </div>
    </el-carousel-item>
  </el-carousel>
</template>

<style lang="scss" scoped>
.banner {
  border-radius: 8px;
  overflow: hidden;

  :deep(.el-carousel__item) {
    border-radius: 8px;
  }

  :deep(.el-carousel__arrow) {
    width: 34px;
    height: 34px;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.35);
    transition: background 0.2s;

    &:hover {
      background: rgba(0, 0, 0, 0.55);
    }
  }

  :deep(.el-carousel__indicators .el-carousel__button) {
    width: 18px;
    height: 4px;
    border-radius: 2px;
    background: rgba(255, 255, 255, 0.6);
  }

  :deep(.el-carousel__indicator.is-active .el-carousel__button) {
    background: #fff;
  }
}

// ---------- 广告 / 热门视频 封面大图 ----------
.cover-slide {
  position: relative;
  width: 100%;
  height: 100%;
  cursor: pointer;
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 64px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.55), transparent);
  pointer-events: none;
}

.cover-info {
  position: absolute;
  left: 16px;
  bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  z-index: 1;
}

.cover-tag {
  font-size: 12px;
  padding: 1px 8px;
  border-radius: 4px;
  flex-shrink: 0;
  color: #fff;

  &.ad-tag {
    border: 1px solid rgba(255, 255, 255, 0.9);
  }

  &.hot-tag {
    background: #fb7299;
  }
}

.cover-title {
  font-size: 16px;
  font-weight: 600;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.35);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 60vw;
}

// ---------- 占位渐变 ----------
.slide {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 0 56px;
  overflow: hidden;
}

.slide-icon {
  font-size: 64px;
  flex-shrink: 0;
  filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.18));
}

.slide-text {
  color: #fff;
  z-index: 1;
}

.slide-title {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 1px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.slide-subtitle {
  margin-top: 8px;
  font-size: 15px;
  opacity: 0.92;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
}

.deco {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.14);
  pointer-events: none;
}

.deco-1 {
  width: 220px;
  height: 220px;
  right: -40px;
  top: -80px;
}

.deco-2 {
  width: 140px;
  height: 140px;
  right: 160px;
  bottom: -60px;
}

@media (max-width: 768px) {
  .slide-icon {
    font-size: 40px;
  }
  .slide-title {
    font-size: 20px;
  }
  .slide {
    padding: 0 24px;
  }
  .cover-title {
    font-size: 14px;
  }
}
</style>
