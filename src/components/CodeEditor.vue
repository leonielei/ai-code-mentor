<template>
  <div class="code-editor-container">
    <div class="editor-header">
      <h5 class="mb-0">{{ title }}</h5>
      <div class="editor-controls">
        <select v-model="selectedLanguage" class="form-select form-select-sm" @change="onLanguageChange">
          <option value="java">Java</option>
          <option value="python">Python</option>
          <option value="javascript">JavaScript</option>
        </select>
        <select v-model="selectedTheme" class="form-select form-select-sm ms-2" @change="onThemeChange">
          <option value="vs-dark">Dark</option>
          <option value="vs-light">Light</option>
        </select>
      </div>
    </div>
    <div ref="editorContainer" :style="{ height: height }" class="monaco-editor-wrapper"></div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import * as monaco from 'monaco-editor'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  language: {
    type: String,
    default: 'java'
  },
  theme: {
    type: String,
    default: 'vs-dark'
  },
  height: {
    type: String,
    default: '500px'
  },
  title: {
    type: String,
    default: 'Éditeur de Code'
  },
  readonly: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'languageChange', 'themeChange'])

const editorContainer = ref(null)
const editor = ref(null)
const code = ref(props.modelValue)
const selectedLanguage = ref(props.language)
const selectedTheme = ref(props.theme)

onMounted(() => {
  if (editorContainer.value) {
    editor.value = monaco.editor.create(editorContainer.value, {
      value: props.modelValue,
      language: selectedLanguage.value,
      theme: selectedTheme.value,
      automaticLayout: true,
      fontSize: 14,
      minimap: { enabled: true },
      scrollBeyondLastLine: false,
      readOnly: props.readonly,
      wordWrap: 'on',
      lineNumbers: 'on',
      glyphMargin: true,
      folding: true,
      lineDecorationsWidth: 10,
      lineNumbersMinChars: 3
    })

    // Listen to content changes
    editor.value.onDidChangeModelContent(() => {
      const value = editor.value.getValue()
      code.value = value
      emit('update:modelValue', value)
      emit('change', value)
    })
  }
})

onUnmounted(() => {
  if (editor.value) {
    editor.value.dispose()
  }
})

watch(() => props.modelValue, (newValue) => {
  if (editor.value && newValue !== editor.value.getValue()) {
    editor.value.setValue(newValue)
  }
})

watch(() => props.language, (newValue) => {
  selectedLanguage.value = newValue
  if (editor.value) {
    monaco.editor.setModelLanguage(editor.value.getModel(), newValue)
  }
})

watch(() => props.theme, (newValue) => {
  selectedTheme.value = newValue
  if (editor.value) {
    monaco.editor.setTheme(newValue)
  }
})

const onLanguageChange = () => {
  if (editor.value) {
    monaco.editor.setModelLanguage(editor.value.getModel(), selectedLanguage.value)
  }
  emit('languageChange', selectedLanguage.value)
}

const onThemeChange = () => {
  if (editor.value) {
    monaco.editor.setTheme(selectedTheme.value)
  }
  emit('themeChange', selectedTheme.value)
}
</script>

<style lang="scss" scoped>
.code-editor-container {
  border: 2px solid #dee2e6;
  border-radius: 0.75rem;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  background: white;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom: 2px solid rgba(255, 255, 255, 0.2);
}

.editor-header h5 {
  font-weight: 700;
  color: white;
}

.editor-controls {
  display: flex;
  gap: 0.5rem;
}

.form-select-sm {
  max-width: 150px;
  border-radius: 0.5rem;
  border: 2px solid rgba(255, 255, 255, 0.3);
  background-color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
}

.monaco-editor-wrapper {
  width: 100%;
  overflow: hidden;
}
</style>

