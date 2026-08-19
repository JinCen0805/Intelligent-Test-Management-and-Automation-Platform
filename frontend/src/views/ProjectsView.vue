<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useProjectStore } from '@/stores/project'

const ui = useUiStore()
const projectStore = useProjectStore()

const { openModal } = ui
const { visibleProjects } = storeToRefs(projectStore)
const { openProject } = projectStore
</script>

<template>
  <section class="view">
    <div class="page-head"><div><h1>项目</h1><p>以项目为中心统一管理需求、用例、自动化、执行与报告。</p></div><div class="head-actions"><button class="btn primary" @click="openModal('project')">＋ 创建项目</button></div></div>
    <div class="card"><div class="card-body table-wrap"><table><thead><tr><th>项目名称</th><th>负责人</th><th>成员</th><th>状态</th><th></th></tr></thead><tbody><tr v-for="project in visibleProjects" :key="project.code"><td><div class="project-title"><div class="project-logo">{{ project.name.slice(0, 1) }}</div><div><strong>{{ project.name }}</strong><p>{{ project.code }} · {{ project.description }}</p></div></div></td><td>{{ project.owner }}</td><td>{{ project.members }} 人</td><td><span class="tag" :class="project.status === '运行中' ? 'green' : 'orange'">{{ project.status }}</span></td><td><button class="btn" @click="openProject(project)">打开</button></td></tr></tbody></table></div></div>
  </section>
</template>
