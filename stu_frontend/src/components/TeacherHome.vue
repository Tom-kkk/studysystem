<script setup>
import { computed, ref, watch } from 'vue'
import {
  getProjectsByCourse,
  getTeacherCourses,
  notifyProjectStudents,
  updateProjectStatus,
} from '../services/courseApi'
import {
  getTeacherReportExportUrl,
  getTeacherReportFileUrl,
  getTeacherReportList,
  updateTeacherReportGrade,
} from '../services/teacherReportApi'

const props = defineProps({
  teacher: {
    type: Object,
    default: null,
  },
})
const teacher = computed(() => props.teacher)

const courses = ref([])
const projectsByCourse = ref({})
const loading = ref(false)
const errorMsg = ref('')
const actionMsg = ref('')
const actionMsgType = ref('success')
let actionMsgTimer = null
const notifyingMap = ref({})

const selectedCourseId = ref('')
const selectedProjectId = ref('')
const reportRows = ref([])
const reportLoading = ref(false)
const reportErrorMsg = ref('')
const reportMsg = ref('')
const gradeDraftMap = ref({})
const gradeSavingMap = ref({})
const onlyUngraded = ref(false)
const statusDialog = ref({
  visible: false,
  loading: false,
  courseId: '',
  status: '',
  deadlineAt: '',
  project: null,
})

function availableProjects() {
  return projectsByCourse.value[selectedCourseId.value] || []
}

function ensureReportSelection() {
  const courseExists = courses.value.some((course) => course.courseId === selectedCourseId.value)
  if (!courseExists) {
    selectedCourseId.value = courses.value[0]?.courseId || ''
  }
  const projects = availableProjects()
  const projectExists = projects.some((project) => project.projectId === selectedProjectId.value)
  if (!projectExists) {
    selectedProjectId.value = projects[0]?.projectId || ''
  }
}

async function loadCourses(currentTeacher) {
  if (!currentTeacher?.userName) return
  loading.value = true
  errorMsg.value = ''
  reportRows.value = []
  reportMsg.value = ''
  reportErrorMsg.value = ''
  try {
    courses.value = await getTeacherCourses(currentTeacher.userName)
    const entries = await Promise.all(
      courses.value.map(async (course) => [course.courseId, await getProjectsByCourse(course.courseId)]),
    )
    projectsByCourse.value = Object.fromEntries(entries)
    ensureReportSelection()
  } catch (e) {
    errorMsg.value = e?.message || '课程加载失败'
  } finally {
    loading.value = false
  }
}

function statusText(status) {
  if (status === 1) return '已经开放'
  if (status === 2) return '已截止'
  return '未开始'
}

function formatDeadline(deadline) {
  if (!deadline) return ''
  return deadline.replace('T', ' ').slice(0, 16)
}

function isDeadline(status) {
  return status === 2
}

function uploadedCount() {
  return reportRows.value.filter((row) => row.uploaded).length
}

function gradedCount() {
  return reportRows.value.filter((row) => (row.grade || '').trim()).length
}

function visibleReportRows() {
  if (!onlyUngraded.value) return reportRows.value
  return reportRows.value.filter((row) => !(row.grade || '').trim())
}

function reportStatusClass(uploaded) {
  return uploaded ? 'report-state ok' : 'report-state bad'
}

function reportFileUrl(sno) {
  return getTeacherReportFileUrl({
    teacherId: teacher.value.userName,
    courseId: selectedCourseId.value,
    projectId: selectedProjectId.value,
    sno,
  })
}

function gradeDraftValue(sno) {
  return gradeDraftMap.value[sno] ?? ''
}

function onGradeInput(sno, value) {
  gradeDraftMap.value[sno] = value
}

function canSaveGrade(row) {
  if (!teacher.value?.userName || !selectedCourseId.value) return false
  return gradeDraftValue(row.sno) !== (row.grade || '')
}

