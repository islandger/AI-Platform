<template>
  <div class="p-4 md:p-6 animate-fade-in">
    <h2 class="text-2xl font-bold text-blue-700 mb-4">插件管理</h2>
    <p class="text-gray-600 mb-6 text-sm md:text-base">注册、管理和配置各种插件，扩展系统功能。</p>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- 注册/编辑插件 -->
      <div class="animate-slide-up">
        <h3 class="text-xl font-semibold text-blue-700 mb-4">
          {{ editingPlugin ? '编辑插件' : '注册插件' }}
        </h3>
        <div class="bg-white p-4 rounded-xl border border-gray-200 shadow-card hover:shadow-card-hover transition-all duration-300">
          <div class="space-y-3">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">名称</label>
              <input 
                v-model="form.name" 
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                placeholder="输入插件名称"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">描述</label>
              <input 
                v-model="form.description" 
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                placeholder="输入插件描述"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">OpenAPI JSON</label>
              <textarea 
                v-model="form.openapi" 
                rows="8" 
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 font-mono text-sm transition-all duration-300"
                placeholder='输入 OpenAPI JSON (例如: {"openapi": "3.0.0", "info": {"title": "My Plugin", "version": "1.0.0"}})'
              ></textarea>
            </div>
            <div class="flex items-center space-x-3 mt-4">
              <button 
                @click="editingPlugin ? updatePlugin() : registerPlugin()" 
                class="px-4 py-2 bg-gradient-to-r from-blue-500 to-blue-600 text-white rounded-lg hover:shadow-button-hover transition-all duration-300 focus:outline-none focus:ring-2 focus:ring-blue-500 shadow-button transform hover:-translate-y-0.5"
                :disabled="registering"
              >
                <span v-if="registering" class="inline-flex items-center">
                  <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  {{ editingPlugin ? '更新中...' : '注册中...' }}
                </span>
                <span v-else class="inline-flex items-center">
                  <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
                  </svg>
                  {{ editingPlugin ? '更新' : '注册' }}
                </span>
              </button>
              <button 
                v-if="editingPlugin"
                @click="cancelEdit"
                class="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors focus:outline-none focus:ring-2 focus:ring-gray-500"
              >
                取消
              </button>
              <span v-if="msg" class="text-sm font-medium flex items-center" :class="msg.includes('成功') ? 'text-green-600' : 'text-red-600'">
                <svg v-if="msg.includes('成功')" class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                </svg>
                {{ msg }}
              </span>
            </div>
          </div>
        </div>

        <!-- 插件列表 -->
        <h3 class="text-xl font-semibold text-blue-700 mt-6 mb-4">已注册插件</h3>
        <div v-if="loadingPlugins" class="text-center py-6 bg-white rounded-xl border border-gray-200 shadow-card">
          <svg class="animate-spin mx-auto h-8 w-8 text-blue-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <p class="mt-2 text-sm text-gray-500">加载插件列表...</p>
        </div>
        <div v-else-if="plugins.length===0" class="text-center py-6 bg-white rounded-xl border border-gray-200 shadow-card text-gray-500">
          暂无插件
        </div>
        <div v-else class="space-y-4">
          <div 
            v-for="plugin in plugins" 
            :key="plugin.id" 
            class="border border-gray-200 rounded-xl p-4 bg-white shadow-card hover:shadow-card-hover transition-all duration-300"
          >
            <div class="flex flex-col space-y-2">
              <div class="flex items-center justify-between">
                <strong class="text-lg text-blue-700">{{ plugin.name }}</strong>
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium" :class="getStatusBadgeClass(plugin.status)">
                  {{ plugin.status }}
                </span>
              </div>
              <div class="text-sm text-gray-600">{{ plugin.description }}</div>
              <div class="flex gap-2 mt-3">
                <button @click="editPlugin(plugin)" class="px-3 py-1 bg-blue-100 text-blue-800 rounded-lg hover:bg-blue-200 transition-colors text-sm">
                  编辑
                </button>
                <button 
                  @click="togglePluginStatus(plugin)"
                  class="px-3 py-1 rounded-lg hover:shadow-md transition-all duration-300 text-sm"
                  :class="plugin.status === 'active' ? 'bg-yellow-100 text-yellow-800 hover:bg-yellow-200' : 'bg-green-100 text-green-800 hover:bg-green-200'"
                >
                  {{ plugin.status === 'active' ? '禁用' : '启用' }}
                </button>
                <button @click="deletePlugin(plugin.id)" class="px-3 py-1 bg-red-100 text-red-800 rounded-lg hover:bg-red-200 transition-colors text-sm">
                  删除
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 插件详情和配置 -->
      <div class="animate-slide-up" style="animation-delay: 0.2s;">
        <h3 class="text-xl font-semibold text-blue-700 mb-4">插件详情</h3>
        <div v-if="selectedPlugin" class="bg-white p-4 rounded-xl border border-gray-200 shadow-card hover:shadow-card-hover transition-all duration-300 mb-6">
          <div class="space-y-4">
            <div>
              <h4 class="text-lg font-medium text-blue-700 mb-2">{{ selectedPlugin.name }}</h4>
              <p class="text-gray-600">{{ selectedPlugin.description }}</p>
            </div>
            <div class="flex items-center space-x-3">
              <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium" :class="getStatusBadgeClass(selectedPlugin.status)">
                状态: {{ selectedPlugin.status }}
              </span>
              <span class="text-sm text-gray-500">
                ID: {{ selectedPlugin.id }}
              </span>
            </div>
            <div class="pt-2 border-t border-gray-200">
              <h5 class="text-sm font-medium text-gray-700 mb-2">OpenAPI 路径</h5>
              <code class="text-sm text-blue-700 bg-blue-50 p-2 rounded">
                {{ selectedPlugin.api_path || '/' + selectedPlugin.name.toLowerCase() }}
              </code>
            </div>
          </div>
        </div>
        <div v-else class="bg-white p-6 rounded-xl border border-gray-200 shadow-card hover:shadow-card-hover transition-all duration-300 mb-6 text-center text-gray-500">
          
          请从左侧选择一个插件查看详情
        </div>

        <!-- 快捷操作 -->
        <div v-if="plugins.length > 0">
          <h3 class="text-xl font-semibold text-blue-700 mb-4">快捷操作</h3>
          <div class="bg-white p-4 rounded-xl border border-gray-200 shadow-card hover:shadow-card-hover transition-all duration-300">
            <div class="space-y-2">
              <button 
                @click="enableAllPlugins"
                class="w-full px-4 py-2 bg-green-100 text-green-800 rounded-lg hover:bg-green-200 transition-colors focus:outline-none focus:ring-2 focus:ring-green-500 shadow-sm hover:shadow-md"
              >
                <svg class="w-4 h-4 mr-2 inline" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
                </svg>
                全部启用
              </button>
              <button 
                @click="disableAllPlugins"
                class="w-full px-4 py-2 bg-yellow-100 text-yellow-800 rounded-lg hover:bg-yellow-200 transition-colors focus:outline-none focus:ring-2 focus:ring-yellow-500 shadow-sm hover:shadow-md"
              >
                <svg class="w-4 h-4 mr-2 inline" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 5.636l-3.536 3.536m0 5.656l3.536 3.536M9.172 9.172L5.636 5.636m3.536 9.192l-3.536 3.536M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-5 0a4 4 0 11-8 0 4 4 0 018 0z"></path>
                </svg>
                全部禁用
              </button>
              <button 
                @click="refreshPlugins"
                class="w-full px-4 py-2 bg-blue-100 text-blue-800 rounded-lg hover:bg-blue-200 transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 shadow-sm hover:shadow-md"
              >
                <svg class="w-4 h-4 mr-2 inline" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
                </svg>
                刷新列表
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { authFetch } from '../auth.js';

