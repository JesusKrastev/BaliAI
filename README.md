<div align="center">
  <h1>🏝️ Bali AI</h1>
  <p><strong>Tu preparador personal para exámenes impulsado por Inteligencia Artificial</strong></p>
</div>

## 📖 Sobre el Proyecto

**Bali AI** es una aplicación Android nativa diseñada para revolucionar la preparación de exámenes. Utilizando la potencia de la IA generativa (Google Gemini), Bali ofrece una experiencia de aprendizaje adaptativa y gamificada, adaptándose al ritmo, preferencias y debilidades de cada estudiante.

## ✨ Características Principales

*   **🤖 Integración con IA (Gemini):** Tutor conversacional, tests y simulacros generados a medida y un camino de aprendizaje adaptado a cada alumno, servidos por Gemini a través de Firebase AI Logic.
*   **🎮 Gamificación Completa:** 
    *   **Niveles & XP:** Gana experiencia (XP) y sube de nivel con tu esfuerzo constante; el camino de rangos reparte premios en monedas.
    *   **Monedas & Tienda:** Gasta monedas en pistas, 50/50, dobles de XP o monedas, cofres sorpresa y apuestas de racha.
    *   **Rachas (Streaks):** Mantén tu racha diaria de estudio, protégela con "congeladores" o recupérala con monedas.
    *   **Minijuegos:** Cinco juegos cortos de reflejos y señales con recompensas reales.
*   **📚 Modos de Práctica:**
    *   Lecciones y repasos del camino de aprendizaje.
    *   Simulacros de examen oficial de 30 preguntas con cronómetro y veredicto de preparación.
    *   Estadísticas por tema y cuenta atrás hasta el examen.
*   **👤 Perfil Adaptativo (Onboarding):** Flujo narrativo que captura la experiencia previa, la fecha del examen y el ritmo de estudio para generar el plan.
*   **🔔 Retención y Notificaciones:** OneSignal y Firebase Cloud Messaging con un canal de Android por tipo de aviso.
*   **💳 Suscripción:** Acceso de pago gestionado con RevenueCat.

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
*   **Inteligencia Artificial:** [Firebase AI Logic](https://firebase.google.com/docs/ai-logic) contra la API de Gemini (sin clave en la app; App Check protege las llamadas)
*   **Pagos:** [RevenueCat](https://www.revenuecat.com/)
*   **Analítica:** PostHog y Firebase Analytics, siempre a través de `AnalyticsTracker`
*   **Notificaciones Push:** [OneSignal](https://onesignal.com/)
*   **Multimedia:** [Coil](https://coil-kt.github.io/coil/) (Manejo asíncrono de imágenes SVG/PNG) y [Lottie](https://airbnb.design/lottie/) (Animaciones vectoriales y microinteracciones).

### Estructura del Código

El código fuente (ubicado en `app/src/main/java/com/jesuskrastev/bali/`) se encuentra estrictamente dividido en las siguientes capas:

*   `domain/`: Casos de Uso independientes del framework (ej. `IncrementCoinsUseCase`, `CalculateReadinessUseCase`) y Modelos de datos del negocio (ej. `User`, `TestResult`).
*   `data/`: Implementaciones de repositorios, manejo de fuentes de datos locales (BBDD Room) y remotas (API Gemini, Firebase Firestore), y mappers.
*   `ui/`: Toda la capa de presentación utilizando Jetpack Compose, organizada por flujos y pantallas (`screens`), junto a las definiciones de diseño (`theme`).
*   `di/`: Módulos de provisión de Hilt marcando el ciclo de vida de los diferentes repositorios y servicios (`RepositoryModule`, `RoomModule`, `FirebaseModule`, `GeminiModule`, etc.).

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
   ONE_SIGNAL_APP_ID=tu_app_id_de_onesignal
   REVENUECAT_API_KEY=tu_clave_publica_de_revenuecat
   POSTHOG_API_KEY=tu_clave_de_posthog
   ```
   Sin ellas la app compila, pero las claves de esos SDK quedan vacías. **No hay clave de Gemini en la app**: las llamadas pasan por Firebase AI Logic. En compilaciones de depuración, registra el token de depuración de App Check (Consola de Firebase → App Check → Apps → Tokens de depuración; se imprime en Logcat la primera vez) o las peticiones de IA serán rechazadas.

4. **Configuración de Firebase:**
   Asegúrate de tener un proyecto creado en la Consola de Firebase. Descarga y añade el archivo `google-services.json` correspondiente dentro del directorio `app/`.

5. **Compilación:**
   Selecciona un emulador (nivel de API 24 o superior) o conecta un dispositivo físico mediante ADB y ejecuta.

## 🧪 Pruebas (Testing)

El proyecto incluye una suite de pruebas automatizadas para garantizar la estabilidad de las funcionalidades principales.

### Ejecución de Tests:

*   **Unit Tests & Screenshot Tests (Robolectric + Roborazzi):**
    Ejecutan en tu máquina local sin necesidad de un emulador.
    ```bash
    ./gradlew test
    ```
    *Nota: Los screenshot tests se ejecutan solo en variantes de depuración (`debug`).*

*   **Instrumentation Tests (Room DAOs, Integración):**
    Requieren un emulador o dispositivo físico conectado.
    ```bash
    ./gradlew connectedAndroidTest
    ```

Tras la ejecución, los resultados se pueden consultar en `app/build/reports/`.

### Reporte de Cobertura (Coverage):
Para generar un reporte detallado de qué parte del código está siendo probada:
```bash
./gradlew testDebugUnitTestCoverage
```
El reporte HTML se generará en `app/build/reports/jacoco/testDebugUnitTestCoverage/html/index.html`.

## 📄 Licencia

Todos los derechos reservados.