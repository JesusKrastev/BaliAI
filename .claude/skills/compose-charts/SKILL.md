---
name: compose-charts
description: >
  Referencia completa de la librería Compose Charts (by Ehsan Narmani, v0.2.5) para Jetpack Compose en Android.
  Usa esta skill siempre que el usuario mencione ComposeCharts, quiera crear gráficos en Jetpack Compose,
  pregunte por PieChart / LineChart / ColumnChart / RowChart en Compose, pregunte por propiedades como
  BarProperties, DotProperties, IndicatorProperties, GridProperties, AxisProperties, DividerProperties,
  LineProperties, LabelProperties, LabelHelperProperties o PopupProperties, o quiera entender cómo
  animar, personalizar o configurar cualquier chart de esta librería.
---

# Compose Charts — Skill de Referencia Completa

Librería de gráficos para Jetpack Compose. Versión documentada: **0.2.5**.  
Repositorio: [ComposeCharts en GitHub](https://github.com/ehsannarmani/ComposeCharts)

---

## Índice

1. [Tipos de Chart disponibles](#1-tipos-de-chart-disponibles)
2. [PieChart](#2-piechart)
3. [LineChart](#3-linechart)
4. [ColumnChart](#4-columnchart)
5. [RowChart](#5-rowchart)
6. [Animation Modes](#6-animation-modes)
7. [Chart Properties — Referencia completa](#7-chart-properties--referencia-completa)
   - 7.1 BarProperties
   - 7.2 DotProperties
   - 7.3 IndicatorProperties
   - 7.4 GridProperties
   - 7.5 AxisProperties
   - 7.6 DividerProperties
   - 7.7 LineProperties
   - 7.8 LabelProperties
   - 7.9 LabelHelperProperties
   - 7.10 PopupProperties
8. [Patrones de uso frecuentes](#8-patrones-de-uso-frecuentes)
9. [Errores comunes y cómo evitarlos](#9-errores-comunes-y-cómo-evitarlos)

---

## 1. Tipos de Chart disponibles

| Chart | Composable | Datos de entrada | Soporte negativos |
|---|---|---|---|
| Gráfico de sectores | `PieChart` | `List<Pie>` | No |
| Gráfico de líneas | `LineChart` | `List<Line>` | Sí |
| Gráfico de columnas (vertical) | `ColumnChart` | `List<Bars>` | Sí |
| Gráfico de barras (horizontal) | `RowChart` | `List<Bars>` | Sí |

---

## 2. PieChart

### Uso básico

```kotlin
var data by remember {
    mutableStateOf(
        listOf(
            Pie(label = "Android", data = 20.0, color = Color.Red, selectedColor = Color.Green),
            Pie(label = "Windows", data = 45.0, color = Color.Cyan, selectedColor = Color.Blue),
            Pie(label = "Linux",   data = 35.0, color = Color.Gray, selectedColor = Color.Yellow)
        )
    )
}

PieChart(
    modifier = Modifier.size(200.dp),
    data = data,
    onPieClick = { clickedPie ->
        val pieIndex = data.indexOf(clickedPie)
        data = data.mapIndexed { i, pie -> pie.copy(selected = i == pieIndex) }
    },
    selectedScale = 1.2f,
    scaleAnimEnterSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    ),
    colorAnimEnterSpec = tween(300),
    colorAnimExitSpec = tween(300),
    scaleAnimExitSpec = tween(300),
    spaceDegreeAnimExitSpec = tween(300),
    style = Pie.Style.Fill      // o Pie.Style.Stroke(width = 100f)
)
```

### Estilo Stroke (donut)

```kotlin
PieChart(
    // ...
    spaceDegree = 7f,
    selectedPaddingDegree = 4f,
    style = Pie.Style.Stroke(width = 100f)
)
```

### Parámetros clave de PieChart

| Parámetro | Tipo | Descripción |
|---|---|---|
| `data` | `List<Pie>` | Lista de sectores |
| `onPieClick` | `(Pie) -> Unit` | Callback al pulsar un sector |
| `selectedScale` | `Float` | Escala del sector seleccionado (ej. 1.2f) |
| `style` | `Pie.Style` | `Fill` o `Stroke(width)` |
| `spaceDegree` | `Float` | Espacio en grados entre sectores |
| `selectedPaddingDegree` | `Float` | Padding extra en sector seleccionado |
| `scaleAnimEnterSpec` | `AnimationSpec<Float>` | Animación de entrada del escalado |
| `colorAnimEnterSpec` | `AnimationSpec<Color>` | Animación de entrada del color |

---

## 3. LineChart

### Uso básico

```kotlin
LineChart(
    modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp),
    data = remember {
        listOf(
            Line(
                label = "Windows",
                values = listOf(28.0, 41.0, 5.0, 10.0, 35.0),
                color = SolidColor(Color(0xFF23af92)),
                firstGradientFillColor = Color(0xFF2BC0A1).copy(alpha = .5f),
                secondGradientFillColor = Color.Transparent,
                strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
                gradientAnimationDelay = 1000,
                drawStyle = DrawStyle.Stroke(width = 2.dp),
            )
        )
    },
    animationMode = AnimationMode.Together(delayBuilder = { it * 500L }),
)
```

### Valores negativos y zero line

```kotlin
LineChart(
    data = remember {
        listOf(
            Line(
                label = "Temperature",
                values = listOf(28.0, 41.0, -15.0, 27.0, 54.0),
                color = Brush.radialGradient(/* ... */)
            )
        )
    },
    zeroLineProperties = LineProperties(
        enabled = true,
        color = SolidColor(Color.Red),
    ),
    minValue = -20.0,
    maxValue = 100.0
)
```

> **Regla de minValue/maxValue:**  
> - `maxValue` por defecto = valor más alto de los datos.  
> - `minValue` por defecto = `0` si no hay negativos; si los hay = el valor más bajo.

### Múltiples líneas

```kotlin
LineChart(
    data = remember {
        listOf(
            Line(label = "Windows", values = listOf(/*...*/), color = Color.Green,  curvedEdges = true),
            Line(label = "Linux",   values = listOf(/*...*/), color = Color.Orange, curvedEdges = false),
            Line(label = "Android", values = listOf(/*...*/), color = Color.Blue,   curvedEdges = true),
        )
    }
)
```

### Dots

```kotlin
Line(
    label = "Windows",
    values = listOf(/*...*/),
    color = Color.Orange,
    curvedEdges = true,
    dotProperties = DotProperties(
        enabled = true,
        color = SolidColor(Color.White),
        strokeWidth = 4f,
        radius = 7f,
        strokeColor = SolidColor(Color.Orange),
    )
)
```

### Líneas discontinuas (dashed)

```kotlin
Line(
    drawStyle = DrawStyle.Stroke(
        width = 3.dp,
        strokeStyle = StrokeStyle.Dashed(intervals = floatArrayOf(10f, 10f))
    )
)
```

### Fill (área rellena)

```kotlin
Line(
    color = Color.Orange,
    drawStyle = DrawStyle.Fill,
    // ...
)
```

### Parámetros clave de `Line`

| Parámetro | Descripción |
|---|---|
| `label` | Nombre de la serie |
| `values` | `List<Double>` con los datos |
| `color` | `Brush` o `Color` de la línea |
| `firstGradientFillColor` | Color superior del gradiente de relleno |
| `secondGradientFillColor` | Color inferior del gradiente de relleno |
| `drawStyle` | `DrawStyle.Stroke(width, strokeStyle)` o `DrawStyle.Fill` |
| `curvedEdges` | `Boolean` — si la línea tiene curvas suaves |
| `dotProperties` | `DotProperties` — configuración de los puntos |
| `strokeAnimationSpec` | AnimationSpec para la animación de dibujo |
| `gradientAnimationDelay` | Delay en ms antes de animar el gradiente |

---

## 4. ColumnChart

### Uso básico (múltiples datos por barra)

```kotlin
ColumnChart(
    modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp),
    data = remember {
        listOf(
            Bars(
                label = "Jan",
                values = listOf(
                    Bars.Data(label = "Linux",   value = 50.0, color = Brush.verticalGradient(/*...*/)),
                    Bars.Data(label = "Windows", value = 70.0, color = SolidColor(Color.Blue)),
                )
            ),
            Bars(
                label = "Feb",
                values = listOf(
                    Bars.Data(label = "Linux",   value = 80.0, color = Brush.verticalGradient(/*...*/)),
                    Bars.Data(label = "Windows", value = 60.0, color = SolidColor(Color.Blue)),
                )
            )
        )
    },
    barProperties = BarProperties(
        radius = Bars.Data.Radius.Rectangle(topRight = 6.dp, topLeft = 6.dp),
        spacing = 3.dp,
        strokeWidth = 20.dp
    ),
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    ),
)
```

### Un solo dato por barra

```kotlin
ColumnChart(
    data = remember {
        listOf(
            Bars(label = "1", values = listOf(Bars.Data(value = 10.0, color = Color.Blue))),
            Bars(label = "2", values = listOf(Bars.Data(value = 20.0, color = Color.Blue))),
            // ...
        )
    },
    barProperties = BarProperties(spacing = 1.dp, strokeWidth = 10.dp),
)
```

### Valores negativos

```kotlin
ColumnChart(
    data = remember {
        listOf(
            Bars(label = "1", values = listOf(Bars.Data(value = -40.0, color = Color.Blue))),
            Bars(label = "2", values = listOf(Bars.Data(value =  50.0, color = Color.Blue))),
        )
    },
    maxValue = 75.0,
    minValue = -75.0
)
```

---

## 5. RowChart

Idéntico a `ColumnChart` pero las barras son horizontales.

```kotlin
RowChart(
    modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp),
    data = remember {
        listOf(
            Bars(
                label = "Jan",
                values = listOf(
                    Bars.Data(label = "Linux",   value = 50.0, color = Brush.verticalGradient(/*...*/)),
                    Bars.Data(label = "Windows", value = 70.0, color = SolidColor(Color.Blue)),
                )
            ),
            // ...
        )
    },
    barProperties = BarProperties(
        radius = Bars.Data.Radius.Rectangle(topRight = 6.dp, topLeft = 6.dp),
        spacing = 3.dp,
        strokeWidth = 20.dp
    ),
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    ),
)
```

### Negativos en RowChart

```kotlin
RowChart(
    data = remember { listOf(
        Bars(label = "1", values = listOf(Bars.Data(value = -40.0, color = Color.Blue))),
        Bars(label = "2", values = listOf(Bars.Data(value =  50.0, color = Color.Blue))),
    )},
    maxValue = 75.0,
    minValue = -75.0
)
```

---

## 6. Animation Modes

Disponible en `RowChart`, `ColumnChart` y `LineChart`.

| Modo | Descripción |
|---|---|
| `AnimationMode.OneByOne` | Las animaciones se ejecutan una tras otra |
| `AnimationMode.Together(delayBuilder)` | Se ejecutan en paralelo; se puede configurar un delay por índice |
| `AnimationMode.None` | Sin animación |

```kotlin
// OneByOne
LineChart(animationMode = AnimationMode.OneByOne)

// Together con delay de 200ms entre cada uno
LineChart(
    animationMode = AnimationMode.Together(delayBuilder = { index -> index * 200L })
)
```

---

## 7. Chart Properties — Referencia completa

### 7.1 BarProperties

Aplica a: `ColumnChart`, `RowChart`

| Propiedad | Tipo | Default | Descripción |
|---|---|---|---|
| `thickness` | `Dp` | `20` | Ancho de la barra |
| `spacing` | `Dp` | `4` | Espacio entre barras dentro del mismo grupo |
| `cornerRadius` | `Bars.Data.Radius` | `Bars.Data.Radius.None` | Radio de esquinas |
| `style` | `DrawStyle` | `DrawStyle.Fill` | Estilo de la barra |

**Tipos de `Bars.Data.Radius`:**
- `Bars.Data.Radius.None` — sin radio
- `Bars.Data.Radius.Circular(radius: Dp)` — todas las esquinas iguales
- `Bars.Data.Radius.Rectangle(topRight, topLeft, bottomRight, bottomLeft)` — esquinas individuales

```kotlin
val barProperties = BarProperties(
    thickness = 15.dp,
    spacing = 4.dp,
    cornerRadius = Bars.Data.Radius.Circular(6.dp),
    style = DrawStyle.Fill
)
```

---

### 7.2 DotProperties

Aplica a: `LineChart` (por cada `Line`)

| Propiedad | Tipo | Default | Descripción |
|---|---|---|---|
| `enabled` | `Boolean` | `false` | Visibilidad de los puntos |
| `radius` | `Dp` | `3.dp` | Tamaño del punto |
| `color` | `Brush` | `SolidColor(Color.Unspecified)` | Color del punto |
| `strokeWidth` | `Dp` | `2.dp` | Grosor del borde |
| `strokeColor` | `Brush` | `SolidColor(Color.Unspecified)` | Color del borde |
| `strokeStyle` | `StrokeStyle` | `StrokeStyle.Normal` | Estilo del borde |
| `animationEnabled` | `Boolean` | `true` | Si `false`, muestra puntos sin delay |
| `animationSpec` | `AnimationSpec` | `tween(300)` | Animación de aparición |

```kotlin
val dotProperties = DotProperties(
    enabled = true,
    radius = 4.dp,
    color = SolidColor(Color.Red),
    strokeWidth = 3.dp,
    strokeColor = Color.White,
    strokeStyle = StrokeStyle.Normal,
    animationEnabled = true,
    animationSpec = tween(500)
)
```

---

### 7.3 IndicatorProperties

Aplica a: todos los charts (contadores laterales).

| Propiedad | Tipo | Default | Descripción |
|---|---|---|---|
| `enabled` | `Boolean` | `true` | Visibilidad de los indicadores |
| `textStyle` | `TextStyle` | `TextStyle.Default` | Estilo del texto |
| `count` | `IndicatorCount` | `IndicatorCount.CountBased(5)` | Tipo y cantidad de indicadores |
| `position` | `IndicatorPosition` | Depende del chart | Posición (ver tabla) |
| `padding` | `Dp` | `12.dp` | Padding entre indicadores y chart |
| `contentBuilder` | `(Double) -> String` | `"%.2f".format(it)` | Formato del texto |
| `indicators` | `List` | `emptyList()` | Sobreescribe los indicadores calculados |

**Posiciones disponibles:**
- En `LineChart` y `ColumnChart`: `IndicatorPosition.Horizontal.Start` / `.End`
- En `LineChart` (posición vertical): `IndicatorPosition.Vertical.Top` / `.Bottom`

**Tipos de `IndicatorCount`:**
- `IndicatorCount.CountBased(count: Int)` — divide el rango en `count` partes iguales
- `IndicatorCount.StepBased(stepBy: Double)` — avanza de `stepBy` en `stepBy` desde min hasta max  
  Ejemplo: max=20, min=-10, stepBy=5 → indicadores: 20, 15, 10, 5, 0, -5, -10

```kotlin
val indicatorProperties = HorizontalIndicatorProperties(
    enabled = true,
    textStyle = MaterialTheme.typography.labelSmall,
    count = IndicatorCount.CountBased(count = 5),
    position = IndicatorPosition.Horizontal.End,
    padding = 32.dp,
    contentBuilder = { indicator -> "%.2f".format(indicator) + " Million" },
    indicators = listOf(10.0, 50.0, 30.0)
)
```

---

### 7.4 GridProperties

Aplica a: todos los charts.

| Propiedad | Tipo | Default | Descripción |
|---|---|---|---|
| `enabled` | `Boolean` | `true` | Visibilidad de la rejilla |
| `xAxisProperties` | `AxisProperties` | `AxisProperties(..)` | Líneas horizontales |
| `yAxisProperties` | `AxisProperties` | `AxisProperties(..)` | Líneas verticales |

```kotlin
val gridProperties = GridProperties(
    enabled = true,
    xAxisProperties = AxisProperties(/* ... */),
    yAxisProperties = AxisProperties(/* ... */)
)
```

---

### 7.5 AxisProperties

Usada dentro de `GridProperties`.

| Propiedad | Tipo | Default | Descripción |
|---|---|---|---|
| `enabled` | `Boolean` | `true` | Visibilidad de las líneas de eje |
| `style` | `StrokeStyle` | `StrokeStyle.Normal` | Estilo de línea |
| `color` | `Color` | `Color.Gray` | Color de la línea |
| `thickness` | `Dp` | `(0.5).dp` | Grosor |
| `lineCount` | `Int` | `5` | Número de líneas del eje |

```kotlin
val axisProperties = AxisProperties(
    enabled = true,
    style = StrokeStyle.Dashed(intervals = floatArrayOf(10f, 10f)),
    color = Color.Gray,
    thickness = (.5).dp,
    lineCount = 5
)
```

---

### 7.6 DividerProperties

Aplica a: todos los charts (divisores entre labels y chart, e indicadores y chart).

| Propiedad | Tipo | Default | Descripción |
|---|---|---|---|
| `enabled` | `Boolean` | `true` | Visibilidad de los divisores |
| `xAxisProperties` | `LineProperties` | `LineProperties(..)` | Divisor horizontal |
| `yAxisProperties` | `LineProperties` | `LineProperties(..)` | Divisor vertical |

```kotlin
val dividerProperties = DividerProperties(
    enabled = true,
    xAxisProperties = LineProperties(/* ... */),
    yAxisProperties = LineProperties(/* ... */)
)
```

---

### 7.7 LineProperties

Usada en `DividerProperties` y `zeroLineProperties` del `LineChart`.

| Propiedad | Tipo | Default | Descripción |
|---|---|---|---|
| `enabled` | `Boolean` | `true` | Visibilidad |
| `style` | `StrokeStyle` | `StrokeStyle.Normal` | Estilo de la línea |
| `color` | `Color` | `Color.Gray` | Color |
| `thickness` | `Dp` | `(0.5).dp` | Grosor |

```kotlin
val lineProperties = LineProperties(
    enabled = true,
    style = StrokeStyle.Dashed(intervals = floatArrayOf(10f, 10f)),
    color = Color.Gray,
    thickness = (.5).dp,
)
```

---

### 7.8 LabelProperties

Aplica a: todos los charts (etiquetas del eje X: "Jan", "Feb"…).

| Propiedad | Tipo | Default | Descripción |
|---|---|---|---|
| `enabled` | `Boolean` | `true` | Visibilidad de las etiquetas |
| `textStyle` | `TextStyle` | `TextStyle.Default` | Estilo del texto |
| `verticalPadding` | `Dp` | `12.dp` | Padding vertical del área de etiquetas |
| `labels` | `List<String>` | `emptyList()` | Sobreescribe las etiquetas calculadas |
| `builder` | `@Composable (modifier, label, shouldRotate, index) -> Unit` | `null` | Composable personalizado para cada etiqueta |
| `rotation` | `LabelProperties.Rotation` | `Rotation()` | Rotación automática si no caben |

**LabelProperties.Rotation:**

| Propiedad | Tipo | Default | Descripción |
|---|---|---|---|
| `mode` | `Rotation.Mode` | `Mode.IfNecessary` | `IfNecessary` (solo si hay conflicto de espacio) o `Force` (siempre) |
| `degree` | `Float` | `-45f` | Grado de rotación |
| `padding` | `Dp?` | `null` | Padding adicional para etiquetas rotadas |

```kotlin
// Básico con etiquetas personalizadas
val labelProperties = LabelProperties(
    enabled = true,
    textStyle = MaterialTheme.typography.labelSmall,
    verticalPadding = 16.dp,
    labels = listOf("Apr", "Mar", "Feb", "Jan"),
    builder = { modifier, label, shouldRotate, index ->
        Text(modifier = modifier, text = label)
    }
)

// Con rotación forzada
val labelProperties = LabelProperties(
    enabled = true,
    textStyle = MaterialTheme.typography.labelSmall,
    verticalPadding = 16.dp,
    rotation = LabelProperties.Rotation(
        mode = LabelProperties.Rotation.Mode.Force,
        degree = -45f
    )
)
```

---

### 7.9 LabelHelperProperties

Aplica a: todos los charts (leyenda en la parte superior del chart: "● Linux ● Windows").

| Propiedad | Tipo | Default | Descripción |
|---|---|---|---|
| `enabled` | `Boolean` | `true` | Visibilidad de la leyenda |
| `textStyle` | `TextStyle` | `TextStyle.Default` | Estilo del texto de la leyenda |

```kotlin
val labelHelperProperties = LabelHelperProperties(
    enabled = true,
    textStyle = MaterialTheme.typography.labelMedium
)
```

---

### 7.10 PopupProperties

Aplica a: todos los charts (tooltip al hacer click o drag).

| Propiedad | Tipo | Default | Descripción |
|---|---|---|---|
| `enabled` | `Boolean` | `true` | Visibilidad del popup |
| `animationSpec` | `AnimationSpec` | `tween(400)` | Animación del popup |
| `duration` | `Long` | `1500` | Tiempo visible en ms (solo Column/Row) |
| `textStyle` | `TextStyle` | `TextStyle.Default` | Estilo del texto |
| `containerColor` | `Color` | `Color(0xff313131)` | Color de fondo |
| `cornerRadius` | `Dp` | `6.dp` | Radio de esquinas |
| `contentHorizontalPadding` | `Dp` | `4.dp` | Padding horizontal interno |
| `contentVerticalPadding` | `Dp` | `2.dp` | Padding vertical interno |
| `mode` | `PopupProperties.Mode` | `Mode.Normal` | `Normal` o `PointMode` (solo en puntos) |
| `contentBuilder` | `(Int, Int, Double) -> String` | `"%.2f".format(it)` | Formatea el contenido: (dataIndex, valueIndex, value) |

> **Tip:** En `LineChart` puedes asignar un `PopupProperties` distinto a cada `Line`, lo que permite deshabilitar el popup en algunas series y no en otras.

```kotlin
val popupProperties = PopupProperties(
    enabled = true,
    animationSpec = tween(300),
    duration = 2000L,
    textStyle = MaterialTheme.typography.labelSmall,
    containerColor = Color.White,
    cornerRadius = 8.dp,
    contentHorizontalPadding = 4.dp,
    contentVerticalPadding = 2.dp,
    contentBuilder = { dataIndex, valueIndex, value ->
        // dataIndex: ¿qué serie? (cuando hay más de una)
        // valueIndex: ¿qué punto?
        value.format(1) + " Million"
    }
)
```

---

## 8. Patrones de uso frecuentes

### Chart con todas las propiedades configuradas

```kotlin
ColumnChart(
    modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp),
    data = remember { /* List<Bars> */ },
    barProperties = BarProperties(
        thickness = 15.dp,
        spacing = 4.dp,
        cornerRadius = Bars.Data.Radius.Circular(6.dp),
    ),
    indicatorProperties = HorizontalIndicatorProperties(
        count = IndicatorCount.StepBased(stepBy = 10.0),
        position = IndicatorPosition.Horizontal.Start,
        contentBuilder = { "%.0f".format(it) }
    ),
    labelProperties = LabelProperties(
        textStyle = MaterialTheme.typography.labelSmall,
        rotation = LabelProperties.Rotation(mode = LabelProperties.Rotation.Mode.IfNecessary)
    ),
    labelHelperProperties = LabelHelperProperties(enabled = true),
    gridProperties = GridProperties(
        xAxisProperties = AxisProperties(lineCount = 5, style = StrokeStyle.Dashed(floatArrayOf(5f,5f)))
    ),
    dividerProperties = DividerProperties(enabled = true),
    popupProperties = PopupProperties(
        containerColor = Color.White,
        contentBuilder = { _, _, value -> "%.1f k".format(value) }
    ),
    animationMode = AnimationMode.Together(delayBuilder = { it * 100L }),
    maxValue = 100.0,
    minValue = 0.0
)
```

### LineChart con múltiples líneas y dots

```kotlin
LineChart(
    data = remember {
        listOf(
            Line(
                label = "Series A",
                values = listOf(10.0, 30.0, 20.0, 50.0, 40.0),
                color = SolidColor(Color.Blue),
                curvedEdges = true,
                dotProperties = DotProperties(
                    enabled = true,
                    radius = 5.dp,
                    color = SolidColor(Color.White),
                    strokeColor = SolidColor(Color.Blue),
                    strokeWidth = 3.dp
                ),
                drawStyle = DrawStyle.Stroke(width = 2.dp),
                firstGradientFillColor = Color.Blue.copy(alpha = 0.3f),
                secondGradientFillColor = Color.Transparent,
            ),
            Line(
                label = "Series B",
                values = listOf(5.0, 15.0, 35.0, 25.0, 55.0),
                color = SolidColor(Color.Red),
                curvedEdges = false,
                drawStyle = DrawStyle.Stroke(
                    width = 2.dp,
                    strokeStyle = StrokeStyle.Dashed(intervals = floatArrayOf(8f, 8f))
                ),
            )
        )
    },
    animationMode = AnimationMode.OneByOne,
    zeroLineProperties = LineProperties(enabled = false),
)
```

---

## 9. Errores comunes y cómo evitarlos

| Error | Causa | Solución |
|---|---|---|
| Barra no visible | `strokeWidth` demasiado pequeño o `spacing` mayor que el área disponible | Ajustar `BarProperties.thickness` y `spacing` |
| Indicadores fuera de rango | `maxValue`/`minValue` no cubren todos los datos | Asegurarse de que `maxValue ≥ max(values)` y `minValue ≤ min(values)` |
| Etiquetas superpuestas | Labels largas sin rotación | Usar `LabelProperties.Rotation(mode = Mode.IfNecessary)` o `Mode.Force` |
| Dots no aparecen | `DotProperties.enabled = false` (default) | Establecer `enabled = true` explícitamente |
| Popup no desaparece (Line) | `duration` no aplica a LineChart (solo Column/Row) | En LineChart el popup desaparece al soltar el drag |
| Negativos no se muestran bien | `minValue` no se establece | Pasar `minValue` explícitamente con valor negativo |
| Animación brusca | `animationSpec` por defecto | Usar `spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)` |
| Gradiente de línea sin efecto | `drawStyle = DrawStyle.Fill` en lugar de `Stroke` | Usar `DrawStyle.Stroke` para líneas con gradiente de relleno; `DrawStyle.Fill` rellena el área |

---

> **Nota:** Toda la documentación corresponde a la versión **0.2.5** de ComposeCharts by Ehsan Narmani.  
> Para versiones posteriores, consultar el repositorio oficial.
