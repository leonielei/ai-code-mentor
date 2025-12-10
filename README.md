# 🚀 AICodeMentor

Plateforme d'apprentissage de programmation pilotée par l'IA - Génération et évaluation automatiques d'exercices de programmation avec LLM local

## ✨ Caractéristiques principales

### 👨‍🏫 Fonctionnalités pour enseignants
- 🤖 **Génération d'exercices par IA** - Décrivez en langage naturel, l'IA génère automatiquement un exercice complet
- ✏️ **Éditeur visuel** - Éditeur Monaco pour modifier le code et les tests
- 📊 **Gestion des étudiants** - Visualiser les soumissions et statistiques

### 👨‍🎓 Fonctionnalités pour étudiants
- 💻 **Édition de code en ligne** - Éditeur Monaco avec support multi-langages
- ✅ **Tests automatiques** - Exécution de tests unitaires en temps réel
- 💡 **Indices intelligents** - Indices personnalisés générés par IA

## 🛠️ Stack technologique

### Frontend
- Vue.js 3 + Vite
- Bootstrap 5
- Monaco Editor
- Vue Router 4
- Axios

### Backend
- Spring Boot 3.2.0
- Spring Data JPA
- H2 Database (en mémoire)
- LangChain4j
- Maven

### IA
- llama.cpp (exécution locale)
- Modèle deepseek-coder-6.7b
- Compatible API LocalAI

## 📦 Démarrage rapide

### Prérequis

- **Java 25** (必须安装并设置 JAVA_HOME 环境变量)
- Node.js 18+
- Maven 3.6+

#### 配置 Java 25

1. **下载并安装 Java 25 JDK**
   - 从 Oracle 或 OpenJDK 官网下载 Java 25
   - 安装到系统（例如：`C:\Program Files\Java\jdk-25` 或 `/usr/lib/jvm/java-25`）

2. **设置 JAVA_HOME 环境变量**
   
   **Windows:**
   ```powershell
   # 临时设置（当前会话）
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-25"
   
   # 永久设置（系统环境变量）
   # 1. 打开"系统属性" -> "高级" -> "环境变量"
   # 2. 新建系统变量：JAVA_HOME = C:\Program Files\Java\jdk-25
   # 3. 编辑 Path 变量，添加：%JAVA_HOME%\bin
   ```
   
   **Linux/Mac:**
   ```bash
   # 临时设置（当前会话）
   export JAVA_HOME=/usr/lib/jvm/java-25
   export PATH=$JAVA_HOME/bin:$PATH
   
   # 永久设置（添加到 ~/.bashrc 或 ~/.zshrc）
   echo 'export JAVA_HOME=/usr/lib/jvm/java-25' >> ~/.bashrc
   echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
   source ~/.bashrc
   ```

3. **验证安装**
   ```bash
   java -version  # 应该显示 java version "25"
   javac -version # 应该显示 javac 25
   echo $JAVA_HOME # 应该显示 Java 25 的安装路径
   ```

### 1️⃣ Installer llama.cpp et le modèle

```bash
mvn process-classes
```

Cela téléchargera environ 4 Go du modèle CodeLlama, ce qui prendra 10-20 minutes.

### 2️⃣ Lancer les services

**选项 A: 使用脚本一键启动（CPU 模式）**
```bash
# Windows
.\start-all.bat

# Linux/Mac
./start-all.sh
```

**选项 B: 手动启动（推荐 GPU 用户）**

1. **启动 llama.cpp (可选，如果使用 GPU 请先手动启动):**
   ```bash
   # 见下方 "Accélération GPU" 部分获取详细命令
   cd llama-cpp
   .\server.exe -m models\deepseek-coder-6.7b-instruct.Q2_K.gguf -ngl 35 -c 4096 --port 11435
   ```

2. **启动后端:**
   ```bash
   cd backend
   # 确保 JAVA_HOME 指向 Java 25
   mvn spring-boot:run
   ```

3. **启动前端:**
   ```bash
   npm run dev
   ```

等待服务启动后，访问：**http://localhost:3000**

## 📋 Ports des services

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 3000 | Serveur de développement Vue.js |
| Backend | 8080 | API Spring Boot |
| IA | 11435 | Serveur llama.cpp |

> **💡 Note:** 默认情况下，`start-all.bat` 会启动 CPU 模式的 llama.cpp。如果您有 NVIDIA GPU 并想使用 GPU 加速，请先手动启动 GPU 版本的 llama-server（见下方"Accélération GPU"部分），然后再启动后端和前端。

## 📚 Structure du projet

```
uni-preset-vue-vite/
├── backend/                    # Backend Spring Boot
│   ├── src/main/java/
│   │   └── com/aicodementor/
│   │       ├── controller/     # Contrôleurs REST API
│   │       ├── service/        # Logique métier et service LLM
│   │       ├── entity/         # Entités JPA
│   │       ├── repository/     # Couche d'accès aux données
│   │       └── dto/            # Objets de transfert de données
│   └── pom.xml
├── src/                        # Frontend Vue.js
│   ├── views/                  # Composants de page
│   ├── components/             # Composants réutilisables
│   ├── router/                 # Configuration de routage
│   ├── services/               # Services API
│   └── assets/                 # Ressources statiques
├── llama-cpp/                  # Binaires et modèles llama.cpp
│   ├── server.exe
│   └── models/
│       └── codellama-7b-instruct.Q4_K_M.gguf
├── start-all.bat              # Script de démarrage en un clic
├── stop-all.bat               # Script d'arrêt en un clic
├── install-llamacpp.bat       # Script d'installation LLM
└── package.json

```

## ⚡ Performances

