import { defineStore } from 'pinia'
import { unreadCount } from '../api/message'
import { pmUnreadCount } from '../api/pm'

/**
 * 顶部导航未读徽标：消息中心 + 私信。
 * 由 App.vue 展示，消息页 / 私信页在“已读”后调用 refresh() 立即刷新，避免红点残留。
 */
export const useUnreadStore = defineStore('unread', {
  state: () => ({
    notify: 0,
    pm: 0
  }),
  actions: {
    async refresh() {
      try {
        this.notify = await unreadCount()
      } catch (e) {
        /* 忽略，保持原值 */
      }
      try {
        this.pm = await pmUnreadCount()
      } catch (e) {
        /* 忽略，保持原值 */
      }
    },
    reset() {
      this.notify = 0
      this.pm = 0
    }
  }
})
