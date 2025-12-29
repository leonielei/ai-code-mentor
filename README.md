# 🚀 AICodeMentor

Plateforme d'apprentissage de programmation pilotée par l'IA - Génération et évaluation automatiques d'exercices de programmation avec LLM local.

## 📋 Table des matières

1. [Présentation](#-présentation)
2. [Fonctionnalités](#-fonctionnalités)
3. [Prérequis](#-prérequis)
4. [Installation](#-installation)
5. [Démarrage](#-démarrage)
6. [Configuration](#-configuration)
7. [Utilisation](#-utilisation)
8. [Dépannage](#-dépannage)

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

**Note** : Vous pouvez utiliser d'autres versions quantisées (Q4_K_M, Q5_K_M, etc.) en téléchargeant manuellement le modèle et en mettant à jour la configuration dans `application.yml`.

---

## 🚀 Démarrage

Le système nécessite 3 services qui doivent être démarrés dans l'ordre :

### 1. Démarrer llama.cpp (serveur LLM)

**Windows** :
```powershell
cd llama-cpp
.\llama-server.exe -m models\deepseek-coder-6.7b-instruct.Q2_K.gguf -ngl 0 -c 4096 --port 11435
```

**Linux / macOS** :
```bash
cd llama-cpp
./server -m models/deepseek-coder-6.7b-instruct.Q2_K.gguf -ngl 0 -c 4096 --port 11435
```

**Note** : Si vous utilisez un autre modèle (Q4_K_M, Q5_K_M, etc.), remplacez `Q2_K` par le nom de votre modèle dans la commande ci-dessus.

**Vérification** : Attendre le message "HTTP server listening" dans la console.

**Note** : Gardez cette fenêtre ouverte. Le serveur doit rester actif.

### 2. Démarrer le backend (Spring Boot)

Ouvrir un **nouveau terminal** :

```bash
cd backend
mvn spring-boot:run
```

**Vérification** : Attendre le message "Started AiCodeMentorApplication" et vérifier : http://localhost:8080/api/exercises

**Note** : Gardez cette fenêtre ouverte.

### 3. Démarrer le frontend (Vue.js)

Ouvrir un **nouveau terminal** :

```bash
npm run dev
```

**Vérification** : Le terminal affichera l'URL (généralement http://localhost:5173)

### Accès à l'application

Une fois les 3 services démarrés, accéder à : **http://localhost:5173** (ou l'URL affichée par Vite)

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

# Configuration LLM
llm:
  provider: llamacpp
  llamacpp:
    base-url: http://localhost:11435
    model: deepseek-coder-6.7b-instruct.Q2_K  # Ou Q4_K_M, Q5_K_M, etc.
    timeout: 300  # Augmenter à 300 pour Q4/Q5
```

### Configuration du frontend

Le proxy est configuré dans `vite.config.js` pour rediriger `/api` vers `http://localhost:8080` en développement.

---

## ⚡ Accélération GPU (optionnel)

### NVIDIA GPU (CUDA)

**Démarrage avec GPU** :
```bash
cd llama-cpp
.\llama-server.exe -m models\deepseek-coder-6.7b-instruct.Q2_K.gguf -ngl 35 -c 4096 --port 11435
```

**Note** : Remplacez `Q2_K` par votre modèle si vous utilisez une autre version.

**Paramètres recommandés** :
- `-ngl 35` : 35 couches sur GPU (ajuster selon la mémoire GPU)
- `-c 4096` : Taille du contexte
- `-t 4` : Threads CPU (pour les couches restantes)

**Vérification** :
```bash
nvidia-smi  # Vérifier l'utilisation GPU
```

### Mode CPU (par défaut)

Si vous n'avez pas de GPU ou rencontrez des problèmes, utilisez le mode CPU :
```bash
-ngl 0  # Toutes les couches sur CPU
```

---

## 🎯 Utilisation

### Workflow enseignant

1. **Créer un exercice**
   - Se connecter avec un compte enseignant (teacher@demo.com / demo123)
   - Accéder à "Créer"
   - Décrire l'exercice en langage naturel (ex: "Écrire une fonction qui inverse une chaîne")
   - L'IA génère automatiquement : énoncé, code de départ, tests, solution, exemples

2. **Modifier et personnaliser**
   - Utiliser l'éditeur pour ajuster le code
   - Modifier les tests si nécessaire
   - Ajouter des indices ou des explications

3. **Publier**
   - Activer le statut "Publié"
   - L'exercice devient visible pour les étudiants

### Workflow étudiant

1. **Parcourir les exercices**
   - Se connecter avec un compte étudiant (student@demo.com / demo123)
   - Consulter la liste des exercices publiés
   - Filtrer par thème ou difficulté
   - Sélectionner un exercice

2. **Résoudre l'exercice**
   - Lire l'énoncé et les exemples
   - Écrire le code dans l'éditeur
   - Cliquer sur "Exécuter les tests"

3. **Obtenir de l'aide**
   - Si les tests échouent, consulter les erreurs
   - Cliquer sur "Obtenir un Indice" (généré automatiquement par IA)
   - Itérer jusqu'à résolution

### Comptes de démonstration

- **Enseignant 1** : `teacher@demo.com` / `demo123`
- **Enseignant 2** : `teacher2@demo.com` / `demo123`
- **Étudiant** : `student@demo.com` / `demo123`

---

## 🐛 Dépannage

### Problème : Port déjà utilisé

**Port 8080 (backend)** :
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

**Port 11435 (llama.cpp)** :
```bash
# Windows
netstat -ano | findstr :11435
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:11435 | xargs kill -9
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
4. Vérifier que le modèle est présent dans `llama-cpp/models/`

### Problème : Erreur de compilation Java

**Vérifier Java 25** :
```bash
java -version  # Doit afficher version 25
echo $JAVA_HOME  # Doit pointer vers Java 25 (Linux/Mac)
echo %JAVA_HOME%  # Doit pointer vers Java 25 (Windows)
```

**Si problème** :
1. Réinstaller Java 25
2. Reconfigurer `JAVA_HOME`
3. Redémarrer le terminal

### Problème : Modèle non trouvé

Si le modèle n'est pas téléchargé automatiquement :

1. Télécharger manuellement depuis HuggingFace :
   - URL : `https://huggingface.co/TheBloke/deepseek-coder-6.7B-instruct-GGUF`
   - Fichiers disponibles : Q2_K (~2.5GB), Q4_K_M (~4GB), Q5_K_M (~5GB), Q8_0 (~7GB)
   - Placer dans : `llama-cpp/models/`

2. Mettre à jour la configuration dans `application.yml` :
   ```yaml
   llm:
     llamacpp:
       model: deepseek-coder-6.7b-instruct.Q4_K_M  # Nom du modèle téléchargé
   ```

3. Utiliser le nom du modèle dans la commande de démarrage de llama.cpp

---

## 🏗️ Architecture technique

### Frontend
- **Vue.js 3.4+** - Framework frontend
- **Vite 5.2+** - Build tool et serveur de développement
- **Bootstrap 5.3+** - Framework CSS
- **Monaco Editor** - Éditeur de code

### Backend
- **Spring Boot 3.5+** - Framework backend
- **Spring Data JPA** - Accès aux données
- **H2 Database** - Base de données (fichier)
- **JUnit 5** - Exécution de tests
- **Maven** - Gestion des dépendances

### Intelligence artificielle
- **llama.cpp** - Serveur d'inférence local
- **deepseek-coder-6.7b-instruct** - Modèle de langage spécialisé en code
- **Format** : GGUF (quantisé Q2_K par défaut, ~2.5GB)

### Ports des services

| Service | Port | URL |
|---------|------|-----|
| Frontend | 5173 | http://localhost:5173 |
| Backend API | 8080 | http://localhost:8080 |
| llama.cpp | 11435 | http://localhost:11435 |

---

## 📚 Structure du projet

```
ai-code-mentor-main/
├── backend/                    # Backend Spring Boot
│   ├── src/main/java/
│   │   └── com/aicodementor/
│   │       ├── controller/     # Contrôleurs REST API
│   │       ├── service/        # Logique métier
│   │       ├── entity/         # Entités JPA
│   │       ├── repository/    # Accès aux données
│   │       └── config/         # Configuration
│   └── pom.xml
├── src/                        # Frontend Vue.js
│   ├── views/                  # Pages
│   ├── components/             # Composants réutilisables
│   ├── services/               # Services API
│   └── router/                 # Routage
├── llama-cpp/                  # Binaires et modèles
│   ├── llama-server.exe/server # Serveur llama.cpp
│   └── models/
│       └── deepseek-coder-6.7b-instruct.Q2_K.gguf
└── package.json
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

## 🤝 Contribution

Les contributions sont les bienvenues ! N'hésitez pas à :
- Ouvrir une Issue pour signaler un bug
- Proposer une Pull Request
- Améliorer la documentation

---

## 📄 Licence

Ce projet est open-source.
