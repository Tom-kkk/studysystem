<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import TeacherHome from '../components/TeacherHome.vue'
import { clearLoginUser, loginUser } from '../stores/auth'

const router = useRouter()
const teacher = computed(() => loginUser.value)

function handleLogout() {
  clearLoginUser()
  router.push('/teacher/login')
}
</script>

<template>
  <main class="page">
    <section class="page-shell teacher-page-shell">
      <header class="top-nav">
        <nav class="links">
          <router-link to="/teacher/home">教师首页</router-link>
          <router-link to="/teacher/course/new">新建课程</router-link>
          <router-link to="/teacher/login">登录页</router-link>
        </nav>
        <button type="button" class="logout-btn" @click="handleLogout">安全退出</button>
      </header>

      <TeacherHome :teacher="teacher" />
    </section>
  </main>
</template>

<style scoped>
.teacher-page-shell {
  max-width: 1080px;
}
</style>

