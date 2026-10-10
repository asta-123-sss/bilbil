import { defineStore } from 'pinia'

/**
 * 登录态管理：token + 当前用户信息，持久化到 localStorage。
 * 额外维护一份「已登录过的账号列表」（每个账号各自的 token + 用户信息），
 * 用于在应用内一键切换账号、免重复登录。
 */

const ACCOUNTS_KEY = 'bilbil_accounts'

function loadAccounts() {
  try {
    const arr = JSON.parse(localStorage.getItem(ACCOUNTS_KEY) || '[]')
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    accounts: loadAccounts()
  }),
  getters: {
    isLogin: (state) => !!state.token,
    userId: (state) => (state.user ? state.user.id : null),
    // 除当前账号外的其他已记住账号（用于「切换账号」）
    otherAccounts: (state) =>
      state.accounts.filter((a) => a.user && a.user.id !== (state.user && state.user.id))
  },
  actions: {
    /** 统一落盘：token / user / accounts */
    save() {
      localStorage.setItem('token', this.token)
      localStorage.setItem('user', JSON.stringify(this.user))
      localStorage.setItem(ACCOUNTS_KEY, JSON.stringify(this.accounts))
    },
    /** 登录成功：设为当前账号，并记录/更新到账号列表 */
    setLogin(token, user) {
      this.token = token
      this.user = user
      const i = this.accounts.findIndex((a) => a.user && a.user.id === user.id)
      const entry = { token, user }
      if (i >= 0) this.accounts[i] = entry
      else this.accounts.push(entry)
      this.save()
    },
    /** 更新当前用户信息（资料修改后） */
    setUser(user) {
      this.user = user
      const i = this.accounts.findIndex((a) => a.user && a.user.id === user.id)
      if (i >= 0) this.accounts[i] = { token: this.token, user }
      this.save()
    },
    /** 切换账号：直接使用已记住账号的 token 与用户信息，免重新登录 */
    switchAccount(entry) {
      this.token = entry.token
      this.user = entry.user
      this.save()
    },
    /** 从记住列表里移除某个账号 */
    removeAccount(userId) {
      this.accounts = this.accounts.filter((a) => !(a.user && a.user.id === userId))
      this.save()
    },
    /** 退出当前账号：清空会话，并把该账号从记住列表移除（其 token 已被后端删除） */
    logout() {
      const uid = this.user && this.user.id
      this.token = ''
      this.user = null
      if (uid != null) {
        this.accounts = this.accounts.filter((a) => !(a.user && a.user.id === uid))
      }
      this.save()
    }
  }
})
