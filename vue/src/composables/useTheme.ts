import { ref } from 'vue'

export type ThemeColor = 'green' | 'burnt-orange' | 'sky-blue'

const THEME_STORAGE_KEY = 'waitlist-theme'

// Load theme from localStorage or default to green
const loadTheme = (): ThemeColor => {
  const stored = localStorage.getItem(THEME_STORAGE_KEY)
  return (stored === 'burnt-orange' || stored === 'green' || stored === 'sky-blue') ? stored : 'green'
}

const themeColor = ref<ThemeColor>(loadTheme())

export const useTheme = () => {
  const getThemeColor = () => themeColor.value
  
  const setThemeColor = (color: ThemeColor) => {
    themeColor.value = color
    localStorage.setItem(THEME_STORAGE_KEY, color)
    document.documentElement.setAttribute('data-theme', color)
  }

  // Initialize theme on first load
  const initTheme = () => {
    document.documentElement.setAttribute('data-theme', themeColor.value)
  }

  return {
    themeColor,
    getThemeColor,
    setThemeColor,
    initTheme
  }
}
