<template>
  <div class="p-4 md:p-6 animate-fade-in">
    <h2 class="text-2xl font-bold text-blue-700 mb-4">工作流设计器</h2>
    <p class="text-gray-600 mb-6 text-sm md:text-base">创建和管理工作流，使用可视化编辑器设计工作流程，配置节点和连接关系。</p>

    <div class="grid grid-cols-1 lg:grid-cols-4 gap-6">
      <!-- 工作流列表 -->
      <div class="lg:col-span-1 animate-slide-up">
        <h3 class="text-xl font-semibold text-blue-700 mb-4">工作流列表</h3>
        <div v-if="loadingWorkflows" class="text-center py-6 bg-white rounded-xl border border-gray-200 shadow-card">
          <svg class="animate-spin mx-auto h-8 w-8 text-blue-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <p class="mt-2 text-sm text-gray-500">加载工作流列表...</p>
        </div>
        <div v-else-if="workflows.length === 0" class="text-center py-6 bg-white rounded-xl border border-gray-200 shadow-card text-gray-500">
          暂无工作流
        </div>
        <div v-else class="space-y-4">
          <div 
            v-for="workflow in workflows" 
            :key="workflow.id" 
            class="border border-gray-200 rounded-xl p-4 bg-white shadow-card hover:shadow-card-hover transition-all duration-300"
          >
            <div class="flex flex-col space-y-2">
              <div class="flex items-center justify-between">
                <strong class="text-lg text-blue-700">{{ workflow.name }}</strong>
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium" :class="getStatusBadgeClass(workflow.status || 'active')">
                  {{ workflow.status || 'active' }}
                </span>
              </div>
              <div class="text-sm text-gray-600">{{ workflow.description }}</div>
              <div class="flex gap-2 mt-3">
                <button @click="editWorkflow(workflow)" class="px-3 py-1 bg-blue-100 text-blue-800 rounded-lg hover:bg-blue-200 transition-colors text-sm">
                  编辑
                </button>
                <button @click="deleteWorkflow(workflow.id)" class="px-3 py-1 bg-red-100 text-red-800 rounded-lg hover:bg-red-200 transition-colors text-sm">
                  删除
                </button>
              </div>
            </div>
          </div>
        </div>
        <button @click="createWorkflow" class="w-full mt-4 px-4 py-2 bg-gradient-to-r from-blue-500 to-blue-600 text-white rounded-lg hover:shadow-button-hover transition-all duration-300 focus:outline-none focus:ring-2 focus:ring-blue-500 shadow-button transform hover:-translate-y-0.5">
          <svg class="w-4 h-4 mr-2 inline" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
          </svg>
          创建新工作流
        </button>
      </div>

      <!-- 工作流设计器 -->
      <div class="lg:col-span-3 animate-slide-up" style="animation-delay: 0.2s;">
        <h3 class="text-xl font-semibold text-blue-700 mb-4">
          {{ currentWorkflow && currentWorkflow.id ? '编辑工作流' : '创建新工作流' }}
        </h3>
        <div v-if="currentWorkflow" class="bg-white p-4 rounded-xl border border-gray-200 shadow-card hover:shadow-card-hover transition-all duration-300">
          <div class="mb-6">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-3">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">名称</label>
                <input 
                  type="text" 
                  v-model="currentWorkflow.name" 
                  required 
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">描述</label>
                <input 
                  type="text" 
                  v-model="currentWorkflow.description" 
                  required 
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                />
              </div>
            </div>
          </div>

          <!-- 工作流编辑器 -->
          <div class="border border-gray-200 rounded-xl overflow-hidden mb-4">
            <div class="flex items-center justify-between p-3 bg-gray-50 border-b border-gray-200">
              <h4 class="text-lg font-medium text-blue-700">工作流画布</h4>
              <div class="flex gap-2">
                <button @click="addStartNode" class="px-3 py-1 bg-blue-100 text-blue-800 rounded-lg hover:bg-blue-200 transition-colors text-sm">
                  添加开始节点
                </button>
                <button @click="clearCanvas" class="px-3 py-1 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors text-sm">
                  清空画布
                </button>
              </div>
            </div>
            <div class="h-96 overflow-hidden bg-gray-50 p-4">
              <!-- 节点列表 -->
              <div class="flex flex-wrap gap-2 mb-4">
                <div 
                  v-for="nodeType in nodeTypes" 
                  :key="nodeType.type"
                  class="px-3 py-2 bg-white border border-gray-300 rounded-lg cursor-move hover:shadow-card transition-all duration-300"
                  draggable="true"
                  @dragstart="onDragStart($event, nodeType)"
                >
                  <div class="flex items-center space-x-2">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z"></path>
                    </svg>
                    <span class="text-sm font-medium">{{ nodeType.label }}</span>
                  </div>
                </div>
              </div>

              <!-- 画布 -->
              <div 
                class="relative h-[calc(100%-40px)] bg-white border-2 border-dashed border-gray-300 rounded-lg"
                @dragover.prevent
                @drop="onDrop($event)"
              >
                <div 
                  v-for="node in currentWorkflow.nodes" 
                  :key="node.id"
                  class="absolute p-3 bg-white border-2 border-gray-300 rounded-lg shadow-card hover:shadow-card-hover transition-all duration-300"
                  :style="{ left: node.position.x + 'px', top: node.position.y + 'px' }"
                  @click="selectNode(node)"
                >
                  <div class="flex items-center justify-between space-x-2">
                    <div class="flex items-center space-x-1">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z"></path>
                      </svg>
                      <span class="text-sm font-medium">{{ getNodeLabel(node) }}</span>
                    </div>
                    <button 
                      @click.stop="deleteNode(node.id)"
                      class="px-2 py-1 bg-red-100 text-red-800 rounded hover:bg-red-200 transition-colors text-xs"
                    >
                      删除
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 节点配置 -->
          <div v-if="selectedNode" class="border border-gray-200 rounded-xl overflow-hidden mb-4">
            <div class="flex items-center justify-between p-3 bg-gray-50 border-b border-gray-200">
              <h4 class="text-lg font-medium text-blue-700">节点配置</h4>
              <button 
                @click="selectedNode = null"
                class="px-3 py-1 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors text-sm"
              >
                关闭
              </button>
            </div>
            <div class="p-4">
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">节点类型</label>
                  <input 
                    type="text" 
                    v-model="selectedNode.type" 
                    readonly
                    class="w-full px-3 py-2 border border-gray-300 rounded-lg bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-500 transition-all duration-300"
                  />
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">节点名称</label>
                  <input 
                    type="text" 
                    v-model="selectedNode.name" 
                    class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                  />
                </div>
              </div>

              <!-- LLM节点配置 -->
              <div v-if="selectedNode.type === 'llm'" class="mt-4">
                <h5 class="text-md font-medium text-gray-700 mb-2">LLM配置</h5>
                <div class="space-y-3">
                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">模型提供商</label>
                    <select 
                      v-model="selectedNode.config.provider" 
                      class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                    >
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
                      v-model="selectedNode.config.model" 
                      class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                      placeholder="如: deepseek-chat"
                    />
                  </div>
                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">提示词</label>
                    <textarea 
                      v-model="selectedNode.config.prompt" 
                      rows="4" 
                      class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                      placeholder="请输入提示词..."
                    ></textarea>
                  </div>
                </div>
              </div>

              <!-- HTTP节点配置 -->
              <div v-if="selectedNode.type === 'http'" class="mt-4">
                <h5 class="text-md font-medium text-gray-700 mb-2">HTTP配置</h5>
                <div class="space-y-3">
                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">方法</label>
                    <select 
                      v-model="selectedNode.config.method" 
                      class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                    >
                      <option value="GET">GET</option>
                      <option value="POST">POST</option>
                      <option value="PUT">PUT</option>
                      <option value="DELETE">DELETE</option>
                    </select>
                  </div>
                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">URL</label>
                    <input 
                      type="text" 
                      v-model="selectedNode.config.url" 
                      class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                      placeholder="http://example.com/api"
                    />
                  </div>
                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">请求体</label>
                    <textarea 
                      v-model="selectedNode.config.body" 
                      rows="4" 
                      class="w-full px-3 py-2 border border-gray-300 rounded-lg font-mono text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                      placeholder='{"key": "value"}'
                    ></textarea>
                  </div>
                </div>
              </div>

              <!-- 知识库节点配置 -->
              <div v-if="selectedNode.type === 'knowledge'" class="mt-4">
                <h5 class="text-md font-medium text-gray-700 mb-2">知识库配置</h5>
                <div class="space-y-3">
                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">知识库ID</label>
                    <input 
                      type="text" 
                      v-model="selectedNode.config.knowledgeBaseId" 
                      class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                    />
                  </div>
                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">Top K</label>
                    <input 
                      type="number" 
                      v-model.number="selectedNode.config.topK" 
                      min="1" 
                      max="20"
                      class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                    />
                  </div>
                </div>
              </div>

              <!-- 字符串处理节点配置 -->
              <div v-if="selectedNode.type === 'string'" class="mt-4">
                <h5 class="text-md font-medium text-gray-700 mb-2">字符串处理配置</h5>
                <div class="space-y-3">
                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">操作类型</label>
                    <select 
                      v-model="selectedNode.config.operation" 
                      class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                    >
                      <option value="concat">拼接</option>
                      <option value="replace">替换</option>
                      <option value="format">格式化</option>
                    </select>
                  </div>
                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">参数</label>
                    <textarea 
                      v-model="selectedNode.config.params" 
                      rows="4" 
                      class="w-full px-3 py-2 border border-gray-300 rounded-lg font-mono text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                      placeholder='{"delimiter": ",", "values": ["{{input1}}", "{{input2}}"]}'
                    ></textarea>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- JSON编辑 -->
          <div class="border border-gray-200 rounded-xl overflow-hidden mb-4">
            <div class="flex items-center justify-between p-3 bg-gray-50 border-b border-gray-200">
              <h4 class="text-lg font-medium text-blue-700">JSON编辑</h4>
            </div>
            <div class="p-4">
              <textarea 
                v-model="workflowJsonStr" 
                rows="10" 
                class="w-full px-3 py-2 border border-gray-300 rounded-lg font-mono text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                placeholder='{"nodes": [], "edges": []}'
              ></textarea>
              <div class="flex gap-2 mt-3">
                <button @click="saveJson" class="px-3 py-1 bg-blue-100 text-blue-800 rounded-lg hover:bg-blue-200 transition-colors text-sm">
                  保存JSON
                </button>
                <button @click="validateJson" class="px-3 py-1 bg-green-100 text-green-800 rounded-lg hover:bg-green-200 transition-colors text-sm">
                  验证JSON
                </button>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="flex items-center justify-between">
            <div class="flex gap-3">
              <button 
                type="button" 
                @click="saveWorkflow" 
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
                  {{ currentWorkflow.id ? '更新工作流' : '创建工作流' }}
                </span>
              </button>
              <button 
                @click="cancelEdit" 
                class="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors focus:outline-none focus:ring-2 focus:ring-gray-500"
              >
                取消
              </button>
              <button 
                v-if="currentWorkflow.id"
                @click="executeWorkflow" 
                class="px-4 py-2 bg-green-100 text-green-800 rounded-lg hover:bg-green-200 transition-colors focus:outline-none focus:ring-2 focus:ring-green-500"
              >
                执行工作流
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
      </div>
    </div>
  </div>
