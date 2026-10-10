<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { playVideo, like, unlike, collect, uncollect, coin, uncoin, listVideos } from '../api/video'
import { follow, unfollow } from '../api/follow'
import { sendDanmaku } from '../api/danmaku'
import { reportProgress } from '../api/history'
import { useAuthStore } from '../stores/auth'
import { formatCount, formatDuration, formatTime } from '../utils/format'
import DanmakuLayer from '../components/DanmakuLayer.vue'
import CommentSection from '../components/CommentSection.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const id = Number(route.params.id)

const video = ref(null)
const notFound = ref(false)
const loading = ref(true)

const videoRef = ref()
const playerRef = ref()
const danmakuRef = ref()

const currentTime = ref(0)
const playing = ref(false)
const seeking = ref(false)
const volume = ref(1)
const danmakuOn = ref(true)

// 弹幕输入
const dmText = ref('')
const dmColor = ref('#FFFFFF')
const dmMode = ref(0)
const dmFontSize = ref(25)
const dmColors = ['#FFFFFF', '#fb7299', '#00a1d6', '#ffd700', '#7fff00', '#ff6b6b']

// 相关推荐
const related = ref([])

function requireLogin() {
  if (!auth.isLogin) {
    ElMessage.warning('请先登录')
    return false
  }
  return true
}

async function load() {
  loading.value = true
  try {
    video.value = await playVideo(id)
    loadRelated()
  } catch (e) {
    notFound.value = true
  } finally {
    loading.value = false
  }
}

async function loadRelated() {
  try {
    const res = await listVideos({ categoryId: video.value.categoryId, orderBy: 'hot', page: 1, size: 8 })
    related.value = (res.list || []).filter((v) => v.id !== id)
  } catch (e) {
    /* ignore */
  }
}

function onTimeUpdate() {
  if (!seeking.value) currentTime.value = videoRef.value.currentTime
}
function onSeekInput() {
  seeking.value = true
}
function onSeekChange(v) {
  seeking.value = false
  videoRef.value.currentTime = v
}
function togglePlay() {
  const v = videoRef.value
  if (!v) return
  if (v.paused) v.play()
  else v.pause()
}
function onVolumeChange(v) {
  videoRef.value.volume = v
}
function toggleFullscreen() {
  if (document.fullscreenElement) document.exitFullscreen()
  else playerRef.value.requestFullscreen()
}

async function toggleLike() {
  if (!requireLogin()) return
  if (video.value.liked) {
    video.value.likeCount = await unlike(id)
    video.value.liked = false
  } else {
    video.value.likeCount = await like(id)
    video.value.liked = true
  }
}

async function toggleCollect() {
  if (!requireLogin()) return
  if (video.value.collected) {
    video.value.collectCount = await uncollect(id)
    video.value.collected = false
  } else {
    video.value.collectCount = await collect(id)
    video.value.collected = true
  }
}

async function toggleCoin() {
  if (!requireLogin()) return
  if (video.value.coined) {
    video.value.coinCount = await uncoin(id)
    video.value.coined = false
  } else {
    video.value.coinCount = await coin(id)
    video.value.coined = true
  }
}

// 上报观看进度（暂停/离开页面时）
async function reportProgressNow() {
  if (!auth.isLogin || !video.value) return
  try {
    await reportProgress(id, Math.floor(currentTime.value || 0))
  } catch (e) {
    /* ignore */
  }
}
function onPause() {
  playing.value = false
  reportProgressNow()
}

async function toggleFollow() {
  if (!requireLogin()) return
  if (video.value.followed) {
    await unfollow(video.value.authorId)
    video.value.followed = false
  } else {
    await follow(video.value.authorId)
    video.value.followed = true
  }
}

