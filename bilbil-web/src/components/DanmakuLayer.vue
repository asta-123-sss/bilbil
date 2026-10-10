<script setup>
import { ref, watch } from 'vue'
import { listDanmaku } from '../api/danmaku'

/**
 * 弹幕显示层：覆盖在 <video> 之上，按播放进度分段拉取并渲染弹幕。
 * 通过 defineExpose 暴露 spawnLocal，供父组件在发送成功后即时显示一条弹幕。
 */
const props = defineProps({
  videoId: { type: [Number, String], default: null },
  currentTime: { type: Number, default: 0 },
  enabled: { type: Boolean, default: true }
})

const active = ref([]) // 正在显示的弹幕
const cache = {} // 30s 区间 -> 弹幕列表（缓存，seek 回来可重放）
let currentBucket = -1
let idSeq = 0

const LANE_COUNT = 12
const LINE_HEIGHT = 30

watch(
  () => [props.videoId, props.enabled, props.currentTime],
  async ([videoId, enabled, t]) => {
    if (!enabled || !videoId) return
    const bucket = Math.floor(t / 30)
    if (bucket === currentBucket) return
    currentBucket = bucket
    if (!cache[bucket]) {
      try {
        const list = await listDanmaku({ videoId, from: bucket * 30, to: bucket * 30 + 30 })
        cache[bucket] = list || []
      } catch (e) {
        cache[bucket] = []
      }
    }
    ;(cache[bucket] || []).forEach((d, i) => setTimeout(() => spawn(d), i * 150))
  },
  { immediate: true }
)

function spawn(d) {
  if (active.value.length > 80) return
  const mode = d.mode ?? 0
  active.value.push({
    id: ++idSeq,
    text: d.content,
    color: d.color || '#FFFFFF',
    fontSize: d.fontSize || 25,
    mode,
    lane: Math.floor(Math.random() * (mode === 0 ? LANE_COUNT : 5)),
    duration: mode === 0 ? 6 + Math.random() * 4 : 3
  })
}

function spawnLocal(content, color, mode, fontSize) {
  spawn({ content, color, mode, fontSize })
}

function removeItem(id) {
  active.value = active.value.filter((it) => it.id !== id)
}

function itemStyle(it) {
  const base = {
    color: it.color,
    fontSize: it.fontSize + 'px',
    animationDuration: it.duration + 's'
  }
  if (it.mode === 0) {
    base.top = it.lane * LINE_HEIGHT + 'px'
  } else if (it.mode === 1) {
    base.top = it.lane * LINE_HEIGHT + 'px'
    base.left = '50%'
    base.transform = 'translateX(-50%)'
  } else {
    base.bottom = it.lane * LINE_HEIGHT + 'px'
    base.left = '50%'
    base.transform = 'translateX(-50%)'
  }
  return base
}

defineExpose({ spawnLocal })
</script>

<template>
  <div class="danmaku-layer">
    <span
      v-for="it in active"
      :key="it.id"
      class="dm"
      :class="it.mode === 1 ? 'dm-top' : it.mode === 2 ? 'dm-bottom' : 'dm-scroll'"
      :style="itemStyle(it)"
      @animationend="removeItem(it.id)"
    >{{ it.text }}</span>
  </div>
</template>

<style scoped>
.danmaku-layer {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}
.dm {
  position: absolute;
  white-space: nowrap;
  font-weight: 500;
  text-shadow: 0 0 2px rgba(0, 0, 0, 0.8);
  animation-fill-mode: forwards;
  animation-timing-function: linear;
}
.dm-scroll {
  left: 100%;
  animation-name: danmaku-scroll;
}
.dm-top,
.dm-bottom {
  animation-name: danmaku-fixed;
}
</style>

<style>
/* 关键帧放全局：内联 style 里用 animation-duration 动态控制时长，需保证 keyframes 名不被 scoped 改写 */
@keyframes danmaku-scroll {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(calc(-100vw - 100%));
  }
}
@keyframes danmaku-fixed {
  0% {
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  80% {
    opacity: 1;
  }
  100% {
    opacity: 0;
  }
}
</style>
