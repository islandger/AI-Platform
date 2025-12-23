# AI 智能平台

一个基于前后端分离架构的AI智能平台，提供智能体编辑、知识库管理、模型管理、插件管理和工作流设计等功能。

## 🚀 功能特性

### 1. 智能体编辑 (AgentEditor)
- 创建和管理智能体
- 配置提示词、大模型、插件和知识库
- 测试智能体功能

### 2. 知识库管理 (KnowledgeBase)
- 创建和管理知识库
- 支持多种文档格式的上传
- 知识库搜索功能

### 3. 模型管理 (ModelManager)
- 管理系统中可用的AI模型
- 配置API密钥和模型参数

### 4. 插件管理 (PluginManager)
- 注册和管理插件
- 配置插件参数
- 查看插件详情

### 5. 工作流设计器 (WorkflowDesigner)
- 创建和管理工作流
- 配置工作流节点和连接
- 执行工作流

## 🛠 技术栈

### 前端
- **框架**: Vue 3
- **构建工具**: Vite
- **UI组件**: 自定义CSS

### 后端
- **框架**: Spring Boot 3.2.0
- **语言**: Java 17
- **ORM**: Spring Data JPA
- **数据库**: MySQL 8.0
- **认证**: JWT (JSON Web Token)

### 部署
- **容器化**: Docker Compose

## 📦 项目结构

```
├── backend/                  # 后端服务
│   ├── src/                 # 后端源代码
│   ├── pom.xml             # Maven依赖配置
│   └── Dockerfile          # Docker构建文件
├── web-vue/                 # 前端应用
│   ├── src/                 # 前端源代码
│   │   ├── components/     # Vue组件
│   │   ├── App.vue         # 主应用组件
│   │   └── main.js         # 应用入口
│   ├── package.json        # npm依赖配置
│   └── vite.config.js      # Vite配置
├── deploy/                  # 部署配置
│   ├── docker-compose.yml  # Docker Compose配置
│   └── init.sql            # 数据库初始化脚本
└── README.md               # 项目说明文档
```

## 🚀 快速开始

### 环境要求
- JDK 17+
- Node.js 16+
- Docker & Docker Compose

### 1. 部署数据库

使用Docker Compose启动MySQL数据库和Adminer管理工具，初始化脚本会自动创建数据库和表结构：

```bash
cd deploy
docker-compose up -d
```

**数据库初始化说明**：
- 自动创建`ai_platform`数据库
- 创建所有必要的表结构（智能体、工作流、知识库、文档、插件、用户等）
- 添加示例用户：`username=demo, password=demo`

### 2. 配置数据库连接

在后端项目中配置数据库连接信息（application.properties或application.yml）：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ai_platform?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=ai_user
spring.datasource.password=ai_pass
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# JWT配置（可选，使用环境变量更安全）
# jwt.secret=your_jwt_secret_key
```

### 3. 启动后端服务

```bash
cd backend
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动。

### 4. 启动前端应用

```bash
cd web-vue
npm install
npm run dev
```

前端应用将在 `http://localhost:5173` 启动。

## 🔧 开发说明

### 前端开发

```bash
# 安装依赖
npm install

# 开发模式运行
npm run dev

# 构建生产版本
npm run build

# 预览生产构建
npm run preview
```

### 后端开发

```bash
# 编译和运行
mvn spring-boot:run

# 运行测试
mvn test

# 构建可执行JAR
mvn package
```

## 📝 使用说明

1. **注册/登录**：首次使用需要注册账号，然后登录系统
2. **创建智能体**：在智能体编辑模块创建新的智能体
3. **配置知识库**：在知识库管理模块上传文档并创建知识库
4. **选择模型**：在模型管理模块配置可用的AI模型
5. **添加插件**：在插件管理模块注册和配置插件
6. **设计工作流**：在工作流设计器模块创建和执行工作流

## 📄 许可证

MIT License

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📧 联系方式

如有问题，请随时联系项目维护者。

