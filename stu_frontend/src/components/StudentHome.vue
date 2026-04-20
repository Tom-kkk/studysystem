<script setup>
import { computed, ref, watch } from 'vue'
import {
  getStudentReportFileUrl,
  getStudentReportList,
  uploadStudentReport,
} from '../services/studentReportApi'

const props = defineProps({
  student: {
    type: Object,
    default: null,
  },
})

const student = computed(() => props.student)
const courseList = ref([])
const loading = ref(false)
const errorMsg = ref('')
const actionMsg = ref('')
const fileMap = ref({})
const progressMap = ref({})
const uploadLoadingMap = ref({})

function rowKey(courseId, projectId) {
  return `${courseId}__${projectId}`
}

async function loadReportData(user) {
  if (!user?.userName) return
  loading.value = true
  errorMsg.value = ''
  try {
    courseList.value = await getStudentReportList(user.userName)
  } catch (e) {
    errorMsg.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function onSelectFile(courseId, projectId, event) {
  const file = event.target.files?.[0]
  if (!file) return
  const key = rowKey(courseId, projectId)
  fileMap.value[key] = file
}

function selectedFileName(courseId, projectId) {
  const key = rowKey(courseId, projectId)
  return fileMap.value[key]?.name || ''
}

function fileInputId(courseId, projectId) {
  return `file_${courseId}_${projectId}`
}

async function onUpload(courseId, project) {
  actionMsg.value = ''
  if (!student.value?.userName) return
  const key = rowKey(courseId, project.projectId)
  const file = fileMap.value[key]
  if (!file) {
    actionMsg.value = `${project.projectName} 文件不可空`
    return
  }
  uploadLoadingMap.value[key] = true
  progressMap.value[key] = 0
  try {
    await uploadStudentReport({
      sno: student.value.userName,
      courseId,
      projectId: project.projectId,
      file,
      onProgress: (percent) => {
        progressMap.value[key] = percent
      },
    })
    actionMsg.value = `${project.projectName} 上传成功`
    await loadReportData(student.value)
    fileMap.value[key] = null
  } catch (e) {
    actionMsg.value = e?.message || '上传失败'
  } finally {
    uploadLoadingMap.value[key] = false
  }
}

function statusClass(status) {
  if (status === 'uploaded') return 'state ok'
  return 'state bad'
}

function getCheckUrl(courseId, project) {
  if (project?.checkUrl) return project.checkUrl
  return getStudentReportFileUrl({
    sno: student.value.userName,
    courseId,
    projectId: project.projectId,
  })
}

function formatDeadline(deadline) {
  if (!deadline) return ''
  return deadline.replace('T', ' ').slice(0, 16)
}

watch(
  student,
  (user) => {
    loadReportData(user)
  },
  { immediate: true },
)
</script>

<template>
  <section class="card student-home">
    <h1>学生界面</h1>
    <p class="welcome">欢迎你，{{ student?.fullName || student?.userName }}（{{ student?.userName }}）</p>
    <p v-if="loading">加载中...</p>
    <p v-else-if="errorMsg" class="error">{{ errorMsg }}</p>
    <p v-if="actionMsg" class="success">{{ actionMsg }}</p>

    <template v-if="!loading && courseList.length">
      <div v-for="course in courseList" :key="course.courseId" class="course-block">
        <p class="course-title"><b>{{ course.courseId }}</b></p>
        <div class="table-head">
          <span>实验项目</span>
          <span>状态</span>
          <span>检查</span>
          <span>文件</span>
          <span>操作</span>
          <span>进度</span>
        </div>
        <div v-for="project in course.projects" :key="project.projectId" class="student-row">
          <div class="project-name">
            <div>{{ project.projectName }}</div>
            <small v-if="project.projectDeadline" class="deadline-tip">
              截止时间：{{ formatDeadline(project.projectDeadline) }}
            </small>
          </div>
          <div :class="statusClass(project.uploadStatus)">{{ project.uploadStatusText }}</div>
          <div class="upload-area">
            <a
              v-if="project.canCheck"
              :href="getCheckUrl(course.courseId, project)"
              target="_blank"
              rel="noreferrer"
            >
              {{ project.checkText || '检查上传' }}
            </a>
            <span v-else>-</span>
          </div>
          <div class="file-cell">
            <input
              :id="fileInputId(course.courseId, project.projectId)"
              class="file-input"
              type="file"
              accept=".pdf,application/pdf"
              :disabled="!project.canUpload"
              @change="onSelectFile(course.courseId, project.projectId, $event)"
            />
            <label
              :for="fileInputId(course.courseId, project.projectId)"
              class="pick-file-btn"
              :class="{ disabled: !project.canUpload }"
            >
              选择文件
            </label>
            <small class="selected-file">
              {{ selectedFileName(course.courseId, project.projectId) || '未选择文件' }}
            </small>
          </div>
          <button
            type="button"
            :disabled="!project.canUpload || uploadLoadingMap[rowKey(course.courseId, project.projectId)]"
            @click="onUpload(course.courseId, project)"
          >
            {{ uploadLoadingMap[rowKey(course.courseId, project.projectId)] ? '上传中...' : '上传' }}
          </button>
          <div class="progress">
            <div class="progress-track">
              <span
                class="progress-fill"
                :style="{ width: `${progressMap[rowKey(course.courseId, project.projectId)] || 0}%` }"
              ></span>
            </div>
            <span class="progress-text">
              {{ progressMap[rowKey(course.courseId, project.projectId)] || 0 }}%
            </span>
          </div>
        </div>
      </div>
    </template>
    <template v-else-if="!loading">
      <p>当前没有可展示的课程。</p>
    </template>
  </section>
</template>

<style scoped>
.student-home {
  max-width: 980px;
}

.course-block {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 14px;
  margin-top: 14px;
  background: #fff;
}

.course-title {
  margin: 0 0 12px;
}

.table-head {
  display: grid;
  grid-template-columns: minmax(180px, 2fr) 86px 90px minmax(220px, 1.3fr) 88px 140px;
  gap: 10px;
  padding: 0 0 8px;
  font-size: 12px;
  color: #64748b;
  border-bottom: 1px solid #e2e8f0;
}

.row-head {
  display: grid;
  grid-template-columns: 160px 80px 90px 1fr 80px 160px;
  gap: 8px;
  color: #6b7280;
  font-size: 12px;
}

.student-row {
  display: grid;
  grid-template-columns: minmax(180px, 2fr) 86px 90px minmax(220px, 1.3fr) 88px 140px;
  gap: 10px;
  align-items: center;
  border-top: 1px dashed #e5e7eb;
  padding-top: 10px;
  margin-top: 10px;
}

.project-name {
  font-weight: 600;
  color: #111827;
  display: grid;
  gap: 2px;
}

.deadline-tip {
  font-size: 12px;
  color: #b45309;
}

.state.ok {
  color: #047857;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 999px;
  padding: 4px 8px;
  text-align: center;
  font-size: 12px;
}

.state.bad {
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 999px;
  padding: 4px 8px;
  text-align: center;
  font-size: 12px;
}

.upload-area {
  text-align: center;
}

.upload-area a {
  color: #2563eb;
  text-decoration: none;
  font-weight: 600;
}

.file-cell {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.pick-file-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 96px;
  height: 34px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.pick-file-btn:hover {
  border-color: #2563eb;
  color: #1d4ed8;
  background: #eff6ff;
}

.pick-file-btn.disabled {
  cursor: not-allowed;
  background: #f3f4f6;
  color: #9ca3af;
  border-color: #e5e7eb;
}

.selected-file {
  color: #475569;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 0 8px;
  height: 34px;
  line-height: 34px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 12px;
}

button {
  height: 34px;
  border: none;
  border-radius: 8px;
  background: #2563eb;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

button:disabled {
  background: #93c5fd;
  cursor: not-allowed;
}

.progress {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-track {
  flex: 1;
  min-width: 0;
  height: 10px;
  background: #e5e7eb;
  border-radius: 999px;
  overflow: hidden;
}

.progress-fill {
  display: block;
  height: 100%;
  background: #22c55e;
  border-radius: 999px;
  transition: width 0.2s ease;
}

.progress-text {
  width: 38px;
  text-align: right;
  color: #475569;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 900px) {
  .table-head {
    display: none;
  }

  .student-row {
    grid-template-columns: 1fr;
    gap: 8px;
    padding-top: 12px;
  }

  .upload-area {
    text-align: left;
  }
}
</style>

