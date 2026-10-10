<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { listCategories } from '../api/category'
import { listVideos } from '../api/video'
import VideoCard from '../components/VideoCard.vue'

const route = useRoute()
const categoryId = computed(() => Number(route.params.id))
const categoryName = ref('')
const page = ref(1)
const size = 12
const total = ref(0)
const videos = ref([])
const loading = ref(false)

async function loadCategoryName() {
  try {
    const cats = (await listCategories()) || []
    const c = cats.find((x) => x.id === categoryId.value)
    categoryName.value = c ? c.name : '分区'
  } catch (e) {
    categoryName.value = '分区'
  }
}

async function loadVideos() {
  loading.value = true
  try {
    const res = await listVideos({
      page: page.value,
      size,
      orderBy: 'new',
      categoryId: categoryId.value
    })
    videos.value = res.list || []
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function onPageChange(p) {
  page.value = p
  loadVideos()
}

onMounted(() => {
  loadCategoryName()
  loadVideos()
})

// 顶栏切换分区时复用同一页面，监听参数变化重新加载
watch(categoryId, () => {
  page.value = 1
  loadCategoryName()
  loadVideos()
})
</script>

<template>
  <div class="category">
    <div class="category-header">
      <h1 class="title">{{ categoryName }}</h1>
      <span v-if="total" class="count">共 {{ total }} 个视频</span>
    </div>

    <div v-loading="loading" class="grid">
      <VideoCard v-for="v in videos" :key="v.id" :video="v" />
    </div>

    <div v-if="!loading && videos.length === 0" class="empty">该分区暂时没有视频</div>

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

.category {
  padding-top: 16px;
}

.category-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 16px;

  .title {
    font-size: 22px;
    font-weight: 700;
    color: #212121;
    margin: 0;
  }

  .count {
    font-size: 13px;
    color: #9499a0;
  }
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
