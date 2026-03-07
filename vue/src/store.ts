import { createStore } from 'vuex'
import { useTheme, type ThemeColor } from './composables/useTheme'

export interface User {
  id?: number
  username?: string
  email?: string
  fullName?: string
  language?: string
  account?: Account
  defaultBoardId?: number
}

export interface Account {
  id: number
  name: string
  brandingColorCode?: string
  createdAt: string

  // twilio info (SID only; auth token is write-only placeholder)
  twilioAccountSid?: string
  twilioAuthToken?: string
  smsEnabled?: boolean

  // new waitlist configuration
  waitlistEnabled?: boolean
  waitlistOpenTime?: string
  waitlistCloseTime?: string
  serviceHours?: Record<string, { openTime?: string; closeTime?: string }>
}

export interface State {
  loginModalMessage: any
  loginModalVisible: any
  token: string | null
  user: User | null
  isAuthenticated: boolean
}

const store = createStore<State>({
  state: {
    token: localStorage.getItem('jwt_token') || null,
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    isAuthenticated: !!localStorage.getItem('jwt_token'),
    // controls for the inline login modal shown when a JWT expires
    loginModalVisible: false,
    loginModalMessage: ''
  },
  mutations: {
    SET_TOKEN(state, token: string) {
      state.token = token
      state.isAuthenticated = true
      localStorage.setItem('jwt_token', token)
    },
    SET_USER(state, user: User) {
      state.user = user
      localStorage.setItem('user', JSON.stringify(user))
      localStorage.removeItem('waitlist-theme')
 
      // Apply account theme color if available
      if (user.account?.brandingColorCode) {
        const { setThemeColor } = useTheme()
        setThemeColor(user.account.brandingColorCode as ThemeColor)
      }
    },
    SET_EMAIL(state, email: string) {
      if (state.user) {
        state.user.email = email
        localStorage.setItem('user', JSON.stringify(state.user))
      }
    },
    LOGOUT(state) {
      state.token = null
      state.user = null
      state.isAuthenticated = false
      localStorage.removeItem('jwt_token')
      localStorage.removeItem('user')
      localStorage.removeItem('waitlist-theme')
      
      // Apply default green theme
      const { setThemeColor } = useTheme()
      setThemeColor('green')
    },
    SHOW_LOGIN_MODAL(state, message: string) {
      state.loginModalVisible = true
      state.loginModalMessage = message
    },
    HIDE_LOGIN_MODAL(state) {
      state.loginModalVisible = false
      state.loginModalMessage = ''
    }
  },
  actions: {
    login({ commit }, { token, user }: { token: string; user?: User }) {
      // logging in should hide any modal that may be open
      commit('HIDE_LOGIN_MODAL')
      commit('SET_TOKEN', token)
      if (user) {
        commit('SET_USER', user)
      }
    },
    logout({ commit }) {
      commit('LOGOUT')
    },
    showLoginModal({ commit }, message: string) {
      commit('SHOW_LOGIN_MODAL', message)
    },
    hideLoginModal({ commit }) {
      commit('HIDE_LOGIN_MODAL')
    }
  },
  getters: {
    isAuthenticated: (state) => state.isAuthenticated,
    token: (state) => state.token,
    user: (state) => state.user,
    loginModalVisible: (state) => state.loginModalVisible,
    loginModalMessage: (state) => state.loginModalMessage
  }
})

export default store