### Mode CPU (actuel)
- Génération d'exercices : 30 secondes - 2 minutes
- Génération d'indices : 10-30 secondes
- Vitesse : ~5.68 tokens/seconde

### Accélération GPU (optionnel)
Si vous avez une carte graphique NVIDIA, téléchargez la version CUDA de llama.cpp pour une accélération 10-50x !

#### 手动启动 llama.cpp avec GPU

**Windows (PowerShell 或 CMD):**
```powershell
# 进入 llama-cpp 目录
cd llama-cpp

# 启动 server.exe 使用 GPU (CUDA)
# 确保使用支持 CUDA 的版本 (ggml-cuda.dll 必须存在)
.\server.exe `
  -m models\deepseek-coder-6.7b-instruct.Q2_K.gguf `
  -ngl 35 `
  -c 4096 `
  -t 4 `
  -b 512 `
  -n 2048 `
  --port 11435 `
  --host 0.0.0.0 `
  --cont-batching

# 或者使用更简单的命令（推荐）
.\server.exe -m models\deepseek-coder-6.7b-instruct.Q2_K.gguf -ngl 35 -c 4096 --port 11435
```

**Linux/Mac:**
```bash
# 进入 llama-cpp 目录
cd llama-cpp

# 启动 server 使用 GPU (CUDA)
./server \
  -m models/deepseek-coder-6.7b-instruct.Q2_K.gguf \
  -ngl 35 \
  -c 4096 \
  -t 4 \
  -b 512 \
  -n 2048 \
  --port 11435 \
  --host 0.0.0.0 \
  --cont-batching

# 或者使用更简单的命令（推荐）
./server -m models/deepseek-coder-6.7b-instruct.Q2_K.gguf -ngl 35 -c 4096 --port 11435
```

**参数说明:**
- `-m`: 模型文件路径
- `-ngl`: 在 GPU 上运行的层数（0 = 仅 CPU，20-35 = 大部分在 GPU，取决于 GPU 内存）
- `-c`: 上下文窗口大小（4096 或 8192）
- `-t`: CPU 线程数（如果部分层在 CPU 上运行）
- `-b`: 批处理大小（512 或更高，取决于 GPU 内存）
- `-n`: 最大生成 token 数（2048 或更高）
- `--port`: 服务端口（默认 11435，**注意：使用 `--port` 而不是 `-p`**）
- `--host`: 绑定地址（0.0.0.0 允许外部访问）
- `--cont-batching`: 启用连续批处理（提高性能）

**⚠️ 重要提示:**
- 使用 `server.exe` (Windows) 或 `server` (Linux/Mac)，而不是 `llama-server.exe`
- 端口参数是 `--port`，**不是** `-p`
- GPU 层数参数是 `-ngl`，**不是** `--gpu-layers`

**检查 GPU 是否可用:**
```bash
# Windows
.\server.exe --help | findstr ngl

# Linux/Mac
./server --help | grep ngl
```

**查看所有可用参数:**
```bash
# Windows
.\server.exe --help

# Linux/Mac
./server --help
```

**验证 GPU 加速:**
启动后，检查日志中是否有类似信息：
```
llama_model_load_internal: using CUDA for GPU acceleration
llama_model_load_internal: n_gpu_layers = 35
```

**注意事项:**
- 确保安装了 NVIDIA CUDA Toolkit 和 cuDNN
- 确保 `ggml-cuda.dll` (Windows) 或 `libggml-cuda.so` (Linux) 存在于 llama-cpp 目录
- 如果 GPU 内存不足，减少 `--gpu-layers` 参数
- 如果遇到错误，尝试 `--gpu-layers 0` 使用纯 CPU 模式

## 🎯 Flux d'utilisation

### Workflow enseignant

1. Se connecter au système
2. Cliquer sur "Créer un exercice"
3. Décrire les exigences de l'exercice en langage naturel
4. L'IA génère un exercice complet (énoncé, code, tests, solution)
5. Examiner et publier

### Workflow étudiant

1. Se connecter au système
2. Parcourir les exercices disponibles
3. Écrire du code dans l'éditeur Monaco
4. Soumettre et exécuter automatiquement les tests
5. Consulter les résultats et indices générés par IA

## 🐛 Dépannage

### Port occupé
```bash
.\stop-all.bat
# Attendre 5 secondes
.\start-all.bat
```

### Frontend vide
Appuyer sur F12 pour voir les erreurs dans la console du navigateur

### Échec de génération IA
Vérifier que llama.cpp est en cours d'exécution :
```bash
# Windows PowerShell
Invoke-WebRequest -Uri http://localhost:11435/health

# Linux/Mac
curl http://localhost:11435/health
```

### GPU 不工作
1. **检查 CUDA 安装:**
   ```bash
   # Windows
   nvidia-smi
   
   # Linux
   nvidia-smi
   ```

2. **检查 llama.cpp GPU 支持:**
   - 确保下载了支持 CUDA 的版本
   - 检查 `llama-cpp` 目录中是否有 `ggml-cuda.dll` (Windows) 或相关 CUDA 库

3. **降级到 CPU 模式:**
   如果 GPU 有问题，可以手动启动纯 CPU 模式：
   ```bash
   # Windows
   .\server.exe -m models\deepseek-coder-6.7b-instruct.Q2_K.gguf -ngl 0 -c 4096 --port 11435
   
   # Linux/Mac
   ./server -m models/deepseek-coder-6.7b-instruct.Q2_K.gguf -ngl 0 -c 4096 --port 11435
   ```

## 📄 Licence

MIT License

## 🤝 Contribution

Les Issues et Pull Requests sont les bienvenues !

---

**Commencer : Exécutez `.\start-all.bat` puis accédez à http://localhost:3000** 🎉
