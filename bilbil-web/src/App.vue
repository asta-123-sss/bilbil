<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from './stores/auth'
import { useUnreadStore } from './stores/unread'
import { logout as apiLogout } from './api/user'
import { listCategories } from './api/category'

const auth = useAuthStore()
const unread = useUnreadStore()
const router = useRouter()

const displayName = computed(() => (auth.user ? auth.user.nickname || auth.user.username : ''))
const searchKeyword = ref('')
const cats = ref([])

async function loadCats() {
  try {
    cats.value = (await listCategories()) || []
  } catch (e) {
    cats.value = []
  }
}

async function refreshUnread() {
  if (!auth.isLogin) {
    unread.reset()
    return
  }
  await unread.refresh()
}

function onSearch() {
  const kw = searchKeyword.value.trim()
  router.push({ path: '/', query: kw ? { keyword: kw } : {} })
}

function onCommand(cmd) {
  if (cmd === 'me') router.push('/me')
  else if (cmd === 'pm') router.push('/pm')
  else if (cmd === 'merchant') router.push('/merchant')
  else if (cmd === 'admin') router.push('/admin')
  else if (cmd === 'logout') handleLogout()
}

async function handleLogout() {
  try {
    await apiLogout()
  } catch (e) {
    /* token 已失效也会被拦截器处理 */
  }
  auth.logout()
  unread.reset()
  router.push('/login')
}

function handleSwitch(acc) {
  auth.switchAccount(acc)
  unread.reset()
  refreshUnread()
  ElMessage.success(`已切换到 ${acc.user.nickname || acc.user.username}`)
  router.push('/')
}

onMounted(() => {
  refreshUnread()
  loadCats()
})
watch(() => auth.isLogin, refreshUnread)
</script>

<template>
  <div class="app">
    <header class="header">
      <div class="header-inner">
        <div class="logo" @click="router.push('/')">
          <svg class="logo-icon" viewBox="0 0 34 24" width="28" height="20" aria-hidden="true">
            <rect x="2" y="6" width="30" height="16" rx="4" fill="#fb7299" />
            <path d="M9 6 L9 2 M16 6 L16 2" stroke="#fb7299" stroke-width="2.6" stroke-linecap="round" />
          </svg>
          <span class="logo-text">映界</span>
        </div>

        <nav class="nav">
          <router-link to="/">首页</router-link>
          <router-link v-for="c in cats" :key="c.id" :to="`/category/${c.id}`">{{ c.name }}</router-link>
        </nav>

        <div class="right">
          <el-input
            v-model="searchKeyword"
            class="search"
            placeholder="搜索你感兴趣的视频"
            clearable
            @keyup.enter="onSearch"
            @clear="onSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>

          <span class="nav-item vip" title="大会员"><el-icon><Crown /></el-icon>大会员</span>

          <router-link v-if="auth.isLogin" class="nav-item" to="/message">
            消息
            <el-badge v-if="unread.notify > 0" :value="unread.notify" :max="99" class="badge" />
          </router-link>
          <span class="nav-item">动态</span>
          <span class="nav-item">收藏</span>
          <router-link v-if="auth.isLogin" class="nav-item" to="/history">历史</router-link>

          <el-button v-if="auth.isLogin" class="upload-btn" @click="router.push('/upload')">
            <el-icon><VideoCamera /></el-icon>投稿
          </el-button>

          <template v-if="auth.isLogin">
            <el-dropdown trigger="click" @command="onCommand">
              <span class="user-trigger">
                <el-avatar :size="34" :src="auth.user && auth.user.avatar">
                  {{ displayName ? displayName[0] : '?' }}
                </el-avatar>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="me">个人中心</el-dropdown-item>
                  <el-dropdown-item command="pm">
                    私信
                    <el-badge v-if="unread.pm > 0" :value="unread.pm" :max="99" class="dropdown-badge" />
                  </el-dropdown-item>
                  <el-dropdown-item command="merchant">商家中心</el-dropdown-item>
                  <el-dropdown-item v-if="auth.user && auth.user.role === 1" command="admin">后台管理</el-dropdown-item>
                  <template v-if="auth.otherAccounts.length">
                    <el-dropdown-item divided disabled>切换账号</el-dropdown-item>
                    <el-dropdown-item v-for="acc in auth.otherAccounts" :key="acc.user.id" @click="handleSwitch(acc)">
                      <el-avatar :size="20" :src="acc.user.avatar" class="acc-avatar">
                        {{ (acc.user.nickname || acc.user.username)[0] }}
                      </el-avatar>
                      {{ acc.user.nickname || acc.user.username }}
                    </el-dropdown-item>
                  </template>
                  <el-dropdown-item divided @click="router.push('/login')">登录其他账号</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="router.push('/login')">登录</el-button>
            <el-button class="register-btn" @click="router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="main">
      <router-view />
    </main>
  </div>
</template>

<style lang="scss" scoped>
$pink: #fb7299;
$text: #212121;
$muted: #757575;

.app {
  min-height: 100vh;
  background: #f4f4f4;
}

.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.header-inner {
  max-width: 1240px;
  margin: 0 auto;
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  flex-shrink: 0;

  .logo-text {
    font-size: 22px;
    font-weight: 700;
    color: $pink;
    letter-spacing: 0.5px;
  }
}

.nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  min-width: 0;

  a,
  .nav-item {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 0 12px;
    height: 64px;
    color: $text;
    font-size: 14px;
    text-decoration: none;
    white-space: nowrap;
    cursor: pointer;

    &:hover {
      color: $pink;
    }
  }

  a.router-link-exact-active {
    color: $pink;
    font-weight: 600;
  }
}

.right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;

  .search {
    width: 220px;

    :deep(.el-input__wrapper) {
      border-radius: 8px;
    }
  }

  .nav-item {
    display: flex;
    align-items: center;
    gap: 4px;
    color: $text;
    font-size: 14px;
    cursor: pointer;
    white-space: nowrap;
    text-decoration: none;

    &:hover {
      color: $pink;
    }

    &.vip {
      color: $pink;
      font-size: 13px;
    }
  }

  .badge {
    margin-left: 2px;
  }

  .upload-btn {
    background: $pink;
    border-color: $pink;
    color: #fff;
    border-radius: 8px;
    padding: 0 16px;
    height: 34px;

    &:hover {
      background: #ff85ab;
      border-color: #ff85ab;
    }
  }

  .register-btn {
    background: $pink;
    border-color: $pink;
    color: #fff;
    border-radius: 8px;

    &:hover {
      background: #ff85ab;
      border-color: #ff85ab;
    }
  }
}

.user-trigger {
  display: inline-flex;
  cursor: pointer;
  outline: none;
}

.dropdown-badge {
  margin-left: 6px;
}

.acc-avatar {
  margin-right: 6px;
  vertical-align: middle;
}

.main {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 24px 24px;
}

@media (max-width: 992px) {
  .nav {
    overflow-x: auto;
    scrollbar-width: none;

    &::-webkit-scrollbar {
      display: none;
    }
  }

  .right .nav-item {
    display: none;
  }

  .right .search {
    width: 150px;
  }
}
</style>