async function onSendDanmaku() {
  if (!requireLogin()) return
  const text = dmText.value.trim()
  if (!text) return
  await sendDanmaku({
    videoId: id,
    content: text,
    videoTime: Math.floor(currentTime.value),
    color: dmColor.value,
    mode: dmMode.value,
    fontSize: dmFontSize.value
  })
  danmakuRef.value && danmakuRef.value.spawnLocal(text, dmColor.value, dmMode.value, dmFontSize.value)
  dmText.value = ''
}

function goAuthor() {
  if (video.value && video.value.authorId) {
    router.push(auth.userId === video.value.authorId ? '/me' : `/user/${video.value.authorId}`)
  }
}

onMounted(load)
onBeforeUnmount(reportProgressNow)
</script>

<template>
  <div v-loading="loading" class="play-page">
    <el-result v-if="notFound" icon="warning" title="视频不存在或已下架">
      <template #extra>
        <el-button type="primary" @click="router.push('/')">返回首页</el-button>
      </template>
    </el-result>

    <template v-else-if="video">
      <div class="main-col">
        <div class="player" ref="playerRef">
          <video
            ref="videoRef"
            :src="video.videoUrl"
            :poster="video.coverUrl"
            @timeupdate="onTimeUpdate"
            @play="playing = true"
            @pause="onPause"
            @ended="playing = false"
          ></video>
          <DanmakuLayer
            ref="danmakuRef"
            :video-id="video.id"
            :current-time="currentTime"
            :enabled="danmakuOn"
          />
        </div>

        <div class="controls">
          <el-button circle size="small" @click="togglePlay">
            <el-icon><VideoPause v-if="playing" /><VideoPlay v-else /></el-icon>
          </el-button>
          <span class="time">{{ formatDuration(currentTime) }} / {{ formatDuration(video.duration) }}</span>
          <el-slider
            class="progress"
            v-model="currentTime"
            :max="video.duration || 1"
            :show-tooltip="false"
            @input="onSeekInput"
            @change="onSeekChange"
          />
          <el-switch v-model="danmakuOn" active-text="弹幕" size="small" />
          <div class="vol">
            <el-icon><Headset /></el-icon>
            <el-slider v-model="volume" :max="1" :step="0.1" size="small" style="width: 70px" @change="onVolumeChange" />
          </div>
          <el-button circle size="small" @click="toggleFullscreen">
            <el-icon><FullScreen /></el-icon>
          </el-button>
        </div>

        <div class="dm-bar">
          <span
            v-for="c in dmColors"
            :key="c"
            class="swatch"
            :style="{ background: c, border: dmColor === c ? '2px solid #fb7299' : '2px solid transparent' }"
            @click="dmColor = c"
          ></span>
          <el-select v-model="dmMode" size="small" style="width: 88px">
            <el-option :value="0" label="滚动" />
            <el-option :value="1" label="顶部" />
            <el-option :value="2" label="底部" />
          </el-select>
          <el-input v-model="dmText" size="small" placeholder="发个友善的弹幕见证当下" @keyup.enter="onSendDanmaku" />
          <el-button size="small" type="primary" @click="onSendDanmaku">发送</el-button>
        </div>

        <h1 class="v-title">{{ video.title }}</h1>
        <div class="v-meta">
          <span class="m-item">▶ {{ formatCount(video.playCount) }}</span>
          <span class="m-item">弹幕 {{ formatCount(video.danmakuCount) }}</span>
          <span class="m-item">{{ formatTime(video.createTime) }}</span>
        </div>

        <div class="author-bar">
          <el-avatar :size="44" :src="video.authorAvatar" @click="goAuthor">
            {{ (video.authorName || '?')[0] }}
          </el-avatar>
          <div class="a-info">
            <div class="a-name" @click="goAuthor">{{ video.authorName }}</div>
          </div>
          <div class="a-actions">
            <el-button
              size="small"
              :type="video.followed ? 'default' : 'primary'"
              @click="toggleFollow"
            >{{ video.followed ? '已关注' : '关注' }}</el-button>
            <el-button size="small" :type="video.liked ? 'primary' : 'default'" @click="toggleLike">
              <el-icon><Pointer /></el-icon> {{ video.liked ? '已赞' : '点赞' }} {{ formatCount(video.likeCount) }}
            </el-button>
            <el-button size="small" :type="video.collected ? 'primary' : 'default'" @click="toggleCollect">
              <el-icon><Star /></el-icon> {{ video.collected ? '已收藏' : '收藏' }} {{ formatCount(video.collectCount) }}
            </el-button>
            <el-button size="small" :type="video.coined ? 'primary' : 'default'" @click="toggleCoin">
              <el-icon><Coin /></el-icon> {{ video.coined ? '已投币' : '投币' }} {{ formatCount(video.coinCount) }}
            </el-button>
          </div>
        </div>

        <div class="v-desc">{{ video.description || '暂无简介' }}</div>

        <CommentSection :video-id="video.id" />
      </div>

      <div class="side-col">
        <h3 class="side-title">相关推荐</h3>
        <div
          v-for="r in related"
          :key="r.id"
          class="rel-item"
          @click="router.push(`/video/${r.id}`)"
        >
          <img v-if="r.coverUrl" :src="r.coverUrl" alt="" />
          <div v-else class="rel-noimg">🎬</div>
          <div class="rel-info">
            <div class="rel-title">{{ r.title }}</div>
            <div class="rel-meta">{{ r.authorName }} · {{ formatCount(r.playCount) }} 播放</div>
          </div>
        </div>
        <div v-if="related.length === 0" class="rel-empty">暂无推荐</div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.play-page {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}
