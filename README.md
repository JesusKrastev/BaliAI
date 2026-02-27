<div align="center">
  <h1>🏝️ Bali AI</h1>
  <p><strong>Tu preparador personal para exámenes impulsado por Inteligencia Artificial</strong></p>
</div>

## 📖 Sobre el Proyecto

**Bali AI** es una aplicación Android nativa diseñada para revolucionar la preparación de exámenes. Utilizando la potencia de la IA generativa (Google Gemini), Bali ofrece una experiencia de aprendizaje adaptativa y gamificada, adaptándose al ritmo, preferencias y debilidades de cada estudiante.

## ✨ Características Principales

*   **🤖 Integración con IA (Gemini):** Explicaciones detalladas de errores, asistencia personalizada y análisis de rendimiento apoyados por la API de Gemini.
*   **🎮 Gamificación Completa:** 
    *   **Niveles & XP:** Gana experiencia (XP) y sube de nivel con tu esfuerzo constante.
    *   **Monedas & Energía:** Sistema de economía dentro de la app para gestionar las sesiones de estudio.
    *   **Rachas (Streaks):** Mantén tu racha diaria de estudio y usa "congeladores" si necesitas un descanso.
*   **📚 Modos de Práctica:**
    *   Exámenes simulados (Exams).
    *   Pruebas de conocimiento.
    *   Repaso inteligente enfocado en reparar fallos previos (Mistakes).
*   **👤 Perfil Adaptativo (Onboarding):** Flujo detallado para capturar el tipo de licencia a examinar, tiempo de estudio disponible, metas diarias y temas que resultan más difíciles.
*   **🔔 Retención y Notificaciones:** Integración con Firebase Cloud Messaging y OneSignal para enviar recordatorios inteligentes y mantener la motivación alta.

## 🛠️ Tecnologías y Arquitectura

El proyecto está desarrollado utilizando los estándares modernos de desarrollo en Android, aplicando principios de **Arquitectura Limpia (Clean Architecture)** y el patrón **MVVM** (Model-View-ViewModel) para lograr un código altamente escalable y mantenible.

### Stack Tecnológico:

*   **Lenguaje:** [Kotlin](https://kotlinlang.org/)
*   **Interfaz de Usuario (UI):** [Jetpack Compose](https://developer.android.com/jetpack/compose) - UI declarativa nativa.
*   **Inyección de Dependencias:** [Dagger Hilt](https://dagger.dev/hilt/)
*   **Almacenamiento Local:** [Room](https://developer.android.com/training/data-storage/room) (Base de datos) & [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (Preferencias)
*   **Navegación:** Compose Navigation (`androidx.navigation:navigation-compose`)
*   **Backend as a Service (BaaS):** [Firebase](https://firebase.google.com/)
    *   Autenticación (Firebase Auth + Google Credential Manager)
    *   Base de datos en la Nube (Firestore)
    *   Métricas de Uso (Google Analytics)
    *   Reportes de Fallos (Crashlytics)
*   **Inteligencia Artificial:** [Google Generative AI SDK](https://ai.google.dev/docs)
*   **Notificaciones Push:** [OneSignal](https://onesignal.com/)
*   **Multimedia:** [Coil](https://coil-kt.github.io/coil/) (Manejo asíncrono de imágenes SVG/PNG) y [Lottie](https://airbnb.design/lottie/) (Animaciones vectoriales y microinteracciones).

### Estructura del Código

El código fuente (ubicado en `app/src/main/java/com/jesuskrastev/bali/`) se encuentra estrictamente dividido en las siguientes capas:

*   `domain/`: Casos de Uso independientes del framework (ej. `IncrementCoinsUseCase`, `CalculateLevelUseCase`) y Modelos de datos del negocio (ej. `User`, `TestResult`).
*   `data/`: Implementaciones de repositorios, manejo de fuentes de datos locales (BBDD Room) y remotas (API Gemini, Firebase Firestore), y mappers.
*   `ui/`: Toda la capa de presentación utilizando Jetpack Compose, organizada por flujos y pantallas (`screens`), junto a las definiciones de diseño (`theme`).
*   `di/`: Módulos de provisión de Hilt marcando el ciclo de vida de los diferentes repositorios y servicios (`RemoteModule`, `RoomModule`, `AuthModule`, etc.).

## 🚀 Instalación y Configuración

Pasos para compilar y ejecutar el proyecto en tu máquina local:

1. **Clonar el repositorio:**
   ```bash
   git clone <URL_DEL_REPOSITORIO>
   cd BaliAI
   ```

2. **Abrir en Android Studio:**
   Abre el proyecto importando el directorio base. Se recomienda usar las versiones más recientes de Android Studio que tengan soporte nativo para el plugin de Compose compiler.

3. **Configurar las Credenciales (Requisito Indispensable):**
   El sistema de build (Gradle) espera la existencia de un archivo llamado `local.properties` en la raíz del proyecto para leer claves de API importantes que **no deben** subirse a control de versiones. 
   Deberás agregar las siguientes líneas a tu `local.properties`:
   ```properties
   GEMINI_API_KEY="tu_clave_api_de_google_ai_studio"
   ONE_SIGNAL_APP_ID="tu_app_id_de_onesignal"
   ```

4. **Configuración de Firebase:**
   Asegúrate de tener un proyecto creado en la Consola de Firebase. Descarga y añade el archivo `google-services.json` correspondiente dentro del directorio `app/`.

5. **Compilación:**
   Selecciona un emulador (nivel de API 24 o superior) o conecta un dispositivo físico mediante ADB y ejecuta.

## 📄 Licencia

Todos los derechos reservados.