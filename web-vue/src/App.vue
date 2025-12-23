<template>
  <div id="app">
    <!-- 导航栏 -->
    <nav class="navbar">
      <div class="container">
        <h1 class="logo">智能体创作平台</h1>
        <div class="nav-links">
          <button v-if="currentUser" @click="logout" class="btn btn-secondary">登出</button>
          <div v-else>
            <button @click="showLogin = true; showRegister = false" class="btn btn-primary">登录</button>
            <button @click="showRegister = true; showLogin = false" class="btn btn-outline">注册</button>
          </div>
        </div>
      </div>
    </nav>

    <!-- 主内容区域 -->
    <div class="container main-content">
      <!-- 已登录用户看到的主界面 -->
      <div v-if="currentUser" class="dashboard">
        <!-- 侧边栏导航 -->
        <div class="dashboard-sidebar">
          <ul class="sidebar-nav">
            <li 
              v-for="module in modules" 
              :key="module.id"
              :class="['nav-item', { active: activeModule === module.id }]"
              @click="activeModule = module.id"
            >
              <svg class="nav-icon" :class="module.icon"></svg>
              <span class="nav-text">{{ module.name }}</span>
            </li>
          </ul>
        </div>
        
        <!-- 内容区域 -->
        <div class="dashboard-content">
          <!-- 智能体管理模块 -->
          <div v-if="activeModule === 'agents'" class="module-content">
            <AgentEditor />
          </div>
          
          <!-- 知识库管理模块 -->
          <div v-if="activeModule === 'knowledge-bases'" class="module-content">
            <KnowledgeBase />
          </div>
          
          <!-- 工作流管理模块 -->
          <div v-if="activeModule === 'workflows'" class="module-content">
            <WorkflowDesigner />
          </div>
          
          <!-- 插件管理模块 -->
          <div v-if="activeModule === 'plugins'" class="module-content">
            <PluginManager />
          </div>
          
          <!-- AI模型管理模块 -->
          <div v-if="activeModule === 'models'" class="module-content">
            <ModelManager />
          </div>
        </div>
      </div>
      
      <!-- 未登录用户看到的界面 -->
      <div v-else>
        <TestComponent />
        <LoginForm v-if="showLogin" @login-success="onLoginSuccess" @close="showLogin = false" @show-register="showRegister = true; showLogin = false" />
        <RegisterForm v-else-if="showRegister" @register-success="onRegisterSuccess" @close="showRegister = false" @show-login="showLogin = true; showRegister = false" />
      </div>
    </div>
  </div>
</template>

<script>
import AgentEditor from './components/AgentEditor.vue';
import KnowledgeBase from './components/KnowledgeBase.vue';
import WorkflowDesigner from './components/WorkflowDesigner.vue';
import PluginManager from './components/PluginManager.vue';
import LoginForm from './components/LoginForm.vue';
import RegisterForm from './components/RegisterForm.vue';
import { getToken, logout, fetchCurrentUser } from './auth.js';
import ModelManager from './components/ModelManager.vue';
import TestComponent from './components/TestComponent.vue';

export default {
  name: 'App',
  components: { 
    AgentEditor, 
    KnowledgeBase, 
    WorkflowDesigner, 
    PluginManager, 
    LoginForm, 
    RegisterForm, 
    ModelManager,
    TestComponent
  },
  data() {
    return {
      currentUser: null,
      showLogin: true,
      showRegister: false,
      activeModule: 'agents',
      modules: [
        { id: 'agents', name: '智能体管理', icon: 'icon-agents' },
        { id: 'knowledge-bases', name: '知识库管理', icon: 'icon-knowledge' },
        { id: 'workflows', name: '工作流管理', icon: 'icon-workflows' },
        { id: 'plugins', name: '插件管理', icon: 'icon-plugins' },
        { id: 'models', name: 'AI模型管理', icon: 'icon-models' }
      ]
    };
  },
  async created() {
    const token = getToken();
    if (token) {
      try {
        this.currentUser = await fetchCurrentUser();
      } catch (e) {
        console.error('Failed to fetch current user:', e);
      }
    } else {
      console.log('No token found, showing login form');
    }
    console.log('App created successfully');
  },
  methods: {
    async logout() { 
      await logout(); 
      this.currentUser = null; 
      this.showLogin = true; 
    },
    onLoginSuccess(user) { 
      this.currentUser = user; 
      this.showLogin = false; 
      this.showRegister = false;
    },
    onRegisterSuccess(user) { 
      this.currentUser = user; 
      this.showRegister = false; 
      this.showLogin = false;
    }
  }
};
</script>

