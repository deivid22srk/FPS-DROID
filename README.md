# FPS DROID 🎮📊

**Monitor de FPS/CPU/GPU em tempo real para Android com Material You**

Um aplicativo Android moderno e bonito que monitora o desempenho do seu dispositivo em tempo real, com overlay flutuante que funciona sobre jogos e aplicativos.

![Material You Design](https://img.shields.io/badge/Material%20You-Design-6750A4?style=for-the-badge)
![Android](https://img.shields.io/badge/Android-8.0+-3DDC84?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=for-the-badge&logo=kotlin)
![Root](https://img.shields.io/badge/Root-Optional-FF6B35?style=for-the-badge)

## ✨ Características

### 📱 Interface Material You
- Design moderno seguindo as diretrizes do Material Design 3
- Suporte completo a Dynamic Color (Material You)
- Animações fluidas e transições suaves
- Interface intuitiva e responsiva

### 📊 Monitoramento em Tempo Real
- **FPS Real**: Mede o FPS real do jogo/app (não os Hz da tela)
- **CPU**: Uso percentual e temperatura
- **GPU**: Uso percentual e frequência
- **RAM**: Uso de memória em tempo real

### 🎯 Overlay Flutuante
- Overlay que sobrepõe todos os apps e jogos
- Posicionamento personalizável
- Design compacto e não intrusivo
- Cores dinâmicas baseadas em desempenho

### 🔐 Segurança com Root
- **Extremo cuidado** no uso de root
- Verificações de segurança antes de cada comando
- Tratamento robusto de erros
- Funciona mesmo sem root (recursos limitados)

## 🚀 Como Usar

### Requisitos
- Android 8.0 (API 26) ou superior
- Permissão de sobreposição (overlay)
- Root (opcional, mas recomendado para recursos completos)

### Instalação

1. **Download do APK**
   - Baixe o APK mais recente na seção [Releases](https://github.com/deivid22srk/FPS-DROID/releases)
   - Ou compile você mesmo (veja seção Build)

2. **Instalação**
   ```bash
   adb install app-release.apk
   ```
   Ou instale manualmente pelo gerenciador de arquivos

3. **Permissões**
   - Ao abrir o app, conceda a permissão de sobreposição
   - Se tiver root, o app detectará automaticamente

4. **Iniciar Monitoramento**
   - Clique em "Start Overlay"
   - O overlay aparecerá na tela
   - Abra qualquer jogo/app e veja o desempenho em tempo real

## 🛠️ Build

### Pré-requisitos
- JDK 17+
- Android SDK
- Gradle 8.4+

### Compilar localmente

```bash
# Clone o repositório
git clone https://github.com/deivid22srk/FPS-DROID.git
cd FPS-DROID

# Compilar Debug APK
./gradlew assembleDebug

# Compilar Release APK
./gradlew assembleRelease

# APKs gerados em:
# app/build/outputs/apk/debug/app-debug.apk
# app/build/outputs/apk/release/app-release.apk
```

### GitHub Actions

O projeto está configurado com GitHub Actions para build automático:
- Build automático em push para `main` e branches `capy/**`
- APKs disponíveis como artifacts após o build
- Configuração em `.github/workflows/build.yml`

## 🎨 Tecnologias

- **Linguagem**: Kotlin 100%
- **UI**: Jetpack Compose
- **Material Design**: Material 3 (Material You)
- **Arquitetura**: MVVM
- **Coroutines**: Para operações assíncronas
- **StateFlow**: Para gerenciamento de estado reativo

## 📋 Estrutura do Projeto

```
FPS-DROID/
├── app/
│   ├── src/main/
│   │   ├── java/com/fpsdroid/monitor/
│   │   │   ├── core/
│   │   │   │   └── PerformanceMonitor.kt    # Lógica de monitoramento
│   │   │   ├── service/
│   │   │   │   └── OverlayService.kt        # Serviço de overlay
│   │   │   ├── ui/
│   │   │   │   ├── theme/                   # Material You Theme
│   │   │   │   └── components/              # Componentes Compose
│   │   │   ├── util/
│   │   │   │   └── RootUtils.kt             # Utilitários de root
│   │   │   ├── MainActivity.kt              # Tela principal
│   │   │   └── FpsDroidApplication.kt       # Application class
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── .github/workflows/
│   └── build.yml                            # GitHub Actions workflow
└── README.md
```

## 🔧 Como Funciona

### Medição de FPS Real
O app utiliza duas abordagens para medir FPS real:
1. **SurfaceFlinger Latency**: Acessa informações de frame timing do compositor do Android
2. **GfxInfo**: Lê contadores de frames renderizados por app

### Monitoramento de CPU
- Lê `/proc/stat` para calcular uso de CPU em tempo real
- Diferença entre duas leituras para percentual preciso

### Temperatura
- Acessa zonas térmicas em `/sys/class/thermal/`
- Suporta múltiplos sensores de temperatura

### GPU
- **Qualcomm (Adreno)**: `/sys/class/kgsl/kgsl-3d0/`
- **Mali**: `/sys/devices/platform/mali.0/`
- Lê frequência e utilização

### RAM
- Lê `/proc/meminfo` para uso de memória
- Calcula uso real (Total - Available)

## ⚠️ Cuidados com Root

O app foi desenvolvido com **extremo cuidado** no uso de root:

1. **Verificação Prévia**: Sempre verifica se root está disponível antes de executar comandos
2. **Try-Catch**: Todos os comandos root são envolvidos em blocos try-catch
3. **Timeout**: Comandos não ficam travados
4. **Logging**: Todos os erros são logados para debug
5. **Fallback**: Tenta ler arquivos sem root primeiro
6. **Cleanup**: Processos são sempre destruídos após uso

## 🎯 Permissões

```xml
<!-- Obrigatória para overlay -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

<!-- Para serviço foreground -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<!-- Para notificações -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Para manter serviço ativo -->
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

**Nota**: Root não é uma permissão, é concedido pelo usuário via SuperSU/Magisk.

## 📸 Screenshots

*(Screenshots serão adicionados após o primeiro build)*

## 🤝 Contribuindo

Contribuições são bem-vindas! Sinta-se livre para:
- Reportar bugs
- Sugerir novas features
- Enviar pull requests
- Melhorar a documentação

## 📄 Licença

Este projeto é open source e está disponível sob a licença MIT.

## 🙏 Agradecimentos

- Material Design Team pelo Material You
- Android Open Source Project
- Comunidade Kotlin e Jetpack Compose

## 📞 Contato

Para questões, sugestões ou suporte:
- Abra uma [Issue](https://github.com/deivid22srk/FPS-DROID/issues)
- Pull Requests são bem-vindos!

---

**Feito com ❤️ e Material You**
