import { createRouter, createWebHistory } from 'vue-router'
import TeacherLoginPage from '../views/TeacherLoginPage.vue'
import TeacherHomePage from '../views/TeacherHomePage.vue'
import CreateCoursePage from '../views/CreateCoursePage.vue'
import { loginUser } from '../stores/auth'
import StudentLoginPage from '../views/StudentLoginPage.vue'
import StudentHomePage from '../views/StudentHomePage.vue'
import StudentForgetPasswordPage from '../views/StudentForgetPasswordPage.vue'
import { studentLoginUser } from '../stores/studentAuth'

const routes = [
  {
    path: '/',
    redirect: '/teacher/login',
  },
  {
    path: '/teacher/login',
    name: 'teacher-login',
    component: TeacherLoginPage,
  },
  {
    path: '/teacher/home',
    name: 'teacher-home',
    component: TeacherHomePage,
    meta: { requiresAuth: true },
  },
  {
    path: '/teacher/course/new',
    name: 'teacher-course-new',
    component: CreateCoursePage,
    meta: { requiresAuth: true },
  },
  {
    path: '/student/login',
    name: 'student-login',
    component: StudentLoginPage,
  },
  {
    path: '/student/home',
    name: 'student-home',
    component: StudentHomePage,
    meta: { requiresStudentAuth: true },
  },
  {
    path: '/student/forget-password',
    name: 'student-forget-password',
    component: StudentForgetPasswordPage,
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !loginUser.value) {
    return { name: 'teacher-login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresStudentAuth && !studentLoginUser.value) {
    return { name: 'student-login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'teacher-login' && loginUser.value) {
    return { name: 'teacher-home' }
  }
  if (to.name === 'student-login' && studentLoginUser.value) {
    return { name: 'student-home' }
  }
  return true
})

export default router

