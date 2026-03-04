import { createRouter, createWebHistory } from 'vue-router'
import Home from './pages/Home.vue'
import Account from './pages/Account.vue'
import Register from './pages/Register.vue'
import Login from './pages/Login.vue'
import ResetPassword from './pages/ResetPassword.vue'
import Waitlist from './pages/Waitlist.vue'
import store from './store'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/waitlist/:action/:code?',
    name: 'Waitlist',
    component: Waitlist
  },
  {
    path: '/account',
    name: 'Account',
    component: Account,
    children: [
      {
        path: 'account',
        name: 'AccountTab',
        component: Account
      },
      {
        path: 'profile',
        name: 'ProfileTab',
        component: Account
      },
      {
        path: 'branding',
        name: 'BrandingTab',
        component: Account
      },
      {
        path: 'users',
        name: 'UsersTab',
        component: Account
      },
      {
        path: 'data',
        name: 'DataTab',
        component: Account
      },
      {
        path: 'board',
        name: 'BoardTab',
        component: Account
      },
      {
        path: 'waitlist',
        name: 'WaitlistTab',
        component: Account
      },
      {
        path: 'messages',
        name: 'MessagesTab',
        component: Account
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/register/',
    name: 'Register',
    component: Register
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: ResetPassword
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// redirect already-logged-in users away from the login page; they should
// always end up on their profile instead of seeing the login form again.
router.beforeEach((to, from, next) => {
  if (to.path === '/login' && store.getters.token) {
    next('/account/profile')
  } else {
    next()
  }
})

export default router