async function onSaveGrade(row) {
  const sno = row.sno
  if (!canSaveGrade(row) || gradeSavingMap.value[sno]) return
  gradeSavingMap.value[sno] = true
  try {
    const grade = gradeDraftValue(sno)
    await updateTeacherReportGrade({
      teacherId: teacher.value.userName,
      courseId: selectedCourseId.value,
      sno,
      grade,
    })
    row.grade = grade
    showActionToast(`${sno} 成绩已保存`, 'success')
    reportMsg.value = `共 ${reportRows.value.length} 人，已上传 ${uploadedCount()} 人，已评分 ${gradedCount()} 人`
  } catch (e) {
    showActionToast(e?.message || '保存成绩失败', 'error')
  } finally {
    gradeSavingMap.value[sno] = false
  }
}

function exportReportList() {
  reportErrorMsg.value = ''
  if (!teacher.value?.userName) return
  if (!selectedCourseId.value || !selectedProjectId.value) {
    reportErrorMsg.value = '请先选择课程和实验项目'
    return
  }
  const url = getTeacherReportExportUrl({
    teacherId: teacher.value.userName,
    courseId: selectedCourseId.value,
    projectId: selectedProjectId.value,
  })
  window.open(url, '_blank')
}

function toIsoLocalDatetime(value) {
  if (!value) return ''
  return `${value}:00`
}

function notifyKey(courseId, projectId) {
  return `${courseId}__${projectId}`
}

async function onChangeProjectStatus(courseId, project, status, deadlineAt = '') {
  actionMsg.value = ''
  if (project.projectOpen === 2) return
  try {
    await updateProjectStatus({
      courseId,
      projectId: project.projectId,
      status,
      deadlineAt: status === 'open' ? toIsoLocalDatetime(deadlineAt) : undefined,
    })
    if (status === 'open') {
      project.projectOpen = 1
      project.projectDeadline = toIsoLocalDatetime(deadlineAt)
    }
    if (status === 'close') {
      project.projectOpen = 0
      project.projectDeadline = null
    }
    if (status === 'deadline') {
      project.projectOpen = 2
      project.projectDeadline = null
    }
    showActionToast(`${project.projectId} 状态已更新`, 'success')
  } catch (e) {
    showActionToast(e?.message || '状态更新失败', 'error')
  }
}

async function onNotifyStudents(courseId, project) {
  const key = notifyKey(courseId, project.projectId)
  if (notifyingMap.value[key]) return
  notifyingMap.value[key] = true
  try {
    const res = await notifyProjectStudents({ courseId, projectId: project.projectId })
    const count = res?.sentCount ?? 0
    showActionToast(`${project.projectId} 通知完成（${count} 封）`, 'success')
  } catch (e) {
    showActionToast(e?.message || '发送通知失败', 'error')
  } finally {
    notifyingMap.value[key] = false
  }
}

function showActionToast(message, type = 'success') {
  actionMsg.value = message
  actionMsgType.value = type
  if (actionMsgTimer) {
    clearTimeout(actionMsgTimer)
  }
  actionMsgTimer = setTimeout(() => {
    actionMsg.value = ''
  }, 2200)
}

function requestChangeProjectStatus(courseId, project, status) {
  if (project.projectOpen === 2) return
  const targetStatus = status === 'open' ? 1 : status === 'close' ? 0 : 2
  if (project.projectOpen === targetStatus) return
  statusDialog.value = {
    visible: true,
    loading: false,
    courseId,
    status,
    deadlineAt: '',
    project,
  }
}

function closeStatusDialog() {
  if (statusDialog.value.loading) return
  statusDialog.value.visible = false
}

function statusActionText(status) {
  if (status === 'open') return '开放'
  if (status === 'close') return '关闭'
  if (status === 'deadline') return '截止'
  return '修改'
}

function statusDialogMessage() {
  const projectId = statusDialog.value.project?.projectId || '-'
  if (statusDialog.value.status === 'deadline') {
    return `确认将 ${projectId} 设为“截止”？该操作之后不可再修改。`
  }
  return `确认将 ${projectId} 设为“${statusActionText(statusDialog.value.status)}”？`
}

