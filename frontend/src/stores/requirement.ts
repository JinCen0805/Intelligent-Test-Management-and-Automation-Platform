import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/api'
import type { ApiRequirement } from '@/api'
import type { Requirement, RequirementDetail } from '@/types'
import { useUiStore } from './ui'
import { useProjectStore } from './project'

export const useRequirementStore = defineStore('requirement', () => {
  const ui = useUiStore()
  const projectStore = useProjectStore()

  const requirements = ref<Requirement[]>([])
  const requirementDetail = ref<RequirementDetail | null>(null)
  const requirementFile = ref<File | null>(null)
  const requirementVersion = ref('V1.0')

  const selectedRequirementId = computed(() => {
    return requirements.value.find((item) => item.id)?.id ?? null
  })

  function fromApiRequirement(requirement: ApiRequirement): Requirement {
    const typeMap: Record<string, string> = {
      PDF: 'PDF',
      DOCX: 'DOCX',
      MD: 'Markdown',
      MARKDOWN: 'Markdown',
      TXT: 'TXT'
    }
    const statusMap: Record<string, Requirement['status']> = {
      STORED: '已存储',
      PARSED: '已存储',
      FAILED: '解析失败',
      PARSING: '解析中'
    }
    return {
      id: requirement.id,
      name: requirement.name,
      version: requirement.version,
      type: typeMap[requirement.fileType] || requirement.fileType,
      status: statusMap[requirement.parseStatus] || '已存储',
      updated: new Date(requirement.updatedAt).toLocaleString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    }
  }

  async function loadRequirements() {
    const projectId = projectStore.currentProject.id
    if (!projectId) return
    const apiRequirements = await api.requirements.list(projectId)
    requirements.value.splice(0, requirements.value.length, ...apiRequirements.map(fromApiRequirement))
  }

  function handleRequirementFile(event: Event) {
    const input = event.target as HTMLInputElement
    requirementFile.value = input.files?.[0] ?? null
  }

  async function uploadRequirement(options: { keepModal?: boolean } = {}) {
    const projectId = projectStore.currentProject.id
    if (!projectId) {
      ui.showToast('当前项目尚未加载')
      return
    }
    if (!requirementFile.value) {
      ui.showToast('请选择项目文件')
      return
    }
    try {
      const uploaded = await api.requirements.upload(projectId, requirementFile.value, requirementVersion.value)
      requirements.value.unshift(fromApiRequirement(uploaded))
      requirementFile.value = null
      requirementVersion.value = 'V1.0'
      if (!options.keepModal) {
        ui.closeModal()
        ui.activeProjectTab = 'requirements'
      }
      ui.showToast('文件已保存到项目文件')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '文件上传失败')
    }
  }

  async function parseRequirement(requirement: Requirement) {
    const projectId = projectStore.currentProject.id
    if (!projectId || !requirement.id) return
    try {
      await api.requirements.parse(projectId, requirement.id)
      await loadRequirements()
      ui.showToast('文件已更新')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '文件处理失败')
    }
  }

  async function openRequirementDetail(requirement: Requirement) {
    const projectId = projectStore.currentProject.id
    if (!projectId || !requirement.id) return
    try {
      const detail = await api.requirements.detail(projectId, requirement.id)
      requirementDetail.value = {
        name: detail.name,
        version: detail.version,
        fileType: detail.fileType,
        parseStatus: detail.parseStatus,
        parseError: detail.parseError,
        contentText: detail.contentText
      }
      ui.openModal('requirementDetail')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '文件信息加载失败')
    }
  }

  return {
    requirements,
    requirementDetail,
    requirementFile,
    requirementVersion,
    selectedRequirementId,
    loadRequirements,
    handleRequirementFile,
    uploadRequirement,
    parseRequirement,
    openRequirementDetail
  }
})
