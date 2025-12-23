<template>
  <div class="p-4 md:p-6 animate-fade-in">
    <h2 class="text-2xl font-bold text-blue-700 mb-4">智能体编辑</h2>
    <p class="text-gray-600 mb-6 text-sm md:text-base">创建、编辑和管理智能体，配置提示词、大模型、插件和知识库。</p>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- 智能体列表 -->
      <div class="lg:col-span-1 animate-slide-up">
        <h3 class="text-xl font-semibold text-blue-700 mb-4">智能体列表</h3>
        <div v-if="loadingAgents" class="text-center py-6 bg-white rounded-xl border border-gray-200 shadow-card">
          <svg class="animate-spin mx-auto h-8 w-8 text-blue-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <p class="mt-2 text-sm text-gray-500">加载智能体列表...</p>
        </div>
        <div v-else-if="agents.length === 0" class="text-center py-6 bg-white rounded-xl border border-gray-200 shadow-card text-gray-500">
          暂无智能体
        </div>
        <div v-else class="space-y-4">
          <div 
            v-for="agent in agents" 
            :key="agent.id" 
            class="border border-gray-200 rounded-xl p-4 bg-white shadow-card hover:shadow-card-hover transition-all duration-300"
          >
            <div class="flex flex-col space-y-2">
              <div class="flex items-center justify-between">
                <strong class="text-lg text-blue-700">{{ agent.name }}</strong>
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium" :class="getStatusBadgeClass(agent.status)">
                  {{ agent.status }}
                </span>
              </div>
              <div class="text-sm text-gray-600">{{ agent.description }}</div>
              <div class="flex gap-2 mt-3">
                <button @click="editAgent(agent)" class="px-3 py-1 bg-blue-100 text-blue-800 rounded-lg hover:bg-blue-200 transition-colors text-sm">
                  编辑
                </button>
                <button @click="deleteAgent(agent.id)" class="px-3 py-1 bg-red-100 text-red-800 rounded-lg hover:bg-red-200 transition-colors text-sm">
                  删除
                </button>
              </div>
            </div>
          </div>
        </div>
        <button @click="createAgent" class="w-full mt-4 px-4 py-2 bg-gradient-to-r from-blue-500 to-blue-600 text-white rounded-lg hover:shadow-button-hover transition-all duration-300 focus:outline-none focus:ring-2 focus:ring-blue-500 shadow-button transform hover:-translate-y-0.5">
          <svg class="w-4 h-4 mr-2 inline" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
          </svg>
          创建新智能体
        </button>
      </div>

      <!-- 智能体编辑器 -->
      <div class="lg:col-span-2 animate-slide-up" style="animation-delay: 0.2s;">
        <h3 class="text-xl font-semibold text-blue-700 mb-4">
          {{ currentAgent && currentAgent.id ? '编辑智能体' : '创建新智能体' }}
        </h3>
        <div v-if="currentAgent" class="bg-white p-4 rounded-xl border border-gray-200 shadow-card hover:shadow-card-hover transition-all duration-300">
          <form @submit.prevent="saveAgent">
            <div class="space-y-4">
              <!-- 基本信息 -->
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">名称</label>
                  <input 
                    type="text" 
                    v-model="currentAgent.name" 
                    required 
                    class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                  />
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">描述</label>
                  <input 
                    type="text" 
                    v-model="currentAgent.description" 
                    required 
                    class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                  />
                </div>
              </div>

              <!-- 大模型配置 -->
              <div>
                <h4 class="text-lg font-medium text-blue-700 mb-3">大模型配置</h4>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">模型提供商</label>
                    <select 
                      v-model="currentAgent.llm_provider" 
                      required 
                      class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                    >
                      <option value="">请选择提供商</option>
                      <option value="deepseek">DeepSeek</option>
                      <option value="doubao">豆包</option>
                      <option value="tongyi">通义千问</option>
                      <option value="wenxin">文心一言</option>
                    </select>
                  </div>
                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">模型名称</label>
                    <input 
                      type="text" 
                      v-model="currentAgent.llm_model" 
                      required 
                      class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                      placeholder="如: deepseek-chat"
                    />
                  </div>
                </div>
                <div class="mt-3">
                  <label class="block text-sm font-medium text-gray-700 mb-1">模型配置（JSON）</label>
                  <textarea 
                    v-model="llmConfigStr" 
                    rows="4" 
                    class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 font-mono text-sm transition-all duration-300"
                    placeholder='{"temperature": 0.7, "max_tokens": 2000}'
                  ></textarea>
                </div>
              </div>

              <!-- 提示词配置 -->
              <div>
                <h4 class="text-lg font-medium text-blue-700 mb-3">提示词配置</h4>
                <div class="space-y-3">
                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">系统提示词</label>
                    <textarea 
                      v-model="currentAgent.system_prompt" 
                      rows="5" 
                      required 
                      class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                      placeholder="定义智能体的角色、能力、行为规范..."
                    ></textarea>
                  </div>
                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">用户提示词模板</label>
                    <textarea 
                      v-model="currentAgent.user_prompt_template" 
                      rows="3" 
                      required 
                      class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                      placeholder="支持变量替换，如: {{userInput}}、{{context}}..."
                    ></textarea>
                  </div>
                </div>
              </div>

              <!-- 插件和知识库关联 -->
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <h4 class="text-lg font-medium text-blue-700 mb-3">关联插件</h4>
                  <div class="space-y-2 max-h-40 overflow-y-auto">
                    <div v-if="loadingPlugins" class="text-center py-3 text-sm text-gray-500">
                      加载插件列表...
                    </div>
                    <div v-else-if="plugins.length === 0" class="text-center py-3 text-sm text-gray-500">
                      暂无可用插件
                    </div>
                    <label v-else 
                      v-for="plugin in plugins" 
                      :key="plugin.id"
                      class="flex items-center space-x-2"
                    >
                      <input 
                        type="checkbox" 
                        v-model="currentAgent.plugins" 
                        :value="plugin.id"
                        class="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                      />
                      <span class="text-sm">{{ plugin.name }}</span>
                    </label>
                  </div>
                </div>
                <div>
                  <h4 class="text-lg font-medium text-blue-700 mb-3">关联知识库</h4>
                  <div class="space-y-2 max-h-40 overflow-y-auto">
                    <div v-if="loadingKnowledgeBases" class="text-center py-3 text-sm text-gray-500">
                      加载知识库列表...
                    </div>
                    <div v-else-if="knowledgeBases.length === 0" class="text-center py-3 text-sm text-gray-500">
                      暂无可用知识库
                    </div>
                    <label v-else 
                      v-for="kb in knowledgeBases" 
                      :key="kb.id"
                      class="flex items-center space-x-2"
                    >
                      <input 
                        type="checkbox" 
                        v-model="currentAgent.knowledge_bases" 
                        :value="kb.id"
                        class="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                      />
                      <span class="text-sm">{{ kb.name }}</span>
                    </label>
                  </div>
                </div>
              </div>

              <!-- 高级配置 -->
              <div>
                <h4 class="text-lg font-medium text-blue-700 mb-3">高级配置</h4>
                <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">状态</label>
                    <select 
                      v-model="currentAgent.status" 
                      class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                    >
                      <option value="active">活跃</option>
                      <option value="inactive">禁用</option>
                    </select>
                  </div>
                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">是否公开</label>
                    <select 
                      v-model="currentAgent.is_public" 
                      class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                    >
                      <option value="false">私有</option>
                      <option value="true">公开</option>
                    </select>
                  </div>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="flex items-center justify-between mt-6">
                <div class="flex gap-3">
                  <button 
                    type="submit" 
                    class="px-4 py-2 bg-gradient-to-r from-blue-500 to-blue-600 text-white rounded-lg hover:shadow-button-hover transition-all duration-300 focus:outline-none focus:ring-2 focus:ring-blue-500 shadow-button transform hover:-translate-y-0.5"
                    :disabled="saving"
                  >
                    <span v-if="saving" class="inline-flex items-center">
                      <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                      </svg>
                      保存中...
                    </span>
                    <span v-else class="inline-flex items-center">
                      <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4"></path>
                      </svg>
                      {{ currentAgent.id ? '更新智能体' : '创建智能体' }}
                    </span>
                  </button>
                  <button 
                    @click="cancelEdit" 
                    class="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors focus:outline-none focus:ring-2 focus:ring-gray-500"
                  >
                    取消
                  </button>
                  <button 
                    v-if="currentAgent.id"
                    @click="testAgent" 
                    class="px-4 py-2 bg-green-100 text-green-800 rounded-lg hover:bg-green-200 transition-colors focus:outline-none focus:ring-2 focus:ring-green-500"
                  >
                    测试智能体
                  </button>
                </div>
                <span v-if="msg" class="text-sm font-medium flex items-center" :class="msg.includes('成功') ? 'text-green-600' : 'text-red-600'">
                  <svg v-if="msg.includes('成功')" class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                  </svg>
                  {{ msg }}
                </span>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { authFetch } from '../auth.js';

