# 🚀 AICodeMentor

Plateforme d'apprentissage de programmation pilotée par l'IA - Génération et évaluation automatiques d'exercices de programmation avec LLM local.

## 📋 Table des matières

1. [Présentation](#-présentation)
2. [Fonctionnalités](#-fonctionnalités)
3. [Architecture technique](#-architecture-technique)
4. [Prérequis](#-prérequis)
5. [Installation](#-installation)
6. [Démarrage](#-démarrage)
7. [Configuration](#-configuration)
8. [Utilisation](#-utilisation)
9. [Dépannage](#-dépannage)
10. [Architecture du code](#-architecture-du-code)
11. [Développement](#-développement)

---

## 🎯 Présentation

AICodeMentor est une plateforme éducative qui utilise l'intelligence artificielle pour générer automatiquement des exercices de programmation et fournir des retours personnalisés aux étudiants. Le système utilise un modèle de langage local (llama.cpp) pour fonctionner entièrement hors ligne, garantissant la confidentialité des données.

### Objectifs

- **Pour les enseignants** : Créer rapidement des exercices de programmation de qualité
- **Pour les étudiants** : Pratiquer la programmation avec des retours instantanés et des indices intelligents
- **Pour les institutions** : Solution open-source, locale et respectueuse de la vie privée

---

## ✨ Fonctionnalités

### 👨‍🏫 Pour les enseignants

- **🤖 Génération d'exercices par IA**
  - Description en langage naturel → Exercice complet généré automatiquement
  - Inclut : énoncé, code de départ, tests unitaires, solution, exemples

- **✏️ Éditeur visuel**
  - Éditeur Monaco (même moteur que VS Code)
  - Modification du code généré et des tests
  - Prévisualisation en temps réel

- **📊 Gestion des étudiants**
  - Visualisation des soumissions
  - Statistiques de performance
  - Suivi des progrès

- **📝 Publication d'exercices**
  - Contrôle de la visibilité (publié/brouillon)
  - Organisation par thème et difficulté
  - Recherche et filtrage

### 👨‍🎓 Pour les étudiants

- **💻 Édition de code en ligne**
  - Éditeur Monaco avec coloration syntaxique
  - Support multi-langages (actuellement Java)
  - Auto-complétion et validation

- **✅ Tests automatiques**
  - Exécution de tests unitaires en temps réel
  - Retour immédiat sur les résultats
  - Affichage des erreurs de compilation et d'exécution

- **💡 Indices intelligents**
  - Indices personnalisés générés par IA
  - Analyse du code de l'étudiant
  - Suggestions contextuelles sans révéler la solution

---

## 🏗️ Architecture technique

### Frontend

| Technologie | Version | Rôle |
|------------|---------|------|
| Vue.js | 3.4+ | Framework frontend |
| Vite | 5.2+ | Build tool et serveur de développement |
| Bootstrap | 5.3+ | Framework CSS |
| Monaco Editor | 0.45+ | Éditeur de code |
| Vue Router | 4.2+ | Routage |
| Axios | 1.6+ | Client HTTP |

### Backend

| Technologie | Version | Rôle |
|------------|---------|------|
| Spring Boot | 3.2+ | Framework backend |
| Spring Data JPA | - | Accès aux données |
| H2 Database | - | Base de données (fichier) |
| JUnit 5 | - | Exécution de tests |
| Maven | 3.6+ | Gestion des dépendances |

### Intelligence artificielle

| Composant | Description |
|-----------|-------------|
| llama.cpp | Serveur d'inférence local |
| deepseek-coder-6.7b | Modèle de langage spécialisé en code |
| Format | GGUF (quantisé Q2_K, ~2.5GB) |

### Ports des services

| Service | Port | URL |
|---------|------|-----|
| Frontend | 3000 | http://localhost:3000 |
| Backend API | 8080 | http://localhost:8080 |
| llama.cpp | 11435 | http://localhost:11435 |

---

## 📋 Prérequis

### Logiciels requis

1. **Java 25 JDK** (obligatoire)
   - Télécharger depuis [Oracle](https://www.oracle.com/java/technologies/downloads/) ou [OpenJDK](https://jdk.java.net/)
   - **Important** : Configurer la variable d'environnement `JAVA_HOME`

2. **Node.js 18+** (pour le frontend)
   - Télécharger depuis [nodejs.org](https://nodejs.org/)

3. **Maven 3.6+** (pour le backend)
   - Télécharger depuis [maven.apache.org](https://maven.apache.org/download.cgi)

### Configuration de Java 25

#### Windows

```powershell
# 1. Définir JAVA_HOME (session actuelle)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"

# 2. Ajouter au PATH
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# 3. Vérifier l'installation
java -version
javac -version
```

**Configuration permanente** :
1. Ouvrir "Paramètres système" → "Variables d'environnement"
2. Créer une variable système : `JAVA_HOME = C:\Program Files\Java\jdk-25`
3. Modifier `Path` : ajouter `%JAVA_HOME%\bin`

#### Linux / macOS

```bash
# 1. Définir JAVA_HOME (session actuelle)
export JAVA_HOME=/usr/lib/jvm/java-25
export PATH=$JAVA_HOME/bin:$PATH

# 2. Vérifier l'installation
java -version
javac -version
```

**Configuration permanente** :
```bash
# Ajouter à ~/.bashrc ou ~/.zshrc
echo 'export JAVA_HOME=/usr/lib/jvm/java-25' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

---

## 📦 Installation

### Étape 1 : Cloner le projet

```bash
git clone <repository-url>
cd ai-code-mentor-main
```

### Étape 2 : Installer les dépendances frontend

```bash
npm install
```

### Étape 3 : Télécharger le modèle IA

Le modèle sera téléchargé automatiquement lors de la première compilation :

```bash
cd backend
mvn process-classes
```

**Note** : Le téléchargement prend environ 10-20 minutes (modèle de ~2.5GB).

Le modèle sera placé dans : `llama-cpp/models/deepseek-coder-6.7b-instruct.Q2_K.gguf`

---

## 🚀 Démarrage

### Option A : Démarrage automatique (recommandé pour débutants)

**Windows** :
```powershell
.\start-all.bat
```

**Linux / macOS** :
```bash
./start-all.sh
```

Cette commande démarre automatiquement :
1. llama.cpp (mode CPU)
2. Backend Spring Boot
3. Frontend Vue.js

Attendre que tous les services soient prêts, puis accéder à : **http://localhost:3000**

### Option B : Démarrage manuel (recommandé pour GPU)

#### 1. Démarrer llama.cpp

**Mode CPU** (par défaut) :
```bash
cd llama-cpp
# Windows
.\server.exe -m models\deepseek-coder-6.7b-instruct.Q2_K.gguf -ngl 0 -c 4096 --port 11435

# Linux / macOS
./server -m models/deepseek-coder-6.7b-instruct.Q2_K.gguf -ngl 0 -c 4096 --port 11435
```

**Mode GPU** (voir section [Accélération GPU](#-accélération-gpu) pour les détails)

#### 2. Démarrer le backend

```bash
cd backend
mvn spring-boot:run
```

Vérifier que le backend est prêt : http://localhost:8080/api/exercises

#### 3. Démarrer le frontend

```bash
npm run dev
```

Accéder à : **http://localhost:3000**

---

## ⚙️ Configuration

### Configuration du backend

Fichier : `backend/src/main/resources/application.yml`

```yaml
# Port du serveur
server:
  port: 8080

# Base de données H2
spring:
  datasource:
    url: jdbc:h2:file:./data/testdb
    username: sa
    password: password
  
  # Console H2 (développement)
  h2:
    console:
      enabled: true
      path: /h2-console

# Configuration LLM
llm:
  provider: llamacpp
  llamacpp:
    base-url: http://localhost:11435
```

### Configuration du frontend

Fichier : `vite.config.js`

Le proxy est configuré pour rediriger `/api` vers `http://localhost:8080` en développement.

---

## ⚡ Accélération GPU

### NVIDIA GPU (CUDA)

**Prérequis** :
- NVIDIA GPU avec support CUDA
- CUDA Toolkit installé
- Version CUDA de llama.cpp

**Démarrage** :
```bash
cd llama-cpp
.\server.exe -m models\deepseek-coder-6.7b-instruct.Q2_K.gguf -ngl 35 -c 4096 --port 11435
```

**Paramètres recommandés** :
- `-ngl 35` : 35 couches sur GPU (ajuster selon la mémoire GPU)
- `-c 4096` : Taille du contexte
- `-t 4` : Threads CPU (pour les couches restantes)

**Vérification** :
```bash
nvidia-smi  # Vérifier l'utilisation GPU
```

### Intel GPU intégré (Vulkan)

**Prérequis** :
- Pilotes Vulkan installés
- Version Vulkan de llama.cpp

**Démarrage** :
```bash
cd llama-cpp
./server -m models/deepseek-coder-6.7b-instruct.Q2_K.gguf -ngl 15 -c 4096 -t 4 --port 11435
```

**Paramètres recommandés** :
- `-ngl 15` : 15 couches sur GPU Vulkan
- Réduire à `-ngl 10` ou `-ngl 5` si mémoire insuffisante

### Paramètres de performance

| Mode | Vitesse | Utilisation |
|------|---------|-------------|
| CPU | ~5-6 tokens/s | 100% CPU |
| NVIDIA GPU | ~20-50 tokens/s | GPU + CPU partiel |
| Intel Vulkan | ~8-15 tokens/s | GPU intégré + CPU |

---

## 🎯 Utilisation

### Workflow enseignant

1. **Créer un exercice**
   - Accéder à "Créer un exercice"
   - Décrire l'exercice en langage naturel (ex: "Écrire une fonction qui inverse une chaîne")
   - L'IA génère automatiquement :
     - Énoncé détaillé
     - Code de départ
     - Tests unitaires
     - Solution
     - Exemples

2. **Modifier et personnaliser**
   - Utiliser l'éditeur pour ajuster le code
   - Modifier les tests si nécessaire
   - Ajouter des indices ou des explications

3. **Publier**
   - Activer le statut "Publié"
   - L'exercice devient visible pour les étudiants

### Workflow étudiant

1. **Parcourir les exercices**
   - Consulter la liste des exercices publiés
   - Filtrer par thème ou difficulté
   - Sélectionner un exercice

2. **Résoudre l'exercice**
   - Lire l'énoncé et les exemples
   - Écrire le code dans l'éditeur
   - Cliquer sur "Exécuter les tests"

3. **Obtenir de l'aide**
   - Si les tests échouent, consulter les erreurs
   - Demander un indice (généré automatiquement par IA)
   - Itérer jusqu'à résolution

---

## 🐛 Dépannage

### Problème : Port déjà utilisé

**Solution** :
```bash
# Arrêter tous les services
.\stop-all.bat  # Windows
./stop-all.sh   # Linux/Mac

# Attendre 5 secondes, puis redémarrer
.\start-all.bat
```

### Problème : Frontend vide ou erreurs

1. Ouvrir la console du navigateur (F12)
2. Vérifier les erreurs dans l'onglet "Console"
3. Vérifier que le backend est accessible : http://localhost:8080/api/exercises

### Problème : Génération IA échoue

**Vérifier llama.cpp** :
```bash
# Windows PowerShell
Invoke-WebRequest -Uri http://localhost:11435/health

# Linux/Mac
curl http://localhost:11435/health
```

**Si non accessible** :
1. Vérifier que llama.cpp est démarré
2. Vérifier le port (11435)
3. Vérifier les logs pour les erreurs

### Problème : GPU ne fonctionne pas

**NVIDIA GPU** :
1. Vérifier CUDA : `nvidia-smi`
2. Vérifier la présence de `ggml-cuda.dll` (Windows) ou `libggml-cuda.so` (Linux)
3. Si problème, utiliser mode CPU : `-ngl 0`

**Intel GPU** :
1. Vérifier Vulkan : `vulkaninfo` (si installé)
2. Si problème, utiliser mode CPU : `-ngl 0`

**Solution universelle** : Utiliser le mode CPU pur
```bash
.\server.exe -m models\deepseek-coder-6.7b-instruct.Q2_K.gguf -ngl 0 -c 4096 --port 11435
```

### Problème : Erreur de compilation Java

**Vérifier Java 25** :
```bash
java -version  # Doit afficher version 25
echo $JAVA_HOME  # Doit pointer vers Java 25
```

**Si problème** :
1. Réinstaller Java 25
2. Reconfigurer `JAVA_HOME`
3. Redémarrer le terminal

---

## 📚 Architecture du code

### Structure du projet

```
ai-code-mentor-main/
├── backend/                          # Backend Spring Boot
│   ├── src/main/java/
│   │   └── com/aicodementor/
│   │       ├── controller/           # Contrôleurs REST API
│   │       │   ├── ExerciseController.java
│   │       │   ├── LLMController.java
│   │       │   ├── SubmissionController.java
│   │       │   ├── UserController.java
│   │       │   └── StatsController.java
│   │       ├── service/              # Logique métier
│   │       │   ├── LLMService.java
│   │       │   └── CodeExecutionService.java
│   │       ├── entity/               # Entités JPA
│   │       │   ├── Exercise.java
│   │       │   ├── User.java
│   │       │   ├── Submission.java
│   │       │   └── KnowledgeBase.java
│   │       ├── repository/           # Accès aux données
│   │       ├── dto/                  # Objets de transfert
│   │       └── config/               # Configuration
│   │           ├── CorsConfig.java
│   │           ├── GlobalExceptionHandler.java
│   │           └── DataInitializer.java
│   └── pom.xml
├── src/                              # Frontend Vue.js
│   ├── views/                        # Pages
│   │   ├── Home.vue
│   │   ├── CreateExercise.vue
│   │   ├── ExerciseList.vue
│   │   ├── ExerciseDetail.vue
│   │   └── ExerciseTest.vue
│   ├── components/                   # Composants réutilisables
│   │   ├── CodeEditor.vue
│   │   └── Navbar.vue
│   ├── services/                     # Services API
│   │   ├── api.js
│   │   └── llmApi.js
│   └── router/                       # Routage
├── llama-cpp/                        # Binaires et modèles
│   ├── server.exe / server           # Serveur llama.cpp
│   └── models/
│       └── deepseek-coder-6.7b-instruct.Q2_K.gguf
├── start-all.bat / start-all.sh     # Scripts de démarrage
└── package.json
```

### Flux de données

```
Frontend (Vue.js)
    ↓ HTTP
Backend (Spring Boot)
    ↓ JPA
Base de données (H2)
    ↓
Backend
    ↓ HTTP
llama.cpp (LLM local)
```

### API REST

#### Exercices
- `GET /api/exercises` - Liste paginée des exercices
- `GET /api/exercises/{id}` - Détails d'un exercice
- `POST /api/exercises` - Créer un exercice
- `PUT /api/exercises/{id}` - Modifier un exercice
- `DELETE /api/exercises/{id}` - Supprimer un exercice
- `GET /api/exercises/published` - Exercices publiés

#### LLM
- `POST /api/llm/generate-exercise` - Générer un exercice
- `POST /api/llm/save-exercise` - Sauvegarder un exercice généré
- `POST /api/llm/execute-tests` - Exécuter des tests
- `POST /api/llm/get-hint` - Obtenir un indice

#### Soumissions
- `GET /api/submissions` - Liste des soumissions
- `POST /api/submissions` - Créer une soumission
- `GET /api/submissions/user/{userId}` - Soumissions d'un utilisateur

---

## 🛡️ Gestion des exceptions

Le projet utilise une gestion centralisée des exceptions via `GlobalExceptionHandler`.

### Types d'exceptions gérées

| Exception | Code HTTP | Usage |
|-----------|-----------|-------|
| `IllegalArgumentException` | 400 | Paramètres invalides |
| `DataIntegrityViolationException` | 400 | Violation contraintes DB |
| `ConstraintViolationException` | 400 | Erreur de validation |
| `NoHandlerFoundException` | 404 | Endpoint non trouvé |
| `Exception` | 500 | Erreur générique |

### Bonnes pratiques

✅ **À faire** :
- Utiliser `logger.error()`, `logger.warn()` pour les logs
- Types de retour explicites : `ResponseEntity<Page<Exercise>>`
- Capturer des exceptions spécifiques
- Laisser les exceptions remonter au `GlobalExceptionHandler`
- Utiliser `@Transactional` pour les opérations DB

❌ **À éviter** :
- `printStackTrace()` ou `System.out.println()`
- `ResponseEntity<?>` (type générique)
- `catch (Exception e)` générique
- Gérer manuellement chaque exception

### Hibernate Lazy Loading

**Problème** : Accès aux associations lazy après fermeture de transaction

**Solution** : Précharger dans la transaction
```java
@Transactional(readOnly = true)
public ResponseEntity<Page<Exercise>> getAllExercises(...) {
    Page<Exercise> exercises = exerciseRepository.findAll(pageable);
    
    // Précharger avant la fin de la transaction
    exercises.getContent().forEach(ex -> {
        if (ex.getCreator() != null) {
            Hibernate.initialize(ex.getCreator());
            ex.getCreator().getId();
        }
    });
    
    return ResponseEntity.ok(exercises);
}
```

---

## 💻 Développement

### Compilation

**Backend** :
```bash
cd backend
mvn clean compile
```

**Frontend** :
```bash
npm run build
```

### Tests

**Backend** :
```bash
cd backend
mvn test
```

### Base de données

**Console H2** : http://localhost:8080/h2-console

**Connexion** :
- JDBC URL: `jdbc:h2:file:./data/testdb`
- Username: `sa`
- Password: `password`

### Logs

Les logs sont configurés dans `application.yml` :
```yaml
logging:
  level:
    com.aicodementor: DEBUG
```

---

## 📄 Licence

MIT License

## 🤝 Contribution

Les contributions sont les bienvenues ! N'hésitez pas à :
- Ouvrir une Issue pour signaler un bug
- Proposer une Pull Request
- Améliorer la documentation

---

**🚀 Pour commencer : Exécutez `.\start-all.bat` puis accédez à http://localhost:3000**
