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

**启动后端:**
```bash
cd backend
# 确保 JAVA_HOME 指向 Java 25
mvn spring-boot:run
```

**启动前端:**
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
curl http://localhost:11435/health
```

## 📄 Licence

MIT License

## 🤝 Contribution

Les Issues et Pull Requests sont les bienvenues !

---

**Commencer : Exécutez `.\start-all.bat` puis accédez à http://localhost:3000** 🎉