.main-col {
  flex: 1;
  min-width: 0;
}
.player {
  position: relative;
  aspect-ratio: 16 / 9;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}
.player video {
  width: 100%;
  height: 100%;
  display: block;
}
.controls {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
}
.time {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}
.progress {
  flex: 1;
  margin: 0;
}
.vol {
  display: flex;
  align-items: center;
  gap: 4px;
}
.dm-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
}
.swatch {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  cursor: pointer;
  display: inline-block;
  box-sizing: border-box;
}
.v-title {
  font-size: 20px;
  margin: 12px 0 8px;
  line-height: 28px;
}
.v-meta {
  display: flex;
  gap: 16px;
  color: #999;
  font-size: 13px;
  margin-bottom: 12px;
}
.author-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}
.a-info {
  flex: 1;
}
.a-name {
  font-weight: 600;
  cursor: pointer;
}
.a-actions {
  display: flex;
  gap: 8px;
}
.v-desc {
  padding: 12px 0;
  font-size: 14px;
  color: #555;
  line-height: 22px;
  white-space: pre-wrap;
  word-break: break-word;
}
.side-col {
  width: 300px;
  flex-shrink: 0;
}
.side-title {
  margin: 0 0 12px;
  font-size: 15px;
}
.rel-item {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  cursor: pointer;
}
.rel-item img,
.rel-noimg {
  width: 120px;
  height: 68px;
  border-radius: 4px;
  object-fit: cover;
  background: #e5e9ef;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.rel-info {
  min-width: 0;
}
.rel-title {
  font-size: 13px;
  line-height: 18px;
  height: 36px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.rel-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #999;
}
.rel-empty {
  color: #99a2aa;
  font-size: 13px;
  text-align: center;
  padding: 20px 0;
}
@media (max-width: 768px) {
  .play-page {
    flex-direction: column;
  }
  .side-col {
    width: 100%;
  }
  .vol {
    display: none;
  }
  .controls {
    flex-wrap: wrap;
  }
  .dm-bar {
    flex-wrap: wrap;
  }
  .dm-bar .el-input {
    flex: 1 1 150px;
    min-width: 0;
  }
  .v-title {
    font-size: 17px;
    line-height: 24px;
  }
  .author-bar {
    flex-wrap: wrap;
  }
  .a-actions {
    flex-wrap: wrap;
    width: 100%;
  }
}
</style>