<style>
/* 全局样式 */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: 'Arial', sans-serif;
  background-color: #f5f7fa;
  color: #333;
  line-height: 1.6;
}

/* 导航栏样式 */
.navbar {
  background: #2c3e50;
  color: white;
  padding: 1rem 0;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 1rem;
}

.logo {
  font-size: 1.5rem;
  font-weight: bold;
}

.nav-links {
  display: flex;
  gap: 1rem;
  align-items: center;
}

/* 按钮样式 */
.btn {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
  transition: all 0.3s ease;
  text-decoration: none;
  display: inline-block;
}

.btn-primary {
  background: #3498db;
  color: white;
}

.btn-primary:hover {
  background: #2980b9;
}

.btn-secondary {
  background: #95a5a6;
  color: white;
}

.btn-secondary:hover {
  background: #7f8c8d;
}

.btn-outline {
  background: transparent;
  color: white;
  border: 1px solid white;
}

.btn-outline:hover {
  background: rgba(255,255,255,0.1);
}

.btn-danger {
  background: #e74c3c;
  color: white;
}

.btn-danger:hover {
  background: #c0392b;
}

/* 主内容区域 */
.main-content {
  padding: 2rem 0;
}

/* 仪表板样式 */
.dashboard {
  display: flex;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  overflow: hidden;
  min-height: calc(100vh - 120px);
}

/* 侧边栏导航 */
.dashboard-sidebar {
  width: 250px;
  background: #f8f9fa;
  border-right: 1px solid #dee2e6;
}

.sidebar-nav {
  list-style: none;
  padding: 1rem 0;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 1rem 1.5rem;
  cursor: pointer;
  transition: all 0.3s ease;
  border-left: 3px solid transparent;
}

.nav-item:hover {
  background: #e9ecef;
}

.nav-item.active {
  background: white;
  border-left-color: #3498db;
  font-weight: bold;
  box-shadow: 2px 0 5px rgba(0,0,0,0.1);
}

.nav-icon {
  width: 20px;
  height: 20px;
  margin-right: 0.75rem;
  /* 这里可以使用图标字体或SVG图标 */
  background-size: contain;
  background-repeat: no-repeat;
}

/* 图标样式（使用背景色模拟，实际项目中可以使用SVG或图标库） */
.icon-agents { background-color: #3498db; }
.icon-knowledge { background-color: #2ecc71; }
.icon-workflows { background-color: #f39c12; }
.icon-plugins { background-color: #9b59b6; }
.icon-models { background-color: #e74c3c; }

.nav-text {
  font-size: 1rem;
}

/* 模块内容区域 */
.dashboard-content {
  flex: 1;
  padding: 2rem;
  overflow-y: auto;
}

.module-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .dashboard {
    flex-direction: column;
  }
  
  .dashboard-sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #dee2e6;
  }
  
  .sidebar-nav {
    display: flex;
    overflow-x: auto;
    padding: 0;
  }
  
  .nav-item {
    flex-direction: column;
    padding: 0.75rem 0.5rem;
    border-left: none;
    border-bottom: 3px solid transparent;
    min-width: 100px;
  }
  
  .nav-item.active {
    border-left: none;
    border-bottom-color: #3498db;
  }
  
  .nav-icon {
    margin-right: 0;
    margin-bottom: 0.25rem;
  }
}
</style>