function requireDeadlineInput() {
  return statusDialog.value.status === 'open'
}

async function confirmStatusDialog() {
  if (!statusDialog.value.project) return
  statusDialog.value.loading = true
  try {
    if (requireDeadlineInput() && !statusDialog.value.deadlineAt) {
      showActionToast('开放项目时请设置截止时间', 'error')
      return
    }
    await onChangeProjectStatus(
      statusDialog.value.courseId,
      statusDialog.value.project,
      statusDialog.value.status,
      statusDialog.value.deadlineAt,
    )
    statusDialog.value.visible = false
  } finally {
    statusDialog.value.loading = false
  }
}

async function onQueryReportRows() {
  reportMsg.value = ''
  reportErrorMsg.value = ''
  if (!teacher.value?.userName) return
  if (!selectedCourseId.value || !selectedProjectId.value) {
    reportErrorMsg.value = '请先选择课程和实验项目'
    return
  }

  reportLoading.value = true
  try {
    reportRows.value = await getTeacherReportList({
      teacherId: teacher.value.userName,
      courseId: selectedCourseId.value,
      projectId: selectedProjectId.value,
    })
    gradeDraftMap.value = Object.fromEntries(
      reportRows.value.map((row) => [row.sno, row.grade || '']),
    )
    reportMsg.value = `共 ${reportRows.value.length} 人，已上传 ${uploadedCount()} 人，已评分 ${gradedCount()} 人`
  } catch (e) {
    reportErrorMsg.value = e?.message || '查询上传情况失败'
  } finally {
    reportLoading.value = false
  }
}

watch(
  teacher,
  (currentTeacher) => {
    loadCourses(currentTeacher)
  },
  { immediate: true },
)

watch(selectedCourseId, () => {
  ensureReportSelection()
})
</script>

