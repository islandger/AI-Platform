<template>
  <div class="main-dashboard">
    <div class="dashboard-header">
      <h1>AI智能体平台</h1>
      <div class="header-actions">
        <span>欢迎，{{ currentUser?.display_name || currentUser?.username }}</span>
        <button @click="handleLogout" class="btn btn-secondary">退出登录</button>
      </div>
    </div>
    
    <div class="dashboard-tabs">
      <button 
        v-for="tab in tabs" 
        :key="tab.id"
        :class="['tab-btn', { active: activeTab === tab.id }]"
        @click="activeTab = tab.id"
      >
        {{ tab.name }}
      </button>
    </div>
    
    <div class="dashboard-content">
      <component :is="activeComponent" />
    </div>
  </div>
</template>

<script>
import { logout } from '../auth.js';
import AgentEditor from './AgentEditor.vue';
import KnowledgeBase from './KnowledgeBase.vue';
import WorkflowDesigner from './WorkflowDesigner.vue';
import PluginManager from './PluginManager.vue';
import ModelManager from './ModelManager.vue';

export default {
  name: 'MainDashboard',
  components: {
    AgentEditor,
    KnowledgeBase,
    WorkflowDesigner,
    PluginManager,
    ModelManager
  },
  props: {
    currentUser: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      activeTab: 'agents',
      tabs: [
        { id: 'agents', name: '智能体管理' },
        { id: 'knowledge-bases', name: '知识库管理' },
        { id: 'workflows', name: '工作流管理' },
        { id: 'plugins', name: '插件管理' },
        { id: 'models', name: '模型管理' }
      ]
    };
  },
  computed: {
    activeComponent() {
      switch (this.activeTab) {
        case 'agents': return 'AgentEditor';
        case 'knowledge-bases': return 'KnowledgeBase';
        case 'workflows': return 'WorkflowDesigner';
        case 'plugins': return 'PluginManager';
        case 'models': return 'ModelManager';
        default: return 'AgentEditor';
      }
    }
  },
  methods: {
    async handleLogout() {
      await logout();
      this.$emit('logout');
    }
  }
};
</script>

<style>
.main-dashboard {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #eee;
}

.dashboard-header h1 {
  margin: 0;
  font-size: 2rem;
  color: #2c3e50;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.dashboard-tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 2rem;
  overflow-x: auto;
  padding-bottom: 0.5rem;
}

.tab-btn {
  padding: 0.75rem 1.5rem;
  background: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 4px 4px 0 0;
  cursor: pointer;
  font-size: 1rem;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.tab-btn:hover {
  background: #e9ecef;
}

.tab-btn.active {
  background: white;
  border-bottom-color: white;
  font-weight: bold;
  box-shadow: 0 -2px 0 #3498db inset;
}

.dashboard-content {
  background: white;
  border: 1px solid #dee2e6;
  border-radius: 0 4px 4px 4px;
  padding: 2rem;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  min-height: 500px;
}
</style>