export default {
  name: 'PluginManager',
  data() {
    return {
      plugins: [],
      form: { name: '', description: '', openapi: '' },
      msg: '',
      registering: false,
      loadingPlugins: false,
      editingPlugin: null,
      selectedPlugin: null
    };
  },
  mounted() {
    this.loadPlugins();
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
    
    async loadPlugins() {
      this.loadingPlugins = true;
      try {
        const res = await authFetch('/api/plugins');
        const data = await res.json();
        this.plugins = Array.isArray(data) ? data : [];
      } catch (e) {
        this.msg = '加载插件失败: ' + (e.message || '未知错误');
        console.error('Failed to load plugins:', e);
      } finally {
        this.loadingPlugins = false;
      }
    },
    
    refreshPlugins() {
      this.loadPlugins();
    },
    
    editPlugin(plugin) {
      this.editingPlugin = plugin;
      this.form = {
        name: plugin.name,
        description: plugin.description,
        openapi: JSON.stringify(plugin.openapi_spec || {}, null, 2)
      };
      this.selectedPlugin = plugin;
      this.msg = '';
    },
    
    cancelEdit() {
      this.editingPlugin = null;
      this.form = { name: '', description: '', openapi: '' };
      this.msg = '';
    },
    
    async registerPlugin() {
      if (!this.form.name) {
        this.msg = '名称必填';
        return;
      }
      
      this.registering = true;
      this.msg = '';
      
      try {
        const payload = {
          name: this.form.name,
          description: this.form.description,
          openapi_spec: JSON.parse(this.form.openapi || '{}')
        };
        
        const res = await authFetch('/api/plugins', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        
        if (res.ok) {
          this.msg = '注册成功';
          this.form = { name: '', description: '', openapi: '' };
          await this.loadPlugins();
        } else {
          const errorText = await res.text();
          this.msg = '注册失败: ' + errorText;
        }
      } catch (e) {
        if (e instanceof SyntaxError) {
          this.msg = '注册失败: OpenAPI JSON 格式错误';
        } else {
          this.msg = '注册失败: ' + (e.message || '请求失败');
        }
        console.error('Failed to register plugin:', e);
      } finally {
        this.registering = false;
      }
    },
    
    async updatePlugin() {
      if (!this.form.name) {
        this.msg = '名称必填';
        return;
      }
      
      this.registering = true;
      this.msg = '';
      
      try {
        const payload = {
          name: this.form.name,
          description: this.form.description,
          openapi_spec: JSON.parse(this.form.openapi || '{}')
        };
        
        const res = await authFetch(`/api/plugins/${this.editingPlugin.id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        
        if (res.ok) {
          this.msg = '更新成功';
          this.editingPlugin = null;
          this.form = { name: '', description: '', openapi: '' };
          await this.loadPlugins();
        } else {
          const errorText = await res.text();
          this.msg = '更新失败: ' + errorText;
        }
      } catch (e) {
        if (e instanceof SyntaxError) {
          this.msg = '更新失败: OpenAPI JSON 格式错误';
        } else {
          this.msg = '更新失败: ' + (e.message || '请求失败');
        }
        console.error('Failed to update plugin:', e);
      } finally {
        this.registering = false;
      }
    },
    
    async togglePluginStatus(plugin) {
      try {
        const newStatus = plugin.status === 'active' ? 'inactive' : 'active';
        const res = await authFetch(`/api/plugins/${plugin.id}/status`, {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ status: newStatus })
        });
        
        if (res.ok) {
          await this.loadPlugins();
        } else {
          const errorText = await res.text();
          this.msg = '状态更新失败: ' + errorText;
        }
      } catch (e) {
        this.msg = '状态更新失败: ' + (e.message || '请求失败');
        console.error('Failed to toggle plugin status:', e);
      }
    },
    
    async enableAllPlugins() {
      try {
        const res = await authFetch('/api/plugins/batch', {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ status: 'active' })
        });
        
        if (res.ok) {
          await this.loadPlugins();
        } else {
          const errorText = await res.text();
          this.msg = '批量启用失败: ' + errorText;
        }
      } catch (e) {
        this.msg = '批量启用失败: ' + (e.message || '请求失败');
        console.error('Failed to enable all plugins:', e);
      }
    },
    
    async disableAllPlugins() {
      try {
        const res = await authFetch('/api/plugins/batch', {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ status: 'inactive' })
        });
        
        if (res.ok) {
          await this.loadPlugins();
        } else {
          const errorText = await res.text();
          this.msg = '批量禁用失败: ' + errorText;
        }
      } catch (e) {
        this.msg = '批量禁用失败: ' + (e.message || '请求失败');
        console.error('Failed to disable all plugins:', e);
      }
    },
    
    async deletePlugin(id) {
      if (!confirm('确定要删除这个插件吗？')) {
        return;
      }
      
      try {
        const res = await authFetch(`/api/plugins/${id}`, { method: 'DELETE' });
        
        if (res.ok) {
          await this.loadPlugins();
          if (this.selectedPlugin && this.selectedPlugin.id === id) {
            this.selectedPlugin = null;
          }
          if (this.editingPlugin && this.editingPlugin.id === id) {
            this.editingPlugin = null;
            this.form = { name: '', description: '', openapi: '' };
          }
        } else {
          const errorText = await res.text();
          this.msg = '删除失败: ' + errorText;
        }
      } catch (e) {
        this.msg = '删除失败: ' + (e.message || '请求失败');
        console.error('Failed to delete plugin:', e);
      }
    }
  }
};
</script>