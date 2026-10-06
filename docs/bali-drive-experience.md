# Bali Drive: entrega de implementación

Fecha: 2026-10-06. Rama local: `feature/bali-drive-experience`.
Base: `origin/develop`, commit `465b4eada31bfc1a56d0bfff8cc17cc626113f85`, tras `git fetch origin`.
Worktree de desarrollo: `.worktrees/bali-drive-experience`, dentro de BaliAI. La copia original de `develop` conserva sus cambios sin confirmar.

## Comportamiento

- Juegos muestra una vista previa animada de Bali Drive y un botón Jugar. Inicio y las rutas antiguas abren Bali Drive; Salir usa el mismo regreso al origen que Atrás.
- El tutorial tiene dos pasos: deslizar para dirigir y mantener pulsado sin mover el dedo para frenar. Incluye un dedo animado y una zona de práctica sin tráfico ni tiempo de carrera. Completar ambos pasos guarda la preferencia por usuario/dispositivo; abandonar antes deja el tutorial pendiente. Las ayudas de situaciones de las tres primeras partidas permanecen independientes.
- Se reutiliza `game_records`: nuevas claves booleanas locales, con un ámbito separado para invitados. Las claves existentes de récord y partidas permanecen intactas. No se modifica Room, Firestore, dependencias ni claves de SDK.
- La barra refleja la distancia recorrida con la paleta de Bali, iconos vectoriales nuevos y semántica accesible. Conserva puntuación, récord, multiplicador, estrellas y potenciadores. La posición de las ayudas usa la altura medida de la cabecera.
- Al terminar se abre un Dialog Material3 con estrellas de valoración, puntos, récord previo/nuevo, aciertos, estrellas recogidas y disponibles, duración, precisión, XP y su desglose, monedas, nivel y todas las faltas con explicaciones. El contenido puede desplazarse; Otra vez y Salir permanecen debajo. Con fuente ampliada, cada estadística presenta etiqueta y valor en columna.
- Las recompensas muestran estado pendiente, completo o fallido. No se presentan como cero mientras llegan. La salida y repetición esperan el intento de guardado, limitado a 15 segundos; tras fallo o timeout vuelven a estar disponibles. No se reintenta la cadena de incrementos. El récord ya guardado se conserva aunque falle una recompensa posterior.
- El runId protege contra finalizaciones antiguas y duplicadas; el motor se retiene en el ViewModel. Los bucles de partida, portada y práctica respetan el ciclo de vida. Liberación/cancelación limpia el gesto y se mantiene el umbral original de frenado de 120 ms.
- La UI mantiene un StateFlow y usa casos de uso para preparación, tutorial y registro. Las transiciones del tutorial son puras. DataStore corre en IO y las operaciones importantes tienen trazas Firebase Performance; no hay trazas por fotograma.

## Verificaciones

- Compilación Kotlin de app y pruebas: correcta.
- Pruebas específicas de Bali Drive, rutas y DataStore: correctas; incluyen el motor, tutorial, persistencia real desde los bytes guardados, aislamiento entre usuarios, cancelación de MotionEvent, fuentes grandes, transiciones y guardados lentos/fallidos.
- `testDebugUnitTest assembleDebug bundleRelease`: debug 841 pruebas, 0 fallos; APK debug y AAB release generados correctamente. La prueba adicional verifica que un fallo de XP no reduzca la XP de la siguiente partida.
- La ejecución previa de `test` incluyó release: 666 pruebas, 15 fallos en `FirstStepsBarTest` y `PrizeCelebrationTest`.
- Los 15 fallos de release están en `FirstStepsBarTest` (7) y `PrizeCelebrationTest` (8): `Unable to resolve activity ... androidx.activity.ComponentActivity`. Estos archivos y la configuración Gradle no se han modificado; el manifiesto de la actividad de pruebas Compose pertenece a debug. Las nuevas pruebas visuales siguen la convención `*ScreenshotTest` para ejecutarse solo en debug.
- Registro de las 24 capturas de Bali Drive y la captura de Juegos de onboarding: correcto, con semillas fijas para las rutas.
- `verifyRoborazziDebug` filtrado a las pantallas afectadas: correcto, 31 pruebas y 25 capturas verificadas. `assembleDebug` final: correcto.
- `git diff --check`: correcto.
- `adb devices`: sin dispositivos. No se han ejecutado `connectedDebugAndroidTest` ni pruebas manuales en API 24/36, rotación real o gestos en hardware. Quedan pendientes para un dispositivo/emulador conectado.

Comandos de capturas, desde el worktree:

