<script setup>
import { clickAd } from '../api/ad'

const props = defineProps({
  ad: { type: Object, required: true }
})

function goAd() {
  if (props.ad.id) {
    clickAd(props.ad.id).catch(() => {})
  }
  if (props.ad.targetUrl) {
    window.open(props.ad.targetUrl, '_blank')
  }
}
</script>

<template>
  <div class="ad-card" @click="goAd">
    <div class="cover">
      <img v-if="ad.coverUrl" :src="ad.coverUrl" loading="lazy" alt="" />
      <div v-else class="no-cover">📢</div>
      <div class="mask"></div>
      <span class="ad-tag">广告</span>
    </div>
    <div class="body">
      <div class="title" :title="ad.title">{{ ad.title }}</div>
      <div class="sponsor">{{ ad.categoryName || '推广' }}</div>
    </div>
  </div>
</template>

<style scoped>
.ad-card {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  padding: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}
.ad-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}
.cover {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 8px;
  overflow: hidden;
  background: #e5e9ef;
  flex-shrink: 0;
}
.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
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
  height: 45%;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.4), rgba(0, 0, 0, 0));
  pointer-events: none;
}
.ad-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 2;
  background: rgba(0, 0, 0, 0.65);
  color: #fff;
  font-size: 12px;
  line-height: 1.5;
  padding: 2px 6px;
  border-radius: 4px;
}
.body {
  display: flex;
  flex-direction: column;
  padding: 8px 2px 2px;
}
.title {
  font-size: 14px;
  line-height: 20px;
  color: #18191c;
  height: 40px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-all;
}
.sponsor {
  margin-top: 4px;
  font-size: 12px;
  line-height: 18px;
  color: #9499a0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