<template>
  <section class="card teacher-home">
    <h1>教师界面</h1>
    <p class="welcome">
      欢迎你，{{ teacher?.fullName || teacher?.userName }}（{{ teacher?.userName }}）
    </p>
    <p v-if="loading">课程加载中...</p>
    <p v-else-if="errorMsg" class="error">{{ errorMsg }}</p>
    <template v-else-if="courses.length">
      <p>你当前管理的课程与项目：</p>
      <div v-for="course in courses" :key="course.courseId" class="course-block">
        <p><b>{{ course.courseId }}</b>（项目数：{{ course.numOfProject ?? '-' }}）</p>
        <ul v-if="projectsByCourse[course.courseId]?.length">
          <li v-for="project in projectsByCourse[course.courseId]" :key="project.projectId">
            {{ project.projectId }}：{{ statusText(project.projectOpen) }}
            <span v-if="project.projectDeadline" class="deadline-tip">
              （截止时间：{{ formatDeadline(project.projectDeadline) }}）
            </span>
            <label>
              <input
                type="radio"
                :name="`${course.courseId}-${project.projectId}`"
                :checked="project.projectOpen === 1"
                :disabled="isDeadline(project.projectOpen)"
                @click.prevent="requestChangeProjectStatus(course.courseId, project, 'open')"
              />
              开放
            </label>
            <label>
              <input
                type="radio"
                :name="`${course.courseId}-${project.projectId}`"
                :checked="project.projectOpen === 0"
                :disabled="isDeadline(project.projectOpen)"
                @click.prevent="requestChangeProjectStatus(course.courseId, project, 'close')"
              />
              关闭
            </label>
            <label>
              <input
                type="radio"
                :name="`${course.courseId}-${project.projectId}`"
                :checked="project.projectOpen === 2"
                :disabled="isDeadline(project.projectOpen)"
                @click.prevent="requestChangeProjectStatus(course.courseId, project, 'deadline')"
              />
              截止
            </label>
            <button
              type="button"
              class="notify-btn"
              :disabled="notifyingMap[notifyKey(course.courseId, project.projectId)]"
              @click="onNotifyStudents(course.courseId, project)"
            >
              {{ notifyingMap[notifyKey(course.courseId, project.projectId)] ? '通知中...' : '通知学生' }}
            </button>
          </li>
        </ul>
      </div>

      <div class="report-check-card">
        <h3>实验报告查看</h3>
        <div class="report-filters">
          <label>
            课程
            <select v-model="selectedCourseId">
              <option v-for="course in courses" :key="course.courseId" :value="course.courseId">
                {{ course.courseId }}
              </option>
            </select>
          </label>
          <label>
            项目
            <select v-model="selectedProjectId">
              <option
                v-for="project in availableProjects()"
                :key="project.projectId"
                :value="project.projectId"
              >
                {{ project.projectId }} - {{ project.projectName }}
              </option>
            </select>
          </label>
          <button type="button" class="query-btn" :disabled="reportLoading" @click="onQueryReportRows">
            {{ reportLoading ? '查询中...' : '查询上传情况' }}
          </button>
          <button type="button" class="query-btn secondary-btn" :disabled="reportLoading" @click="exportReportList">
            导出名单
          </button>
        </div>

        <p v-if="reportMsg" class="report-msg">{{ reportMsg }}</p>
        <p v-if="reportErrorMsg" class="error">{{ reportErrorMsg }}</p>
        <label v-if="reportRows.length" class="ungraded-switch">
          <input v-model="onlyUngraded" type="checkbox" />
          仅查看未评分
        </label>

        <div v-if="reportRows.length" class="report-table">
          <div class="report-head">
            <span>学号</span>
            <span>姓名</span>
            <span>状态</span>
            <span>文件名</span>
            <span>成绩</span>
            <span>操作</span>
          </div>
          <div v-for="row in visibleReportRows()" :key="row.sno" class="report-row">
            <span>{{ row.sno }}</span>
            <span>{{ row.studentName || '-' }}</span>
            <span :class="reportStatusClass(row.uploaded)">
              {{ row.uploaded ? '已上传' : '未上传' }}
            </span>
            <span>{{ row.fileName || '-' }}</span>
            <span class="grade-cell">
              <input
                :value="gradeDraftValue(row.sno)"
                type="text"
                maxlength="50"
                placeholder="例如：88 / A- / 不及格"
                @input="onGradeInput(row.sno, $event.target.value)"
              />
            </span>
            <span class="actions-cell">
              <button
                type="button"
                class="query-btn action-btn"
                :disabled="gradeSavingMap[row.sno] || !canSaveGrade(row)"
                @click="onSaveGrade(row)"
              >
                {{ gradeSavingMap[row.sno] ? '保存中...' : '保存成绩' }}
              </button>
              <a
                v-if="row.uploaded"
                :href="reportFileUrl(row.sno)"
                target="_blank"
                rel="noreferrer"
                class="query-btn secondary-btn action-btn action-link-btn"
              >
                查看报告
              </a>
              <span v-else class="action-placeholder">-</span>
            </span>
          </div>
        </div>
      </div>
    </template>
    <template v-else>
      <p>现在还没有课程，先去新建课程并上传教学班 Excel。</p>
      <router-link class="primary-link" to="/teacher/course/new">进入“新建课程”</router-link>
    </template>

    <div v-if="statusDialog.visible" class="modal-mask" @click.self="closeStatusDialog">
      <div class="modal-card" role="dialog" aria-modal="true" aria-label="状态确认">
        <h4>确认状态变更</h4>
        <p>{{ statusDialogMessage() }}</p>
        <label v-if="requireDeadlineInput()" class="deadline-field">
          <span>截止时间</span>
          <input v-model="statusDialog.deadlineAt" type="datetime-local" />
        </label>
        <div class="modal-actions">
          <button type="button" class="query-btn secondary-btn" :disabled="statusDialog.loading" @click="closeStatusDialog">
            取消
          </button>
          <button type="button" class="query-btn" :disabled="statusDialog.loading" @click="confirmStatusDialog">
            {{ statusDialog.loading ? '提交中...' : '确认' }}
          </button>
        </div>
      </div>
    </div>

    <transition name="toast-fade">
      <div v-if="actionMsg" class="toast" :class="actionMsgType === 'error' ? 'toast-error' : 'toast-success'">
        {{ actionMsg }}
      </div>
    </transition>
  </section>
