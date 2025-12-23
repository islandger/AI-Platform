<template>
  <div class="p-4 md:p-6 animate-fade-in">
    <h2 class="text-2xl font-bold text-blue-700 mb-4">知识库管理</h2>
    <p class="text-gray-600 mb-6 text-sm md:text-base">创建和管理知识库，支持多种文档格式的上传和搜索。</p>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- 创建知识库 -->
      <div class="animate-slide-up">
        <h3 class="text-xl font-semibold text-blue-700 mb-4">创建知识库</h3>
        <div class="bg-white p-4 rounded-xl border border-gray-200 shadow-card hover:shadow-card-hover transition-all duration-300">
          <div class="space-y-3">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">名称</label>
              <input 
                v-model="form.name" 
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                placeholder="输入知识库名称"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">描述</label>
              <input 
                v-model="form.description" 
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                placeholder="输入知识库描述"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">文档源类型</label>
              <select 
                v-model="form.source_type" 
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
              >
                <option value="file">文件上传</option>
                <option value="url">URL</option>
                <option value="text">纯文本</option>
              </select>
            </div>
            <div v-if="form.source_type === 'file'">
              <label class="block text-sm font-medium text-gray-700 mb-1">上传文件</label>
              <input 
                type="file" 
                @change="handleFileUpload" 
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 cursor-pointer"
                multiple
              />
              <div v-if="selectedFiles.length > 0" class="mt-2">
                <div v-for="(file, index) in selectedFiles" :key="index" class="text-sm text-gray-600 bg-gray-50 p-2 rounded mb-1 flex justify-between items-center">
                  {{ file.name }}
                  <button @click="removeFile(index)" class="text-red-600 hover:text-red-800">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                    </svg>
                  </button>
                </div>
              </div>
            </div>
            <div v-if="form.source_type === 'url'">
              <label class="block text-sm font-medium text-gray-700 mb-1">URL</label>
              <input 
                v-model="form.source_url" 
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                placeholder="输入网页URL"
              />
            </div>
            <div v-if="form.source_type === 'text'">
              <label class="block text-sm font-medium text-gray-700 mb-1">文本内容</label>
              <textarea 
                v-model="form.source_text" 
                rows="4" 
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                placeholder="输入文本内容"
              ></textarea>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">向量维度</label>
              <input 
                v-model.number="form.vector_dimension" 
                type="number" 
                min="100" 
                max="2000" 
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                placeholder="输入向量维度 (默认: 1024)"
              />
            </div>
            <div class="flex items-center space-x-3 mt-4">
              <button 
                @click="createKnowledgeBase" 
                class="px-4 py-2 bg-gradient-to-r from-blue-500 to-blue-600 text-white rounded-lg hover:shadow-button-hover transition-all duration-300 focus:outline-none focus:ring-2 focus:ring-blue-500 shadow-button transform hover:-translate-y-0.5"
                :disabled="creating"
              >
                <span v-if="creating" class="inline-flex items-center">
                  <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  创建中...
                </span>
                <span v-else class="inline-flex items-center">
                  <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
                  </svg>
                  创建
                </span>
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

        <!-- 知识库列表 -->
        <h3 class="text-xl font-semibold text-blue-700 mt-6 mb-4">知识库列表</h3>
        <div v-if="loading" class="text-center py-6 bg-white rounded-xl border border-gray-200 shadow-card">
          <svg class="animate-spin mx-auto h-8 w-8 text-blue-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <p class="mt-2 text-sm text-gray-500">加载知识库列表...</p>
        </div>
        <div v-else-if="knowledge_bases.length===0" class="text-center py-6 bg-white rounded-xl border border-gray-200 shadow-card text-gray-500">
          暂无知识库
        </div>
        <div v-else class="space-y-4">
          <div 
            v-for="kb in knowledge_bases" 
            :key="kb.id" 
            class="border border-gray-200 rounded-xl p-4 bg-white shadow-card hover:shadow-card-hover transition-all duration-300"
          >
            <div class="flex flex-col space-y-2">
              <div class="flex items-center justify-between">
                <strong class="text-lg text-blue-700">{{ kb.name }}</strong>
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium" :class="getStatusBadgeClass(kb.status)">
                  {{ kb.status }}
                </span>
              </div>
              <div class="text-sm text-gray-600">{{ kb.description }}</div>
              <div class="flex gap-2 mt-3">
                <button @click="viewKnowledgeBase(kb)" class="px-3 py-1 bg-blue-100 text-blue-800 rounded-lg hover:bg-blue-200 transition-colors text-sm">
                  查看
                </button>
                <button @click="deleteKnowledgeBase(kb.id)" class="px-3 py-1 bg-red-100 text-red-800 rounded-lg hover:bg-red-200 transition-colors text-sm">
                  删除
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 知识库详情 -->
      <div class="animate-slide-up" style="animation-delay: 0.2s;">
        <h3 class="text-xl font-semibold text-blue-700 mb-4">
          {{ selectedKB ? selectedKB.name + ' - 详情' : '知识库详情' }}
        </h3>
        <div v-if="selectedKB" class="bg-white p-4 rounded-xl border border-gray-200 shadow-card hover:shadow-card-hover transition-all duration-300 mb-6">
          <div class="space-y-4">
            <div>
              <h4 class="text-lg font-medium text-blue-700 mb-2">{{ selectedKB.name }}</h4>
              <p class="text-gray-600">{{ selectedKB.description }}</p>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <div class="text-sm text-gray-500 mb-1">文档数量</div>
                <div class="text-lg font-medium">{{ selectedKB.document_count || 0 }}</div>
              </div>
              <div>
                <div class="text-sm text-gray-500 mb-1">向量维度</div>
                <div class="text-lg font-medium">{{ selectedKB.vector_dimension || 1024 }}</div>
              </div>
              <div>
                <div class="text-sm text-gray-500 mb-1">源类型</div>
                <div class="text-lg font-medium">{{ selectedKB.source_type || '混合' }}</div>
              </div>
              <div>
                <div class="text-sm text-gray-500 mb-1">创建时间</div>
                <div class="text-lg font-medium">{{ formatDate(selectedKB.created_at) }}</div>
              </div>
            </div>
            <div class="pt-2 border-t border-gray-200">
              <h5 class="text-sm font-medium text-gray-700 mb-2">文档列表</h5>
              <div v-if="selectedKB.documents && selectedKB.documents.length > 0" class="space-y-2">
                <div 
                  v-for="(doc, index) in selectedKB.documents" 
                  :key="index"
                  class="text-sm text-gray-600 bg-gray-50 p-2 rounded flex justify-between items-center"
                >
                  <div>
                    <span class="font-medium">{{ doc.title || '文档 ' + (index + 1) }}</span>
                    <span class="ml-2 text-gray-500">({{ doc.type || '未知' }})</span>
                  </div>
                  <button @click="deleteDocument(selectedKB.id, doc.id)" class="text-red-600 hover:text-red-800">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                    </svg>
                  </button>
                </div>
              </div>
              <div v-else class="text-center py-4 text-gray-500">
                暂无文档
              </div>
            </div>
          </div>
        </div>
        <div v-else class="bg-white p-6 rounded-xl border border-gray-200 shadow-card hover:shadow-card-hover transition-all duration-300 mb-6 text-center text-gray-500">
          
          请从左侧选择一个知识库查看详情
        </div>

        <!-- 知识库搜索 -->
        <div v-if="selectedKB">
          <h3 class="text-xl font-semibold text-blue-700 mb-4">知识库搜索</h3>
          <div class="bg-white p-4 rounded-xl border border-gray-200 shadow-card hover:shadow-card-hover transition-all duration-300">
            <div class="space-y-3">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">搜索查询</label>
                <input 
                  v-model="searchQuery" 
                  @keyup.enter="searchKnowledgeBase" 
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-300"
                  placeholder="输入搜索内容..."
                />
              </div>
              <div class="flex items-center space-x-3">
                <button 
                  @click="searchKnowledgeBase" 
                  class="px-4 py-2 bg-gradient-to-r from-blue-500 to-blue-600 text-white rounded-lg hover:shadow-button-hover transition-all duration-300 focus:outline-none focus:ring-2 focus:ring-blue-500 shadow-button transform hover:-translate-y-0.5"
                  :disabled="searching"
                >
                  <span v-if="searching" class="inline-flex items-center">
                    <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    搜索中...
                  </span>
                  <span v-else class="inline-flex items-center">
                    <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                    </svg>
                    搜索
                  </span>
                </button>
              </div>
              <div v-if="searchResults && searchResults.length > 0" class="space-y-3">
                <div class="text-sm font-medium text-gray-700 mb-2">搜索结果 ({{ searchResults.length }})</div>
                <div 
                  v-for="(result, index) in searchResults" 
                  :key="index"
                  class="p-3 bg-gray-50 rounded-lg border border-gray-200"
                >
                  <div class="text-sm text-gray-600">{{ result.content }}</div>
                  <div class="text-xs text-gray-500 mt-1">相关性: {{ (result.score * 100).toFixed(2) }}%</div>
                </div>
              </div>
              <div v-if="searchResults && searchResults.length === 0 && searchQuery" class="text-center py-4 text-gray-500">
                未找到相关结果
              </div>
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
  name: 'KnowledgeBase',
  data() {
    return {
      form: {
        name: '',
        description: '',
        source_type: 'file',
        source_url: '',
        source_text: '',
        vector_dimension: 1024
      },
      selectedFiles: [],
      knowledge_bases: [],
      selectedKB: null,
      msg: '',
      creating: false,
      loading: false,
      searching: false,
      searchQuery: '',
      searchResults: null
    };
  },
  mounted() {
    this.loadKnowledgeBases();
  },
  methods: {
    getStatusBadgeClass(status) {
      switch(status.toLowerCase()) {
        case 'active':
          return 'bg-green-100 text-green-800';
        case 'inactive':
          return 'bg-red-100 text-red-800';
        case 'processing':
          return 'bg-yellow-100 text-yellow-800';
        default:
          return 'bg-gray-100 text-gray-800';
      }
    },
    
    formatDate(dateString) {
      if (!dateString) return '-';
      const date = new Date(dateString);
      return date.toLocaleString();
    },
    
    async loadKnowledgeBases() {
      this.loading = true;
      try {
        const res = await authFetch('/api/knowledge-bases');
        const data = await res.json();
        this.knowledge_bases = Array.isArray(data) ? data : [];
      } catch (e) {
        this.msg = '加载知识库失败: ' + (e.message || '未知错误');
        console.error('Failed to load knowledge bases:', e);
      } finally {
        this.loading = false;
      }
    },
    
    handleFileUpload(event) {
      this.selectedFiles = Array.from(event.target.files);
    },
    
    removeFile(index) {
      this.selectedFiles.splice(index, 1);
      // 重置文件输入，以便用户可以重新选择相同的文件
      const fileInput = document.querySelector('input[type="file"]');
      if (fileInput) {
        fileInput.value = '';
      }
    },
    
    async createKnowledgeBase() {
      if (!this.form.name) {
        this.msg = '名称必填';
        return;
      }
      
      this.creating = true;
      this.msg = '';
      
      try {
        let payload = {
          name: this.form.name,
          description: this.form.description,
          source_type: this.form.source_type,
          vector_dimension: this.form.vector_dimension
        };
        
        if (this.form.source_type === 'url') {
          payload.source_url = this.form.source_url;
        } else if (this.form.source_type === 'text') {
          payload.source_text = this.form.source_text;
        }
        
        if (this.form.source_type === 'file' && this.selectedFiles.length > 0) {
          // 使用 FormData 上传文件
          const formData = new FormData();
          formData.append('name', this.form.name);
          formData.append('description', this.form.description);
          formData.append('source_type', this.form.source_type);
          formData.append('vector_dimension', this.form.vector_dimension);
          
          // 添加文件
          this.selectedFiles.forEach(file => {
            formData.append('files', file);
          });
          
          const res = await authFetch('/api/knowledge-bases', {
            method: 'POST',
            body: formData
            // 不需要设置 Content-Type，浏览器会自动设置为 multipart/form-data
          });
          
          if (res.ok) {
            this.msg = '创建成功';
            this.resetForm();
            await this.loadKnowledgeBases();
          } else {
            const errorText = await res.text();
            this.msg = '创建失败: ' + errorText;
          }
        } else {
          // 普通 JSON 请求
          const res = await authFetch('/api/knowledge-bases', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
          });
          
          if (res.ok) {
            this.msg = '创建成功';
            this.resetForm();
            await this.loadKnowledgeBases();
          } else {
            const errorText = await res.text();
            this.msg = '创建失败: ' + errorText;
          }
        }
      } catch (e) {
        this.msg = '创建失败: ' + (e.message || '请求失败');
        console.error('Failed to create knowledge base:', e);
      } finally {
        this.creating = false;
      }
    },
    
    resetForm() {
      this.form = {
        name: '',
        description: '',
        source_type: 'file',
        source_url: '',
        source_text: '',
        vector_dimension: 1024
      };
      this.selectedFiles = [];
      // 重置文件输入
      const fileInput = document.querySelector('input[type="file"]');
      if (fileInput) {
        fileInput.value = '';
      }
    },
    
    async viewKnowledgeBase(knowledgeBase) {
      try {
        const res = await authFetch(`/api/knowledge-bases/${knowledgeBase.id}`);
        const data = await res.json();
        this.selectedKB = data;
      } catch (e) {
        this.msg = '加载知识库详情失败: ' + (e.message || '未知错误');
        console.error('Failed to view knowledge base:', e);
      }
    },
    
    async deleteKnowledgeBase(id) {
      if (!confirm('确定要删除这个知识库吗？')) {
        return;
      }
      
      try {
        const res = await authFetch(`/api/knowledge-bases/${id}`, { method: 'DELETE' });
        
        if (res.ok) {
          await this.loadKnowledgeBases();
          if (this.selectedKB && this.selectedKB.id === id) {
            this.selectedKB = null;
          }
        } else {
          const errorText = await res.text();
          this.msg = '删除失败: ' + errorText;
        }
      } catch (e) {
        this.msg = '删除失败: ' + (e.message || '请求失败');
        console.error('Failed to delete knowledge base:', e);
      }
    },
    
    async deleteDocument(knowledgeBaseId, documentId) {
      try {
        const res = await authFetch(`/api/knowledge-bases/${knowledgeBaseId}/documents/${documentId}`, { method: 'DELETE' });
        
        if (res.ok) {
          if (this.selectedKB) {
            await this.viewKnowledgeBase(this.selectedKB);
          }
        } else {
          const errorText = await res.text();
          this.msg = '删除文档失败: ' + errorText;
        }
      } catch (e) {
        this.msg = '删除文档失败: ' + (e.message || '请求失败');
        console.error('Failed to delete document:', e);
      }
    },
    
    async searchKnowledgeBase() {
      if (!this.searchQuery || !this.selectedKB) {
        return;
      }
      
      this.searching = true;
      this.searchResults = null;
      
      try {
        const res = await authFetch(`/api/knowledge-bases/${this.selectedKB.id}/search`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ query: this.searchQuery })
        });
        
        if (res.ok) {
          const data = await res.json();
          this.searchResults = data.results || [];
        } else {
          const errorText = await res.text();
          this.msg = '搜索失败: ' + errorText;
        }
      } catch (e) {
        this.msg = '搜索失败: ' + (e.message || '请求失败');
        console.error('Failed to search knowledge base:', e);
      } finally {
        this.searching = false;
      }
    }
  }
};
</script>