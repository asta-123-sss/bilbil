<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { listCategories } from '../api/category'
import { listVideos, listRecommend } from '../api/video'
import { listAdFeed } from '../api/ad'
import VideoCard from '../components/VideoCard.vue'
import BannerCarousel from '../components/BannerCarousel.vue'

const route = useRoute()
const auth = useAuthStore()

const categories = ref([])
const activeCategory = ref(null) // null 表示热门/全部
const keyword = ref('')
const orderBy = ref('recommend') // recommend | hot | new
const page = ref(1)
const size = 12
const total = ref(0)
const videos = ref([])
const ads = ref([])
const bannerVideos = ref([])
const loading = ref(false)

// 二级分类栏：常显前几个，其余进「更多」下拉
const VISIBLE_CAT = 6
const visibleCats = computed(() => categories.value.slice(0, VISIBLE_CAT))
const moreCats = computed(() => categories.value.slice(VISIBLE_CAT))

async function loadCategories() {
  categories.value = (await listCategories()) || []
}

async function loadVideos() {
  loading.value = true
  try {
    let res
    const isRecommend = orderBy.value === 'recommend' && !activeCategory.value && !keyword.value.trim()
    if (isRecommend) {
      res = await listRecommend({ page: page.value, size })
    } else {
      const params = { page: page.value, size, orderBy: orderBy.value }
      if (activeCategory.value) params.categoryId = activeCategory.value
      if (keyword.value.trim()) params.keyword = keyword.value.trim()
      res = await listVideos(params)
    }
    videos.value = res.list || []
    total.value = res.total
  } finally {
    loading.value = false
  }
}

// 首页顶部轮播图：广告 + 热门视频（独立拉取，不混进下方视频列表）
async function loadBanner() {
  try {
    ads.value = (await listAdFeed()) || []
  } catch (e) {
    ads.value = []
  }
  try {
    const res = await listVideos({ page: 1, size: 6, orderBy: 'hot' })
    bannerVideos.value = res.list || []
  } catch (e) {
    bannerVideos.value = []
  }
}

// 「热门」按钮：回到全部分区，按热度排序
function selectHot() {
  activeCategory.value = null
  orderBy.value = 'hot'
  page.value = 1
  loadVideos()
}

// 「推荐」tab：回到个性化推荐流
function selectRecommend() {
  activeCategory.value = null
  orderBy.value = 'recommend'
  page.value = 1
  loadVideos()
}

function selectCategory(id) {
  if (orderBy.value === 'recommend') orderBy.value = 'new'
  activeCategory.value = id
  page.value = 1
  loadVideos()
}

// 「换一换」：刷新当前信息流（推荐模式下重新拉推荐）
function refreshFeed() {
  page.value = 1
  loadVideos()
}

function onPageChange(p) {
  page.value = p
  loadVideos()
}

onMounted(() => {
  keyword.value = (route.query.keyword || '').trim()
  // 带关键词进入时走「最新」列表（后端 list 仅支持 new/hot，不支持 recommend）
  if (keyword.value && orderBy.value === 'recommend') orderBy.value = 'new'
  loadCategories()
  loadBanner()
  loadVideos()
})

// 顶栏搜索会带 ?keyword= 跳回首页，这里监听变化
watch(
  () => route.query.keyword,
  (kw) => {
    keyword.value = (kw || '').trim()
    if (orderBy.value === 'recommend') orderBy.value = 'new'
    page.value = 1
    loadVideos()
  }
)
</script>

<template>
  <div class="home">
    <BannerCarousel class="banner" :ads="ads" :videos="bannerVideos" />

    <div class="channel">
      <div class="channel-left">
        <div class="channel-top">
          <el-avatar :size="36" class="channel-avatar" :src="auth.user && auth.user.avatar">B</el-avatar>
          <span
            class="hot-btn"
            :class="{ active: activeCategory === null && orderBy === 'hot' }"
            @click="selectHot"
          >热门</span>
        </div>
        <div class="channel-tabs">
          <span
            class="tab"
            :class="{ active: activeCategory === null && orderBy === 'recommend' }"
            @click="selectRecommend"
          >推荐</span>
          <span
            v-for="c in visibleCats"
            :key="c.id"
            class="tab"
            :class="{ active: activeCategory === c.id }"
            @click="selectCategory(c.id)"
          >{{ c.name }}</span>
          <el-dropdown v-if="moreCats.length" trigger="click" @command="selectCategory">
            <span class="tab more">更多<el-icon><ArrowDown /></el-icon></span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="c in moreCats" :key="c.id" :command="c.id">
                  {{ c.name }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <div class="channel-right">
        <el-button class="refresh-btn" @click="refreshFeed">
          <el-icon><Refresh /></el-icon>换一换
        </el-button>
      </div>
    </div>

    <div v-if="keyword" class="search-tip">
      搜索「{{ keyword }}」的结果
    </div>

    <div v-loading="loading" class="grid">
      <VideoCard v-for="v in videos" :key="v.id" :video="v" />
    </div>

    <div v-if="!loading && videos.length === 0" class="empty">
      暂无视频，快去<a href="/upload">投稿</a>吧~
    </div>

    <div v-if="total > size" class="pager">
      <el-pagination
        layout="prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>

<style lang="scss" scoped>
$pink: #fb7299;
$text: #212121;
$muted: #9499a0;

.home {
  padding-top: 16px;
}

.banner {
  margin-bottom: 16px;
}

.channel {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.channel-left {
  flex: 1;
  min-width: 0;
}

.channel-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.channel-avatar {
  background: $pink;
  color: #fff;
  font-weight: 700;
  flex-shrink: 0;
}

.hot-btn {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 18px;
  border-radius: 15px;
  background: #fff;
  color: $text;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    color: $pink;
  }

  &.active {
    background: $pink;
    color: #fff;
  }
}

.channel-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.tab {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 4px 12px;
  border-radius: 14px;
  font-size: 13px;
  color: #61666d;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;

  &:hover {
    color: $pink;
  }

  &.active {
    background: #ffe1ea;
    color: $pink;
    font-weight: 600;
  }

  &.more {
    gap: 4px;
  }
}

.channel-right {
  flex-shrink: 0;
  padding-top: 2px;
}

.refresh-btn {
  border-radius: 16px;
  color: $pink;
  border-color: $pink;

  &:hover {
    background: #ffe1ea;
    border-color: $pink;
    color: $pink;
  }
}

.search-tip {
  margin-bottom: 12px;
  font-size: 14px;
  color: $muted;
}

.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.empty {
  text-align: center;
  color: #99a2aa;
  padding: 60px 0;

  a {
    color: $pink;
  }
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 1200px) {
  .grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
}
</style>