</template>

<style scoped>
.teacher-home {
  width: 100%;
  max-width: 1080px;
}

.report-check-card {
  margin-top: 18px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px;
  background: #f8fafc;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.report-check-card h3 {
  margin: 0 0 12px;
}

.report-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  gap: 10px;
}

.report-filters label {
  display: grid;
  gap: 4px;
  font-size: 13px;
}

.report-filters select {
  min-width: 180px;
  height: 34px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0 8px;
  background: #fff;
}

.query-btn {
  height: 34px;
}

.secondary-btn {
  background: #f8fafc;
  color: #0f172a;
  border: 1px solid #cbd5e1;
}

.deadline-tip {
  margin-left: 8px;
  color: #b45309;
  font-size: 13px;
}

.notify-btn {
  margin-left: 10px;
  height: 28px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #1d4ed8;
  cursor: pointer;
  padding: 0 10px;
}

.notify-btn:disabled {
  cursor: not-allowed;
  color: #94a3b8;
  border-color: #e2e8f0;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}

.modal-card {
  width: min(460px, calc(100vw - 24px));
  border-radius: 12px;
  background: #fff;
  padding: 16px;
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.2);
}

.modal-card h4 {
  margin: 0;
}

.modal-card p {
  margin: 10px 0 0;
  color: #334155;
}

.deadline-field {
  margin-top: 12px;
  display: grid;
  gap: 6px;
}

.deadline-field span {
  font-size: 13px;
  color: #334155;
}

.deadline-field input {
  height: 34px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0 8px;
}

.modal-actions {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.toast {
  position: fixed;
  right: 18px;
  bottom: 18px;
  z-index: 60;
  padding: 10px 14px;
  border-radius: 10px;
  color: #fff;
  font-size: 13px;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.22);
}

.toast-success {
  background: #16a34a;
}

.toast-error {
  background: #dc2626;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.2s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.report-msg {
  margin: 10px 0 0;
  color: #0369a1;
}

.report-table {
  margin-top: 12px;
}

.report-head,
.report-row {
  display: grid;
  grid-template-columns: 100px 86px 90px minmax(180px, 1.5fr) 140px minmax(210px, 1.1fr);
  gap: 10px;
  align-items: center;
  justify-items: center;
  text-align: center;
}

.report-head {
  color: #64748b;
  font-size: 12px;
  border-bottom: 1px solid #dbeafe;
  padding-bottom: 8px;
}

.report-row {
  border-bottom: 1px dashed #e2e8f0;
  padding: 10px 0;
}

.report-row > span {
  width: 100%;
}

.report-row > span:nth-child(4) {
  word-break: break-all;
}

.grade-cell input {
  width: 140px;
  max-width: 100%;
  height: 32px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0 8px;
  background: #fff;
}

.actions-cell .query-btn {
  width: auto;
  min-width: 84px;
}

.actions-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: nowrap;
  gap: 6px;
  min-width: 190px;
}

.actions-cell a,
.actions-cell span {
  white-space: nowrap;
}

.action-btn {
  height: 28px;
  padding: 0 12px;
  font-size: 12px;
  border-radius: 8px;
  line-height: 28px;
  white-space: nowrap;
  margin-top: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.action-link-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
}

.action-placeholder {
  color: #94a3b8;
  font-size: 12px;
}

.ungraded-switch {
  margin-top: 10px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #334155;
  font-size: 13px;
}

.report-state {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 12px;
  width: 70px;
}

.report-state.ok {
  color: #047857;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
}

.report-state.bad {
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
}

@media (max-width: 900px) {
  .report-head {
    display: none;
  }

  .report-row {
    grid-template-columns: 1fr;
    gap: 6px;
  }
}
</style>

