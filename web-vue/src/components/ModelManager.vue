<template>
  <div class="p-4 md:p-6 animate-fade-in">
    <h2 class="text-2xl font-bold text-blue-700 mb-4">模型管理</h2>
    <p class="text-gray-600 mb-6 text-sm md:text-base">管理系统中可用的AI模型，配置API密钥和模型参数。</p>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- 模型列表 -->
      <div class="animate-slide-up">
        <h3 class="text-xl font-semibold text-blue-700 mb-4">可用模型</h3>
        
        <div v-if="loadingModels" class="text-center py-6 bg-white rounded-xl border border-gray-200 shadow-card">
          <svg class="animate-spin mx-auto h-8 w-8 text-blue-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <p class="mt-2 text-sm text-gray-500">加载模型列表...</p>
        </div>
        
        <div v-else-if="models.length === 0" class="text-center py-6 bg-white rounded-xl border border-gray-200 shadow-card text-gray-500">
          暂无可用模型
        </div>
        
        <div v-else class="space-y-4">
          <div 
            v-for="model in models" 
            :key="model.id" 
            class="border border-gray-200 rounded-xl p-4 bg-white shadow-card hover:shadow-card-hover transition-all duration-300"
          >
            <div class="flex flex-col space-y-2">
              <div class="flex items-center justify-between">
                <strong class="text-lg text-blue-700">{{ model.name }}</strong>
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium" :class="getStatusBadgeClass(model.status)">
                  {{ model.status }}
                </span>
              </div>
              <div class="text-sm text-gray-600">{{ model.description }}</div>
              <div class="text-xs text-gray-500">提供商: {{ model.provider }} | 模型: {{ model.model_name }}</div>
              <div class="mt-3 flex gap-2">
                <button 
                  @click="editModel(model)" 
                  class="px-3 py-1 bg-blue-100 text-blue-800 rounded-lg hover:bg-blue-200 transition-colors text-sm"
                >
                  编辑配置
                </button>
                <button 
                  @click="toggleModelStatus(model)" 
                  :class="[
                    'px-3 py-1 rounded-lg hover:bg-opacity-90 transition-colors text-sm',
                    model.status === 'active' ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800'
                  ]"
                >
                  {{ model.status === 'active' ? '禁用' : '启用' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 模型配置 -->
      <div class="animate-slide-up" style="animation-delay: 0.2s;">
        <h3 class="text-xl font-semibold text-blue-700 mb-4">
          {{ currentModel ? '编辑模型配置' : '添加新模型' }}
        </h3>
        
        <div class="bg-white p-4 rounded-xl border border-gray-200 shadow-card hover:shadow-card-hover transition-all duration-300">
          <form @submit.prevent="saveModel">
            <div class="space-y-3">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">模型名称</label>
                <input 
                  v-model="currentModel.name" 
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                  placeholder="输入模型名称"
                  required
                />
              </div>
              
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">描述</label>
                <input 
                  v-model="currentModel.description" 
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                  placeholder="输入模型描述"
                  required
                />
              </div>
              
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">提供商</label>
                <select 
                  v-model="currentModel.provider" 
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                  required
                >
                  <option value="">请选择提供商</option>
                  <option value="deepseek">DeepSeek</option>
                  <option value="doubao">豆包</option>
                  <option value="tongyi">通义千问</option>
                  <option value="wenxin">文心一言</option>
                  <option value="custom">自定义</option>
                </select>
              </div>
              
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">模型ID</label>
                <input 
                  v-model="currentModel.model_name" 
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                  placeholder="输入模型ID（如：deepseek-chat）"
                  required
                />
              </div>
              
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">API密钥</label>
                <div class="flex">
                  <input 
                    v-model="currentModel.api_key" 
                    :type="showApiKey ? 'text' : 'password'"
                    class="flex-1 px-3 py-2 border border-gray-300 rounded-l-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                    placeholder="输入API密钥"
                    required
                  />
                  <button 
                    type="button" 
                    @click="showApiKey = !showApiKey"
                    class="px-3 py-2 bg-gray-100 border border-gray-300 rounded-r-lg hover:bg-gray-200 transition-colors focus:outline-none focus:ring-2 focus:ring-gray-500"
                  >
                    <svg v-if="!showApiKey" class="w-4 h-4 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                    </svg>
                    <svg v-else class="w-4 h-4 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"></path>
                    </svg>
                  </button>
                </div>
              </div>
              
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">模型配置（JSON）</label>
                <textarea 
                  v-model="modelConfigStr" 
                  rows="6" 
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 font-mono text-sm transition-all duration-300"
                  placeholder='{"temperature": 0.7, "max_tokens": 1000}'
                ></textarea>
              </div>
              
              <div class="flex items-center space-x-3 mt-4">
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
                    保存配置
                  </span>
                </button>
                
                <button 
                  type="button" 
                  @click="cancelEdit" 
                  class="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors focus:outline-none focus:ring-2 focus:ring-gray-500"
                >
                  取消
                </button>
              </div>
              
              <span v-if="msg" class="text-sm font-medium flex items-center" :class="msg.includes('成功') ? 'text-green-600' : 'text-red-600'">
                <svg v-if="msg.includes('成功')" class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                </svg>
                {{ msg }}
              </span>
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
  name: 'ModelManager',
  data() {
    return {
      models: [],
      currentModel: {
        id: null,
        name: '',
        description: '',
        provider: '',
        model_name: '',
        api_key: '',
        config: {},
        status: 'active'
      },
      modelConfigStr: '',
      msg: '',
      loadingModels: false,
      saving: false,
      showApiKey: false
    };
  },
  mounted() {
    this.loadModels();
  },
  methods: {
    getStatusBadgeClass(status) {
      switch(status.toLowerCase()) {
        case 'active':
          return 'bg-green-100 text-green-800';
        case 'inactive':
          return 'bg-red-100 text-red-800';
        case 'pending':
          return 'bg-yellow-100 text-yellow-800';
        default:
          return 'bg-gray-100 text-gray-800';
      }
    },
    
    async loadModels() {
      this.loadingModels = true;
      try {
        const res = await authFetch('/api/models', { method: 'GET' });
        if (res.ok) {
          this.models = await res.json();
        } else {
          this.msg = '加载模型失败: ' + (await res.text());
        }
      } catch (e) {
        this.msg = '加载模型失败: ' + (e.message || '未知错误');
        console.error('Failed to load models:', e);
      } finally {
        this.loadingModels = false;
      }
    },
    
    editModel(model) {
      this.currentModel = { ...model };
      this.modelConfigStr = JSON.stringify(model.config || {}, null, 2);
      this.msg = '';
    },
    
    cancelEdit() {
      this.resetForm();
    },
    
    resetForm() {
      this.currentModel = {
        id: null,
        name: '',
        description: '',
        provider: '',
        model_name: '',
        api_key: '',
        config: {},
        status: 'active'
      };
      this.modelConfigStr = '';
      this.msg = '';
      this.showApiKey = false;
    },
    
    async saveModel() {
      this.msg = '';
      this.saving = true;
      
      try {
        // 验证配置JSON格式
        if (this.modelConfigStr) {
          this.currentModel.config = JSON.parse(this.modelConfigStr);
        }
        
        // 准备请求参数
        const method = this.currentModel.id ? 'PUT' : 'POST';
        const path = this.currentModel.id ? `/api/models/${this.currentModel.id}` : '/api/models';
        
        const res = await authFetch(path, {
          method: method,
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(this.currentModel)
        });
        
        if (res.ok) {
          this.msg = '保存成功';
          await this.loadModels();
          this.resetForm();
        } else {
          const errorText = await res.text();
          this.msg = '保存失败: ' + errorText;
        }
      } catch (e) {
        if (e instanceof SyntaxError) {
          this.msg = '模型配置JSON格式错误';
        } else {
          this.msg = '保存失败: ' + (e.message || '未知错误');
        }
        console.error('Failed to save model:', e);
      } finally {
        this.saving = false;
      }
    },
    
    async toggleModelStatus(model) {
      try {
        const newStatus = model.status === 'active' ? 'inactive' : 'active';
        const res = await authFetch(`/api/models/${model.id}/status`, {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ status: newStatus })
        });
        
        if (res.ok) {
          model.status = newStatus;
          this.msg = `模型已${newStatus === 'active' ? '启用' : '禁用'}`;
        } else {
          this.msg = '更新状态失败: ' + (await res.text());
        }
      } catch (e) {
        this.msg = '更新状态失败: ' + (e.message || '未知错误');
        console.error('Failed to toggle model status:', e);
      }
    }
  }
};
</script>