```powershell
.\gradlew.bat recordRoborazziDebug --tests "*BaliDriveScreenshotTest" --tests "*BaliDriveScreenScreenshotTest" --tests "*OnboardingShowcaseScreenshotTest.captureGames"
.\gradlew.bat verifyRoborazziDebug --tests "*BaliDriveScreenshotTest" --tests "*BaliDriveScreenScreenshotTest" --tests "*OnboardingShowcaseScreenshotTest.captureGames"
```

Las referencias visuales están bajo `app/build/`, conforme a las pruebas existentes, y no se versionan. Las capturas de resultados renderizan el contenido canónico del Dialog para evitar la limitación de captura de ventanas modales de Robolectric; las pruebas semánticas comprueban el Dialog real y sus acciones.

## Artefactos locales

- [APK debug](../app/build/outputs/apk/debug/app-debug.apk)
- [Informe completo debug](../app/build/reports/bali-drive/full-debug/index.html)
- [Informe release](../app/build/reports/tests/testReleaseUnitTest/index.html)
- [Juegos](../app/build/bali-drive/catalogue.png)
- [Tutorial: dirigir](../app/build/bali-drive/tutorial_steer.png)
- [Tutorial: frenar](../app/build/bali-drive/tutorial_brake.png)
- [Partida y progreso](../app/build/bali-drive/8_hud.png)
- [Resultados](../app/build/bali-drive/9_results.png)
- [Resultados con fuente ampliada y tema oscuro](../app/build/bali-drive/results_large_fonts.png)

## Archivos modificados o creados

- `app/src/main/java/com/jesuskrastev/bali/data/repository/DataStoreGameRecordRepository.kt`
- `app/src/main/java/com/jesuskrastev/bali/domain/repository/GameRecordRepository.kt`
- `app/src/main/java/com/jesuskrastev/bali/domain/usecase/CompleteGameTutorialUseCase.kt`
- `app/src/main/java/com/jesuskrastev/bali/domain/usecase/PrepareGameUseCase.kt`
- `app/src/main/java/com/jesuskrastev/bali/domain/usecase/SubmitGameRunUseCase.kt`
- `app/src/main/java/com/jesuskrastev/bali/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/jesuskrastev/bali/ui/screens/games/GameType.kt`
- `app/src/main/java/com/jesuskrastev/bali/ui/screens/games/GamesScreen.kt`
- `app/src/main/java/com/jesuskrastev/bali/ui/screens/games/drive/BaliDriveEvent.kt`
- `app/src/main/java/com/jesuskrastev/bali/ui/screens/games/drive/BaliDriveReducer.kt`
- `app/src/main/java/com/jesuskrastev/bali/ui/screens/games/drive/BaliDriveScreen.kt`
- `app/src/main/java/com/jesuskrastev/bali/ui/screens/games/drive/BaliDriveViewModel.kt`
- `app/src/main/java/com/jesuskrastev/bali/ui/screens/games/drive/DriveControls.kt`
- `app/src/main/java/com/jesuskrastev/bali/ui/screens/games/drive/DriveCover.kt`
- `app/src/main/java/com/jesuskrastev/bali/ui/screens/games/drive/DriveHud.kt`
- `app/src/main/java/com/jesuskrastev/bali/ui/screens/games/drive/DriveIcons.kt`
- `app/src/main/java/com/jesuskrastev/bali/ui/screens/games/drive/DriveResults.kt`
- `app/src/main/java/com/jesuskrastev/bali/ui/screens/games/drive/DriveSession.kt`
- `app/src/main/java/com/jesuskrastev/bali/ui/screens/games/drive/DriveTutorial.kt`
- `app/src/main/java/com/jesuskrastev/bali/ui/screens/games/drive/DriveUiState.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/jesuskrastev/bali/data/repository/DataStoreGameRecordRepositoryTest.kt`
- `app/src/test/java/com/jesuskrastev/bali/ui/screens/games/GamesNavigationTest.kt`
- `app/src/test/java/com/jesuskrastev/bali/ui/screens/games/drive/BaliDriveReducerTest.kt`
- `app/src/test/java/com/jesuskrastev/bali/ui/screens/games/drive/BaliDriveScreenScreenshotTest.kt`
- `app/src/test/java/com/jesuskrastev/bali/ui/screens/games/drive/BaliDriveScreenshotTest.kt`
- `app/src/test/java/com/jesuskrastev/bali/ui/screens/games/drive/BaliDriveViewModelTest.kt`
- `app/src/test/java/com/jesuskrastev/bali/ui/screens/games/drive/DriveControlsTest.kt`
- `app/src/test/java/com/jesuskrastev/bali/ui/screens/onboarding/OnboardingShowcaseScreenshotTest.kt`
- `docs/bali-drive-experience.md` (este informe).