</template>

<script>
import { authFetch } from '../auth.js';

export default {
  name: 'WorkflowDesigner',
  data() {
    return {
      workflows: [],
      currentWorkflow: null,
      workflowJsonStr: '',
      loadingWorkflows: false,
      msg: '',
      saving: false,
      selectedNode: null,
      nodeTypes: [
        { type: 'start', label: '开始节点' },
        { type: 'llm', label: 'LLM节点' },
        { type: 'http', label: 'HTTP节点' },
        { type: 'knowledge', label: '知识库节点' },
        { type: 'intent', label: '意图识别节点' },
        { type: 'string', label: '字符串处理节点' },
        { type: 'end', label: '结束节点' }
      ]
    };
  },
  async mounted() {
    await this.loadWorkflows();
  },
  watch: {
    currentWorkflow: {
      handler(workflow) {
        if (workflow) {
          // 确保nodes和edges是数组
          if (!Array.isArray(workflow.nodes)) {
            workflow.nodes = [];
          }
          if (!Array.isArray(workflow.edges)) {
            workflow.edges = [];
          }
          this.workflowJsonStr = JSON.stringify({
            nodes: workflow.nodes,
            edges: workflow.edges
          }, null, 2);
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
    
    getNodeLabel(node) {
      const nodeType = this.nodeTypes.find(type => type.type === node.type);
      return nodeType ? nodeType.label : node.type;
    },
    
    async loadWorkflows() {
      this.loading = true;
      try {
        const res = await authFetch('/api/workflows');
        const data = await res.json();
        this.workflows = Array.isArray(data) ? data : [];
      } catch (error) {
        console.error('加载工作流失败:', error);
        this.msg = '加载工作流失败: ' + (error.message || '未知错误');
      } finally {
        this.loading = false;
      }
    },
    
    async createWorkflow() {
      if (!this.currentWorkflow.name) {
        this.msg = '请输入工作流名称';
        return;
      }
      
      this.creating = true;
      try {
        const workflowData = {
          name: this.currentWorkflow.name,
          description: this.currentWorkflow.description,
          graph: this.currentWorkflow.graph || { nodes: [], edges: [] },
          status: 'active'
        };
        
        const res = await authFetch('/api/workflows', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(workflowData)
        });
        
        if (res.ok) {
          const newWorkflow = await res.json();
          this.workflows.push(newWorkflow);
          this.currentWorkflow = { name: '', description: '' };
          this.msg = '工作流创建成功';
        } else {
          const errorText = await res.text();
          this.msg = '创建失败: ' + errorText;
        }
      } catch (error) {
        this.msg = '创建失败: ' + (error.message || '未知错误');
        console.error('创建工作流失败:', error);
      } finally {
        this.creating = false;
      }
    },
    
    async updateWorkflow() {
      if (!this.currentWorkflow.id) {
        this.msg = '请先选择一个工作流';
        return;
      }
      
      this.updating = true;
      try {
        const workflowData = {
          name: this.currentWorkflow.name,
          description: this.currentWorkflow.description,
          graph: this.currentWorkflow.graph || { nodes: [], edges: [] },
          status: this.currentWorkflow.status
        };
        
        const res = await authFetch(`/api/workflows/${this.currentWorkflow.id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(workflowData)
        });
        
        if (res.ok) {
          const updatedWorkflow = await res.json();
          const index = this.workflows.findIndex(w => w.id === updatedWorkflow.id);
          if (index !== -1) {
            this.workflows[index] = updatedWorkflow;
          }
          this.msg = '工作流更新成功';
        } else {
          const errorText = await res.text();
          this.msg = '更新失败: ' + errorText;
        }
      } catch (error) {
        this.msg = '更新失败: ' + (error.message || '未知错误');
        console.error('更新工作流失败:', error);
      } finally {
        this.updating = false;
      }
    },
    
    async deleteWorkflow(id) {
      if (!confirm('确定要删除这个工作流吗？')) {
        return;
      }
      
      try {
        const res = await authFetch(`/api/workflows/${id}`, { method: 'DELETE' });
        
        if (res.ok) {
          this.workflows = this.workflows.filter(w => w.id !== id);
          if (this.currentWorkflow.id === id) {
            this.currentWorkflow = { name: '', description: '', graph: { nodes: [], edges: [] } };
          }
          this.msg = '工作流删除成功';
        } else {
          const errorText = await res.text();
          this.msg = '删除失败: ' + errorText;
        }
      } catch (error) {
        this.msg = '删除失败: ' + (error.message || '未知错误');
        console.error('删除工作流失败:', error);
      }
    },
    
    async executeWorkflow() {
      if (!this.currentWorkflow.id) {
        this.msg = '请先选择一个工作流';
        return;
      }
      
      this.executing = true;
      try {
        const res = await authFetch(`/api/workflows/${this.currentWorkflow.id}/execute`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({}) // 可以根据需要传递参数
        });
        
        if (res.ok) {
          const executionResult = await res.json();
          this.executionResult = executionResult;
          this.msg = '工作流执行成功';
        } else {
          const errorText = await res.text();
          this.msg = '执行失败: ' + errorText;
        }
      } catch (error) {
        this.msg = '执行失败: ' + (error.message || '未知错误');
        console.error('执行工作流失败:', error);
      } finally {
        this.executing = false;
      }
    }
  }
};
</script>