export default {
  name: 'AgentEditor',
  data() {
    return {
      agents: [],
      currentAgent: null,
      llmConfigStr: '',
      plugins: [],
      knowledgeBases: [],
      msg: '',
      loadingAgents: false,
      loadingPlugins: false,
      loadingKnowledgeBases: false,
      saving: false
    };
  },
  async mounted() {
    await this.loadAgents();
  },
  watch: {
    currentAgent: {
      handler(agent) {
        if (agent) {
          this.llmConfigStr = JSON.stringify(agent.llm_config || {}, null, 2);
          this.loadPlugins();
          this.loadKnowledgeBases();
        }
      },
      immediate: true
    }
  },
  methods: {
    getStatusBadgeClass(status) {
      switch(status.toLowerCase()) {
        case 'active':
          return 'bg-green-100 text-green-800';
        case 'inactive':
          return 'bg-red-100 text-red-800';
        default:
          return 'bg-gray-100 text-gray-800';
      }
    },
    
    async loadAgents() {
      this.loadingAgents = true;
      try {
        const res = await authFetch('/api/agents', { method: 'GET' });
        if (res.ok) {
          this.agents = await res.json();
        } else {
          this.msg = '加载智能体失败: ' + (await res.text());
        }
      } catch (e) {
        this.msg = '加载智能体失败: ' + (e.message || '未知错误');
        console.error('Failed to load agents:', e);
      } finally {
        this.loadingAgents = false;
      }
    },
    
    async loadPlugins() {
      this.loadingPlugins = true;
      try {
        const res = await authFetch('/api/plugins', { method: 'GET' });
        if (res.ok) {
          this.plugins = await res.json();
        }
      } catch (e) {
        console.error('Failed to load plugins:', e);
      } finally {
        this.loadingPlugins = false;
      }
    },
    
    async loadKnowledgeBases() {
      this.loadingKnowledgeBases = true;
      try {
        const res = await authFetch('/api/knowledge-bases', { method: 'GET' });
        if (res.ok) {
          this.knowledgeBases = await res.json();
        }
      } catch (e) {
        console.error('Failed to load knowledge bases:', e);
      } finally {
        this.loadingKnowledgeBases = false;
      }
    },
    
    createAgent() {
      this.currentAgent = {
        name: '',
        description: '',
        system_prompt: '',
        user_prompt_template: '{{userInput}}',
        llm_provider: '',
        llm_model: '',
        llm_config: {},
        plugins: [],
        knowledge_bases: [],
        status: 'active',
        is_public: false
      };
      this.llmConfigStr = JSON.stringify({}, null, 2);
      this.msg = '';
    },
    
    editAgent(agent) {
      this.currentAgent = { ...agent };
      this.llmConfigStr = JSON.stringify(agent.llm_config || {}, null, 2);
      // 确保plugins和knowledge_bases是数组
      if (!Array.isArray(this.currentAgent.plugins)) {
        this.currentAgent.plugins = [];
      }
      if (!Array.isArray(this.currentAgent.knowledge_bases)) {
        this.currentAgent.knowledge_bases = [];
      }
      this.msg = '';
    },
    
    cancelEdit() {
      this.currentAgent = null;
      this.msg = '';
    },
    
    async saveAgent() {
      this.msg = '';
      this.saving = true;
      
      try {
        // 验证LLM配置JSON格式
        if (this.llmConfigStr) {
          this.currentAgent.llm_config = JSON.parse(this.llmConfigStr);
        }
        
        // 准备请求参数
        const method = this.currentAgent.id ? 'PUT' : 'POST';
        const path = this.currentAgent.id ? `/api/agents/${this.currentAgent.id}` : '/api/agents';
        
        const res = await authFetch(path, {
          method: method,
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(this.currentAgent)
        });
        
        if (res.ok) {
          this.msg = this.currentAgent.id ? '更新成功' : '创建成功';
          await this.loadAgents();
          this.currentAgent = null;
        } else {
          const errorText = await res.text();
          this.msg = this.currentAgent.id ? '更新失败: ' + errorText : '创建失败: ' + errorText;
        }
      } catch (e) {
        if (e instanceof SyntaxError) {
          this.msg = 'LLM配置JSON格式错误';
        } else {
          this.msg = this.currentAgent.id ? '更新失败: ' + (e.message || '未知错误') : '创建失败: ' + (e.message || '未知错误');
        }
        console.error('Failed to save agent:', e);
      } finally {
        this.saving = false;
      }
    },
    
    async deleteAgent(id) {
      if (!confirm('确定要删除这个智能体吗？')) {
        return;
      }
      
      try {
        const res = await authFetch(`/api/agents/${id}`, { method: 'DELETE' });
        if (res.ok) {
          await this.loadAgents();
          if (this.currentAgent && this.currentAgent.id === id) {
            this.currentAgent = null;
          }
        } else {
          this.msg = '删除失败: ' + (await res.text());
        }
      } catch (e) {
        this.msg = '删除失败: ' + (e.message || '未知错误');
        console.error('Failed to delete agent:', e);
      }
    },
    
    async testAgent() {
      try {
        const res = await authFetch(`/api/agents/${this.currentAgent.id}/test`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ test_input: '你好，我是测试输入' })
        });
        
        if (res.ok) {
          const result = await res.json();
          alert('测试结果：\n' + result.response);
        } else {
          this.msg = '测试失败: ' + (await res.text());
        }
      } catch (e) {
        this.msg = '测试失败: ' + (e.message || '未知错误');
        console.error('Failed to test agent:', e);
      }
    }
  }
};
</script>