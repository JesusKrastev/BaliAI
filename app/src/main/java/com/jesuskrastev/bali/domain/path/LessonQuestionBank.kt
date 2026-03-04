package com.jesuskrastev.bali.domain.path

/**
 * Static question bank for every lesson node in the DGT learning path.
 * Keys match deterministic node IDs from DgtLearningPathTemplate.
 */
object LessonQuestionBank {

    data class StaticQuestion(
        val text: String,
        val options: List<String>,
        val correctAnswerIndex: Int,
        val explanation: String,
        val imageUrl: String? = null
    )

    private val questionsPerNode: Map<String, List<StaticQuestion>> = mapOf(

        // ══════════════════════════════════════════════════════════════════
        // SECCIÓN 0 — El Conductor
        // ══════════════════════════════════════════════════════════════════

        "node_0_0_lesson" to listOf( // Factores físicos y psíquicos
            StaticQuestion("¿Qué factor psicológico puede afectar más negativamente a la conducción?", listOf("El exceso de confianza", "La experiencia al volante", "El conocimiento de la ruta"), 0, "El exceso de confianza reduce la percepción del riesgo y aumenta las probabilidades de accidente."),
            StaticQuestion("¿Cómo afecta el estrés a la conducción?", listOf("No tiene ningún efecto", "Mejora la atención del conductor", "Aumenta la agresividad y reduce la concentración"), 2, "El estrés provoca reacciones más impulsivas, falta de atención y conducción agresiva."),
            StaticQuestion("¿Cuánto tarda el organismo en alcanzar la máxima capacidad visual al pasar de una zona iluminada a una oscura?", listOf("Unos 2 segundos", "Unos 15-30 segundos", "Más de 5 minutos"), 1, "La adaptación a la oscuridad requiere entre 15 y 30 segundos."),
            StaticQuestion("Las emociones intensas como la ira o la euforia, ¿afectan a la conducción?", listOf("No, solo el alcohol afecta", "Sí, alteran la toma de decisiones y la percepción del riesgo", "Solo afectan a conductores noveles"), 1, "Cualquier emoción intensa distrae y altera la capacidad de juicio al volante."),
            StaticQuestion("¿Qué debe hacer un conductor que se siente indispuesto?", listOf("Conducir más despacio", "Detenerse en un lugar seguro", "Abrir las ventanillas y seguir"), 1, "Lo correcto es detenerse en un lugar seguro hasta encontrarse bien.")
        ),

        "node_0_1_lesson" to listOf( // Alcohol
            StaticQuestion("¿Cuál es la tasa máxima de alcohol en sangre para conductores en general?", listOf("0,5 g/l", "0,8 g/l", "0,3 g/l"), 0, "El límite general es 0,5 g/l en sangre (0,25 mg/l en aire espirado).", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_tp18.svg"),
            StaticQuestion("¿Cuál es la tasa máxima de alcohol para conductores noveles?", listOf("0,5 g/l", "0,3 g/l", "0,1 g/l"), 1, "Los conductores noveles y profesionales tienen un límite de 0,3 g/l en sangre."),
            StaticQuestion("¿Qué efectos produce el alcohol en la conducción?", listOf("Mejora los reflejos", "Aumenta el campo visual", "Disminuye los reflejos y la concentración"), 2, "El alcohol deteriora reflejos, coordinación, concentración y percepción de distancias."),
            StaticQuestion("¿Se puede rechazar una prueba de alcoholemia?", listOf("Sí, sin consecuencias", "No, negarse es delito penal", "Solo si no ha bebido"), 1, "Negarse a realizar la prueba de alcoholemia es un delito tipificado en el Código Penal."),
            StaticQuestion("¿Cuándo alcanza el alcohol su máxima concentración en sangre?", listOf("A los 5 minutos de ingerirlo", "Entre 30 y 90 minutos después", "A las 3 horas"), 1, "El pico de alcoholemia se alcanza entre 30 y 90 minutos tras la ingesta.")
        ),

        "node_0_2_lesson" to listOf( // Drogas y medicamentos
            StaticQuestion("¿Cómo afectan las drogas estimulantes (cocaína, anfetaminas) a la conducción?", listOf("Mejoran la capacidad de reacción", "Provocan falsa seguridad y agresividad", "No afectan si se toman en pequeñas dosis"), 1, "Los estimulantes dan falsa sensación de control y provocan conducción agresiva."),
            StaticQuestion("¿Qué medicamentos pueden afectar a la conducción?", listOf("Solo los psicofármacos", "Solo los que llevan pictograma en el envase", "Ansiolíticos, antihistamínicos, analgésicos y otros"), 2, "Muchos medicamentos comunes pueden causar somnolencia, mareos o reducción de reflejos."),
            StaticQuestion("Si un medicamento lleva un triángulo rojo con un coche en el envase, ¿qué significa?", listOf("Que no se puede conducir bajo ningún concepto", "Que puede afectar a la capacidad de conducción", "Que solo afecta a conductores profesionales"), 1, "El pictograma advierte de que el medicamento puede afectar a la conducción."),
            StaticQuestion("¿Es delito conducir bajo los efectos de drogas?", listOf("No, solo es infracción administrativa", "Sí, puede ser delito contra la seguridad vial", "Solo si se provoca un accidente"), 1, "Conducir bajo la influencia de drogas es delito contra la seguridad vial."),
            StaticQuestion("¿El cannabis afecta a la conducción?", listOf("No, es una droga suave", "Sí, altera la percepción del tiempo y reduce los reflejos", "Solo si se fuma mientras se conduce"), 1, "El cannabis altera la percepción espacio-temporal y reduce la capacidad de reacción.")
        ),

        "node_0_3_lesson" to listOf( // Fatiga y somnolencia
            StaticQuestion("¿Cada cuánto tiempo se recomienda descansar en viajes largos?", listOf("Cada 3-4 horas", "Cada 2 horas o 200 km", "Cada hora"), 1, "Se recomienda descansar al menos 15-20 minutos cada 2 horas o 200 km."),
            StaticQuestion("¿Cuál es el síntoma más peligroso de la fatiga?", listOf("Bostezar frecuentemente", "Los microsueños", "Cambiar de postura"), 1, "Los microsueños son pérdidas de consciencia de 2-3 segundos sin que el conductor lo perciba."),
            StaticQuestion("¿A qué horas del día es más frecuente la somnolencia al volante?", listOf("A mediodía", "De 3 a 5 de la madrugada y de 14 a 16 horas", "Al anochecer"), 1, "Los ritmos circadianos provocan mayor somnolencia entre las 3-5h y las 14-16h."),
            StaticQuestion("¿Qué debe hacer si siente sueño al volante?", listOf("Poner música alta y abrir ventanillas", "Detenerse y dormir una siesta corta de 20 minutos", "Beber un café y seguir conduciendo"), 1, "Lo más eficaz es detenerse y dormir 20 minutos; café + siesta es la mejor combinación."),
            StaticQuestion("Una comida copiosa antes de conducir, ¿afecta a la conducción?", listOf("No, da energía", "Sí, favorece la somnolencia postprandial", "Solo si contiene alcohol"), 1, "La digestión pesada desvía sangre al aparato digestivo, causando somnolencia.")
        ),

        "node_0_4_lesson" to listOf( // Distracciones
            StaticQuestion("¿Cuál es la principal causa de accidentes de tráfico?", listOf("El exceso de velocidad", "Las distracciones", "El mal estado de la vía"), 1, "Las distracciones al volante son la primera causa de siniestralidad vial."),
            StaticQuestion("¿Está permitido manipular el GPS mientras se conduce?", listOf("Sí, siempre que sea manos libres", "No, debe programarse antes de iniciar la marcha", "Sí, si se hace con una sola mano"), 1, "El GPS debe programarse con el vehículo detenido para evitar distracciones."),
            StaticQuestion("¿Cuántos metros recorre a ciegas un conductor que mira el móvil 2 segundos a 120 km/h?", listOf("Unos 33 metros", "Unos 67 metros", "Unos 15 metros"), 1, "A 120 km/h se recorren 33 m/s, por lo que en 2 segundos son unos 67 metros sin mirar."),
            StaticQuestion("¿Usar auriculares mientras se conduce está permitido?", listOf("Sí, si se usa solo uno", "No, está prohibido", "Sí, siempre"), 1, "Está prohibido usar auriculares o cascos conectados a dispositivos de sonido mientras se conduce."),
            StaticQuestion("¿Qué tipo de distracción es hablar con los pasajeros?", listOf("Distracción visual", "Distracción cognitiva y auditiva", "No es una distracción"), 1, "Hablar distrae la mente (cognitiva) y el oído (auditiva) del tráfico.")
        ),

        "node_0_5_lesson" to listOf( // Cinturón, airbag y cabecero
            StaticQuestion("¿Es obligatorio el uso del cinturón de seguridad en vías urbanas?", listOf("No, solo en vías interurbanas", "Sí, en todas las vías", "Solo en autopistas y autovías"), 1, "El cinturón es obligatorio en todas las vías, tanto urbanas como interurbanas.", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_r106.svg"),
            StaticQuestion("¿El airbag sustituye al cinturón de seguridad?", listOf("Sí, si el coche lo lleva no hace falta cinturón", "No, el airbag es un complemento del cinturón", "Solo en trayectos urbanos"), 1, "El airbag está diseñado para funcionar junto al cinturón; sin él puede causar lesiones."),
            StaticQuestion("¿Para qué sirve el reposacabezas?", listOf("Para mayor comodidad", "Para evitar el latigazo cervical en colisiones traseras", "Para apoyar la cabeza y dormir"), 1, "El reposacabezas previene el latigazo cervical en caso de impacto por detrás."),
            StaticQuestion("¿Cómo debe colocarse correctamente el cinturón?", listOf("La banda diagonal sobre el hombro y la horizontal sobre las caderas", "Lo más holgado posible", "La banda diagonal bajo el brazo"), 0, "La banda superior va sobre el hombro y la inferior sobre los huesos de la cadera, ajustado al cuerpo."),
            StaticQuestion("¿Qué personas pueden estar exentas del uso del cinturón?", listOf("Los mayores de 65 años", "Personas con certificado médico de exención", "Los conductores profesionales"), 1, "Solo se exime a personas con certificado médico oficial de exención.")
        ),

        "node_0_6_lesson" to listOf( // SRI
            StaticQuestion("¿Hasta qué altura es obligatorio usar un sistema de retención infantil (SRI)?", listOf("Hasta los 12 años", "Hasta 135 cm de estatura", "Hasta los 10 años"), 1, "Los menores de 135 cm deben ir en un SRI homologado adecuado a su talla y peso."),
            StaticQuestion("¿Pueden los niños viajar en el asiento delantero?", listOf("Nunca", "Sí, con SRI si el vehículo no tiene asientos traseros o están ocupados por otros menores", "Sí, siempre que lleven cinturón"), 1, "Excepcionalmente pueden ir delante con SRI si no hay plazas traseras disponibles."),
            StaticQuestion("¿Qué grupo de SRI es adecuado para un bebé recién nacido?", listOf("Grupo 0 o 0+, a contramarcha", "Grupo 2, mirando hacia delante", "Un cojín elevador"), 0, "Los recién nacidos deben ir en el grupo 0/0+ en posición a contramarcha."),
            StaticQuestion("¿Está permitido llevar a un niño en brazos en el coche?", listOf("Sí, en el asiento trasero", "No, nunca", "Sí, si va con cinturón el adulto"), 1, "Llevar a un niño en brazos está prohibido; en caso de colisión es imposible retenerlo."),
            StaticQuestion("¿Cada cuánto hay que revisar el SRI?", listOf("Cada año en la ITV", "Regularmente y sustituir tras un accidente", "No necesita revisión"), 1, "El SRI debe revisarse periódicamente y sustituirse tras cualquier accidente, aunque parezca en buen estado.")
        ),

        // ══════════════════════════════════════════════════════════════════
        // SECCIÓN 1 — El Vehículo
        // ══════════════════════════════════════════════════════════════════

        "node_1_0_lesson" to listOf( // Documentación obligatoria
            StaticQuestion("¿Qué documentos debe llevar siempre el conductor?", listOf("DNI y seguro", "Permiso de conducción, seguro en vigor y documentación del vehículo", "Solo el permiso de conducción"), 1, "Se debe llevar permiso de conducir, permiso de circulación, tarjeta ITV y recibo del seguro."),
            StaticQuestion("¿Es obligatorio llevar el permiso de conducción en formato físico?", listOf("Sí, siempre", "No, la DGT puede comprobar su validez electrónicamente", "Solo en viajes internacionales"), 1, "La DGT dispone de una base de datos electrónica; no obstante, es recomendable llevarlo."),
            StaticQuestion("¿Qué ocurre si circula sin seguro obligatorio?", listOf("Una multa leve", "Sanción económica grave y posible inmovilización", "No pasa nada si no hay accidente"), 1, "Circular sin seguro es infracción muy grave con multas de 601 a 3.005 euros."),
            StaticQuestion("¿Quién debe llevar el permiso de circulación del vehículo?", listOf("Solo el propietario", "El conductor que circule con el vehículo", "Solo en viajes interurbanos"), 1, "Quien conduce el vehículo debe poder acreditar la documentación del mismo."),
            StaticQuestion("¿La tarjeta ITV es obligatoria?", listOf("No, es solo informativa", "Sí, acredita que el vehículo ha pasado la inspección", "Solo para vehículos de más de 10 años"), 1, "La tarjeta ITV es obligatoria y certifica que el vehículo está en condiciones aptas.")
        ),

        "node_1_1_lesson" to listOf( // Partes externas
            StaticQuestion("¿Qué función tienen los retrovisores?", listOf("Solo estética", "Permitir al conductor ver el tráfico trasero y lateral", "Reducir la resistencia al aire"), 1, "Los retrovisores permiten vigilar el tráfico que viene por detrás y por los laterales."),
            StaticQuestion("¿Cuándo deben funcionar los limpiaparabrisas?", listOf("Solo con lluvia intensa", "Siempre que la visibilidad a través del parabrisas se reduzca", "Solo de noche"), 1, "Deben usarse siempre que se reduzca la visibilidad: lluvia, nieve, polvo, etc."),
            StaticQuestion("¿Qué es el ángulo muerto?", listOf("La zona visible por el retrovisor interior", "La zona no visible por los retrovisores", "El ángulo del volante"), 1, "El ángulo muerto es la zona alrededor del vehículo que no se ve por los retrovisores."),
            StaticQuestion("¿Para qué sirven los parachoques?", listOf("Para decorar el vehículo", "Para absorber impactos en colisiones a baja velocidad", "Para proteger la matrícula"), 1, "Los parachoques absorben energía en impactos leves, protegiendo a los ocupantes."),
            StaticQuestion("¿Es obligatorio llevar matrícula trasera iluminada?", listOf("Solo en autopistas", "Sí, es obligatoria para que sea visible de noche", "No, es opcional"), 1, "La matrícula trasera debe ser visible, y su iluminación es obligatoria.")
        ),

        "node_1_2_lesson" to listOf( // Motor, transmisión
            StaticQuestion("¿Qué es la cilindrada de un motor?", listOf("La potencia del motor", "El volumen total de los cilindros", "El consumo de combustible"), 1, "La cilindrada es el volumen que desplazan los pistones dentro de los cilindros."),
            StaticQuestion("¿Qué tipo de tracción tiene un vehículo 4x4?", listOf("Tracción delantera", "Tracción a las cuatro ruedas", "Tracción trasera"), 1, "Un 4x4 transmite la fuerza del motor a las cuatro ruedas simultáneamente."),
            StaticQuestion("¿Qué hace el embrague?", listOf("Frena el vehículo", "Conecta y desconecta el motor de la transmisión", "Acelera el motor"), 1, "El embrague permite acoplar/desacoplar el motor de la caja de cambios para cambiar de marcha."),
            StaticQuestion("¿Qué diferencia hay entre un motor diésel y uno gasolina?", listOf("El diésel funciona por compresión y el gasolina por chispa", "No hay diferencia", "El gasolina es más eficiente siempre"), 0, "El diésel comprime el aire hasta que enciende el combustible; el gasolina usa bujías."),
            StaticQuestion("¿Qué es el par motor?", listOf("La velocidad máxima", "La fuerza de giro que genera el motor", "El número de cilindros"), 1, "El par motor es la fuerza de rotación que el motor entrega al eje de transmisión.")
        ),

        "node_1_3_lesson" to listOf( // Neumáticos
            StaticQuestion("¿Cuál es la profundidad mínima legal del dibujo de un neumático?", listOf("3 mm", "1,6 mm", "2,5 mm"), 1, "La profundidad mínima legal del dibujo es 1,6 mm para turismos."),
            StaticQuestion("¿Cómo afecta una presión incorrecta de los neumáticos?", listOf("No tiene efectos", "Aumenta el consumo y la distancia de frenado", "Solo afecta a la comodidad"), 1, "Una presión incorrecta aumenta el desgaste, el consumo y reduce la adherencia."),
            StaticQuestion("¿Cuándo se debe comprobar la presión de los neumáticos?", listOf("En caliente, tras un viaje largo", "En frío, antes de iniciar la marcha", "Solo cuando se note que están bajos"), 1, "La presión se mide con los neumáticos fríos para obtener una lectura fiable."),
            StaticQuestion("¿Qué indican los testigos de desgaste (TWI) del neumático?", listOf("Que el neumático es nuevo", "Que se ha alcanzado el límite mínimo de profundidad", "La fecha de fabricación"), 1, "Los TWI son indicadores que avisan cuando el dibujo llega al mínimo legal de 1,6 mm."),
            StaticQuestion("¿Es legal circular con neumáticos de diferente medida en el mismo eje?", listOf("Sí, siempre", "No, deben ser iguales en el mismo eje", "Solo en el eje trasero"), 1, "Los neumáticos de un mismo eje deben tener las mismas características.")
        ),

        "node_1_4_lesson" to listOf( // Iluminación
            StaticQuestion("¿Cuándo es obligatorio encender las luces de cruce?", listOf("Solo de noche", "De noche, en túneles y cuando la visibilidad sea insuficiente", "Solo cuando llueve"), 1, "Las luces de cruce son obligatorias de noche, en túneles y con visibilidad reducida."),
            StaticQuestion("¿Cuándo se pueden usar las luces de largo alcance (carretera)?", listOf("Siempre que se quiera ver mejor", "De noche fuera de poblado, si no deslumbran a otros", "En autopistas siempre"), 1, "Las luces largas se usan fuera de poblado de noche, cambiando a cruce si viene un vehículo de frente."),
            StaticQuestion("¿Para qué sirven las luces antiniebla traseras?", listOf("Para ver mejor la carretera", "Para ser vistos con niebla densa, lluvia intensa o nube de polvo", "Para adelantar"), 1, "Las antiniebla traseras solo se usan con visibilidad muy reducida para ser vistos."),
            StaticQuestion("¿Es obligatorio llevar luces diurnas (DRL)?", listOf("Sí, todos los coches fabricados desde 2011", "No, son opcionales", "Solo en autopistas"), 0, "Desde 2011 es obligatorio que los turismos nuevos lleven luces diurnas de serie."),
            StaticQuestion("¿Se pueden sustituir las bombillas de los faros por unas de mayor potencia?", listOf("Sí, siempre", "No, deben ser las homologadas para el vehículo", "Sí, si son LED"), 1, "Solo se pueden usar bombillas homologadas; modificar la iluminación puede ser sancionado.")
        ),

        "node_1_5_lesson" to listOf( // Frenos
            StaticQuestion("¿Qué hace el sistema ABS?", listOf("Reduce la potencia del motor", "Impide el bloqueo de las ruedas al frenar", "Aumenta la fuerza de frenado"), 1, "El ABS evita que las ruedas se bloqueen al frenar, manteniendo la capacidad de dirección."),
            StaticQuestion("¿Qué es la distancia de frenado?", listOf("La distancia desde que se ve el peligro hasta que se para", "La distancia desde que se pisa el freno hasta que el vehículo se detiene", "La distancia de seguridad"), 1, "La distancia de frenado es el recorrido desde que se acciona el freno hasta la parada total."),
            StaticQuestion("¿Si se duplica la velocidad, la distancia de frenado se…?", listOf("Duplica", "Cuadruplica", "Triplica"), 1, "La distancia de frenado crece con el cuadrado de la velocidad: doble velocidad = 4x distancia."),
            StaticQuestion("¿Qué es el EBD?", listOf("Un tipo de combustible", "Sistema de distribución electrónica de frenada", "Un sistema de navegación"), 1, "El EBD reparte la fuerza de frenado entre ejes según la carga y adherencia de cada rueda."),
            StaticQuestion("¿Cómo se debe frenar en una curva?", listOf("Frenando fuerte dentro de la curva", "Reduciendo la velocidad antes de entrar en la curva", "Acelerando y frenando a la vez"), 1, "Se debe frenar antes de la curva y mantener velocidad uniforme dentro de ella.")
        ),

        "node_1_6_lesson" to listOf( // ADAS
            StaticQuestion("¿Qué es el ESP o control de estabilidad?", listOf("Un sistema de navegación", "Un sistema que evita que el vehículo derrape", "Un tipo de seguro"), 1, "El ESP detecta la pérdida de adherencia y frena ruedas individuales para estabilizar el coche."),
            StaticQuestion("¿Desde cuándo es obligatorio el ESP en turismos nuevos en la UE?", listOf("Desde 2010", "Desde 2014", "Desde 2020"), 1, "El ESP es obligatorio en todos los turismos nuevos vendidos en la UE desde noviembre de 2014."),
            StaticQuestion("¿Qué hace el asistente de mantenimiento de carril?", listOf("Aparca el coche automáticamente", "Alerta o corrige si el vehículo se sale del carril", "Controla la velocidad"), 1, "Detecta las marcas viales y avisa o corrige si el vehículo se desvía involuntariamente."),
            StaticQuestion("¿Qué es el frenado automático de emergencia (AEB)?", listOf("El freno de mano electrónico", "Un sistema que frena solo si detecta un obstáculo y el conductor no reacciona", "El ABS mejorado"), 1, "El AEB detecta posibles colisiones y aplica los frenos si el conductor no actúa a tiempo."),
            StaticQuestion("¿Los ADAS sustituyen la responsabilidad del conductor?", listOf("Sí, el coche conduce solo", "No, son ayudas y el conductor sigue siendo responsable", "Solo en autopistas"), 1, "Los ADAS son sistemas de asistencia, no de conducción autónoma; el conductor es siempre responsable.")
        ),

        "node_1_7_lesson" to listOf( // Mantenimiento e ITV
            StaticQuestion("¿Cuándo debe pasar la primera ITV un turismo nuevo?", listOf("A los 2 años", "A los 4 años", "Al año"), 1, "Los turismos nuevos pasan la primera ITV a los 4 años de su primera matriculación."),
            StaticQuestion("¿Cada cuánto se pasa la ITV un turismo de entre 4 y 10 años?", listOf("Cada año", "Cada 2 años", "Cada 6 meses"), 1, "Entre los 4 y 10 años, la ITV se pasa cada 2 años."),
            StaticQuestion("¿Y después de los 10 años?", listOf("Cada 2 años", "Cada año", "Ya no es necesaria"), 1, "A partir de 10 años de antigüedad, la ITV es anual."),
            StaticQuestion("¿Qué ocurre si no se pasa la ITV en plazo?", listOf("Nada, existen plazos de gracia ilimitados", "Infracción grave y posible inmovilización del vehículo", "Solo una multa leve"), 1, "Circular sin ITV en vigor es infracción grave y el vehículo puede ser inmovilizado."),
            StaticQuestion("¿Es recomendable revisar los niveles de líquidos del vehículo periódicamente?", listOf("Solo antes de la ITV", "Sí, regularmente: aceite, refrigerante, frenos y dirección", "Solo si se enciende un testigo"), 1, "Un mantenimiento preventivo incluye revisar regularmente los niveles de los líquidos esenciales.")
        ),

        "node_1_8_lesson" to listOf( // Carga y remolques
            StaticQuestion("¿Cuánto puede sobresalir la carga por detrás en un turismo?", listOf("No puede sobresalir", "Un 10% de la longitud del vehículo", "Hasta 1 metro"), 1, "En turismos la carga puede sobresalir por detrás un 10% de la longitud total del vehículo."),
            StaticQuestion("¿Qué permiso se necesita para llevar un remolque de más de 750 kg de MMA?", listOf("El B es suficiente siempre", "El B+E o el B96 según el caso", "Solo el C"), 1, "Para remolques de más de 750 kg se necesita el B+E o la autorización B96, según la MMA del conjunto."),
            StaticQuestion("¿Qué es la MMA de un vehículo?", listOf("La marca del fabricante", "La Masa Máxima Autorizada", "El peso en vacío"), 1, "La MMA es el peso máximo total permitido para circular, incluyendo carga y ocupantes."),
            StaticQuestion("¿La carga debe ir señalizada si sobresale?", listOf("No, nunca", "Sí, con un panel reflectante V-20", "Solo de noche"), 1, "Si la carga sobresale, debe señalizarse con un panel V-20 reflectante."),
            StaticQuestion("¿Qué ocurre si la carga no está bien sujeta?", listOf("Solo es peligroso en autopistas", "Puede desprenderse, provocar accidentes y es sancionable", "No pasa nada si se va despacio"), 1, "La carga mal sujeta puede caer a la vía, causar accidentes y supone infracción grave.")
        ),

        // ══════════════════════════════════════════════════════════════════
        // SECCIÓN 2 — Señales I: Peligro y Prohibición
        // ══════════════════════════════════════════════════════════════════

        "node_2_0_lesson" to listOf( // Prelación entre señales
            StaticQuestion("¿Qué señal prevalece sobre las demás?", listOf("La señal vertical", "La del agente de tráfico", "El semáforo"), 1, "La señal del agente prevalece sobre semáforos, señales verticales y marcas viales."),
            StaticQuestion("¿Cuál es el orden de prelación de señales?", listOf("Agente > semáforo > señal vertical > marca vial", "Semáforo > agente > marca vial > señal vertical", "Señal vertical > semáforo > agente > marca vial"), 0, "Agente > Señalización circunstancial > Semáforo > Señal vertical > Marca vial."),
            StaticQuestion("Si un semáforo está en rojo pero un agente indica que circule, ¿qué hace?", listOf("Se obedece el semáforo", "Se obedece al agente", "Se espera a que se pongan de acuerdo"), 1, "El agente de tráfico siempre prevalece sobre cualquier otra señalización."),
            StaticQuestion("¿Las marcas viales pueden contradecir una señal vertical?", listOf("Sí, siempre", "No, la señal vertical prevalece", "Solo en autopistas"), 1, "Si hay contradicción, la señal vertical prevalece sobre la marca vial."),
            StaticQuestion("¿Las señales de obras prevalecen sobre las señales permanentes?", listOf("No, las permanentes mandan siempre", "Sí, la señalización circunstancial de obras prevalece", "Solo de día"), 1, "La señalización circunstancial (obras, accidentes) prevalece sobre la permanente.")
        ),

        "node_2_1_lesson" to listOf( // Señales de peligro
            StaticQuestion("¿Qué indica esta señal?", listOf("Presencia de semáforos próximos", "Intersección con prioridad sobre vías que confluyen", "Zona de obras en la calzada"), 1, "La señal P-1 advierte de una intersección donde tenemos prioridad de paso.", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_p1.svg"),
            StaticQuestion("¿Qué forma tienen las señales de peligro?", listOf("Circular con borde rojo", "Triangular con borde rojo y vértice hacia arriba", "Cuadrada con fondo azul"), 1, "Las señales de peligro son triángulos con borde rojo y fondo blanco."),
            StaticQuestion("¿Qué indica esta señal triangular con un semáforo?", listOf("Que hay un semáforo próximo", "Que el semáforo está averiado", "Que hay un paso de peatones"), 0, "La señal P-2 advierte de la proximidad de un semáforo.", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_p2.svg"),
            StaticQuestion("¿A qué distancia suelen colocarse las señales de peligro?", listOf("Justo en el punto de peligro", "Entre 150 y 250 metros antes", "A 500 metros"), 1, "En vías interurbanas se colocan entre 150 y 250 metros antes del peligro."),
            StaticQuestion("¿Qué advierte esta señal con peatones?", listOf("Zona escolar", "Proximidad de un paso de peatones", "Calle peatonal"), 1, "La señal P-4 advierte de la proximidad de un paso de peatones.", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_p4.svg")
        ),

        "node_2_2_lesson" to listOf( // Señales de prohibición de entrada
            StaticQuestion("¿Qué indica esta señal circular roja con franja blanca?", listOf("Sentido único", "Prohibido el paso a toda clase de vehículos", "Dirección obligatoria"), 1, "La señal R-100 prohíbe el paso a todo tipo de vehículos en ambos sentidos.", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_r100.svg"),
            StaticQuestion("¿Qué indica la señal circular roja con rectángulo blanco horizontal?", listOf("Prohibido estacionar", "Dirección prohibida (sentido contrario)", "Vía cortada"), 1, "La señal R-200 prohíbe la entrada porque indica dirección prohibida.", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_r200.svg"),
            StaticQuestion("¿Qué indica la señal R-101 (círculo rojo con coche)?", listOf("Prohibido el paso a vehículos de motor", "Aparcamiento de coches", "Vía preferente para coches"), 0, "La R-101 prohíbe el paso a vehículos de motor y ciclomotores.", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_r101.svg"),
            StaticQuestion("¿Una señal de prohibición de entrada afecta a peatones?", listOf("Sí, siempre", "No, solo afecta a vehículos salvo indicación expresa", "Solo en autopistas"), 1, "Las señales de prohibición de entrada afectan a vehículos; los peatones tienen sus propias restricciones."),
            StaticQuestion("¿Las bicicletas pueden circular por donde hay señal R-100?", listOf("Sí, siempre", "No, R-100 prohíbe el paso a todos los vehículos", "Sí, si hay arcén"), 1, "R-100 prohíbe el paso a todos los vehículos, incluidas las bicicletas.")
        ),

        "node_2_3_lesson" to listOf( // Señales de prohibición: velocidad
            StaticQuestion("¿Qué indica esta señal circular con el número 80?", listOf("Velocidad mínima obligatoria 80 km/h", "Velocidad máxima permitida 80 km/h", "Velocidad recomendada 80 km/h"), 1, "Las señales R-301 con número indican la velocidad máxima permitida en ese tramo.", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_r301-80.svg"),
            StaticQuestion("¿Qué velocidad máxima indica esta señal?", listOf("50 km/h", "60 km/h", "30 km/h"), 0, "La señal R-301-50 establece un límite de velocidad máxima de 50 km/h.", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_r301-50.svg"),
            StaticQuestion("¿Hasta dónde es válida una señal de velocidad máxima?", listOf("Hasta la siguiente intersección solamente", "Hasta que otra señal la modifique, anule o termine el tramo", "Solo 200 metros"), 1, "La limitación es válida hasta que otra señal la cambie o aparezca una de fin de limitación."),
            StaticQuestion("¿Cuál es la velocidad máxima genérica en vías urbanas?", listOf("30 km/h", "50 km/h", "60 km/h"), 1, "El límite genérico en vías urbanas es 50 km/h en calzadas de más de un carril por sentido, 30 km/h en calzadas de un único carril y 20 km/h en vías de plataforma única."),
            StaticQuestion("¿Qué límite indica esta señal?", listOf("110 km/h", "100 km/h", "120 km/h"), 0, "La señal R-301-110 establece un límite de velocidad máxima de 110 km/h.", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_r301-110.svg")
        ),

        "node_2_4_lesson" to listOf( // Restricción: peso, altura
            StaticQuestion("¿Qué indica una señal circular con un número y la palabra 't'?", listOf("Velocidad mínima", "Peso máximo autorizado para ese tramo", "Distancia al próximo pueblo"), 1, "Las señales R-107 indican el peso máximo total permitido para circular por ese tramo."),
            StaticQuestion("¿Qué indica una señal con dos flechas verticales y un número?", listOf("Anchura máxima", "Altura máxima permitida", "Longitud máxima"), 1, "Las señales de restricción por altura prohíben pasar a vehículos que superen esa medida."),
            StaticQuestion("¿A quién afecta principalmente una señal de restricción de peso?", listOf("A todos los vehículos", "A camiones, autobuses y vehículos pesados", "Solo a motos"), 1, "Estas señales afectan a cualquier vehículo que supere el peso indicado, pero son más relevantes para pesados."),
            StaticQuestion("¿Es sancionable pasar por un puente con más peso del indicado?", listOf("No, es solo una recomendación", "Sí, es infracción y pone en peligro la estructura", "Solo si hay un agente"), 1, "Superar el peso máximo señalizado es infracción y puede dañar la estructura."),
            StaticQuestion("¿Dónde suelen ubicarse señales de restricción de altura?", listOf("En curvas peligrosas", "En pasos inferiores, túneles y puentes bajos", "En rotondas"), 1, "Se colocan antes de túneles, puentes y pasos donde la altura libre es limitada.")
        ),

        "node_2_5_lesson" to listOf( // Fin de prohibición
            StaticQuestion("¿Qué indica la señal R-308b?", listOf("Prohibición de adelantamiento", "Fin de la prohibición de adelantamiento", "Fin de limitación de velocidad"), 1, "La señal R-308b es la de fin de prohibición de adelantamiento.", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_r308b.svg"),
            StaticQuestion("¿Cómo se reconoce una señal de fin de prohibición?", listOf("Fondo blanco con líneas oblicuas grises/negras", "Fondo azul con flecha", "Triángulo invertido"), 0, "Las señales de fin de prohibición son circulares con fondo blanco y barras diagonales grises."),
            StaticQuestion("¿La señal de fin de limitación de velocidad obliga a acelerar?", listOf("Sí, hay que ir más rápido", "No, simplemente termina la restricción anterior", "Solo en autopistas"), 1, "Indica que cesa la limitación; a partir de ahí rige el límite genérico de la vía."),
            StaticQuestion("¿Una intersección anula la limitación de velocidad?", listOf("Sí, siempre", "No, la limitación se mantiene hasta que se indique lo contrario", "Solo en rotondas"), 1, "La limitación se mantiene hasta que una señal la modifique o aparezca fin de restricción."),
            StaticQuestion("¿Qué significa una señal circular blanca con barras grises y sin ningún número?", listOf("Fin de todas las prohibiciones", "Inicio de zona urbana", "Prohibido circular"), 0, "Indica el fin de todas las prohibiciones anteriores de ese tramo.")
        ),

        "node_2_6_lesson" to listOf( // Señales de obligación
            StaticQuestion("¿Qué forma y color tienen las señales de obligación?", listOf("Triangulares rojas", "Circulares azules con pictograma blanco", "Cuadradas verdes"), 1, "Las señales de obligación son circulares con fondo azul y pictograma blanco."),
            StaticQuestion("¿Qué indica una señal azul con una flecha hacia la derecha?", listOf("Carril bici a la derecha", "Sentido obligatorio a la derecha", "Giro prohibido a la derecha"), 1, "Las flechas blancas sobre fondo azul indican dirección obligatoria."),
            StaticQuestion("¿La señal de cadenas obligatorias (R-412) obliga a llevar cadenas en…?", listOf("Solo ruedas traseras", "Al menos las ruedas motrices", "Todas las ruedas siempre"), 1, "Las cadenas se colocan en las ruedas motrices para tener tracción en nieve o hielo."),
            StaticQuestion("¿Qué indica la señal azul con un peatón?", listOf("Paso de peatones", "Camino obligatorio para peatones", "Prohibido el paso a peatones"), 1, "La señal de obligación azul con peatón indica senda obligatoria para peatones."),
            StaticQuestion("¿Se puede desobedecer una señal de obligación?", listOf("Sí, si no hay tráfico", "No, las señales de obligación son vinculantes", "Solo de noche"), 1, "Las señales de obligación deben cumplirse siempre que sean visibles.")
        ),

        // ══════════════════════════════════════════════════════════════════
        // SECCIÓN 3 — Señales II: Indicación y Marcas
        // ══════════════════════════════════════════════════════════════════

        "node_3_0_lesson" to listOf( // Señales de indicación general
            StaticQuestion("¿Qué color de fondo tienen las señales de indicación?", listOf("Rojo", "Azul o verde", "Amarillo"), 1, "Las señales de indicación usan fondo azul (servicios, urbanas) o verde (autopistas)."),
            StaticQuestion("¿Qué indica la señal cuadrada azul con la letra P?", listOf("Parada de autobús", "Aparcamiento o zona de estacionamiento", "Prohibido aparcar"), 1, "La señal S-28 indica zona de estacionamiento permitido.", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_s28.svg"),
            StaticQuestion("¿Qué diferencia hay entre señales informativas y de servicio?", listOf("No hay diferencia", "Las informativas orientan y las de servicio indican gasolineras, hospitales, etc.", "Las de servicio solo están en autopistas"), 1, "Las informativas orientan sobre destinos; las de servicio indican instalaciones próximas."),
            StaticQuestion("¿Las señales de indicación son obligatorias?", listOf("Sí, siempre", "No, son informativas y orientativas", "Solo en carreteras nacionales"), 1, "Las señales de indicación informan y orientan; no imponen obligaciones."),
            StaticQuestion("¿Qué indica una señal rectangular azul con una cama?", listOf("Hotel o alojamiento cercano", "Hospital próximo", "Área de descanso"), 0, "El pictograma de una cama indica la presencia de alojamiento cercano.")
        ),

        "node_3_1_lesson" to listOf( // Autopistas y autovías
            StaticQuestion("¿Qué indica esta señal?", listOf("Comienzo de una autopista", "Fin de autopista", "Vía convencional"), 0, "La señal S-1 indica el inicio de una autopista.", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_s1.svg"),
            StaticQuestion("¿Qué indica esta señal?", listOf("Inicio de autovía", "Fin de autopista", "Área de descanso"), 1, "La señal S-2 indica el fin de la autopista.", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_s2.svg"),
            StaticQuestion("¿De qué color es el fondo de las señales en autopistas?", listOf("Azul", "Verde", "Blanco"), 0, "Las señales de orientación en autopistas tienen fondo azul; las de autovías, verde."),
            StaticQuestion("¿Pueden circular ciclistas por autopistas?", listOf("Sí, con luces", "No, está prohibido", "Solo mayores de 14 años"), 1, "En autopistas está prohibida la circulación de ciclistas, peatones, ciclomotores y vehículos lentos."),
            StaticQuestion("¿Cuál es la velocidad mínima en autopistas y autovías?", listOf("40 km/h", "60 km/h", "80 km/h"), 1, "La velocidad mínima genérica en autopistas y autovías es 60 km/h.")
        ),

        "node_3_2_lesson" to listOf( // Carriles y servicios
            StaticQuestion("¿Qué indica una señal azul con una bomba de gasolina?", listOf("Taller mecánico", "Gasolinera o estación de servicio cercana", "Prohibido repostar"), 1, "El pictograma de una bomba indica estación de servicio próxima."),
            StaticQuestion("¿Qué indica la señal azul con una 'H'?", listOf("Helipuerto", "Hospital próximo", "Hotel"), 1, "La letra H en fondo azul indica la proximidad de un hospital."),
            StaticQuestion("¿Qué son los carriles VAO?", listOf("Carriles para vehículos averiados", "Carriles de alta ocupación reservados para coches con más de un ocupante", "Carriles reservados para taxis"), 1, "Los carriles VAO (Vehículo de Alta Ocupación) favorecen el uso compartido del coche."),
            StaticQuestion("¿Las señales de servicio obligan a detenerse?", listOf("Sí, siempre", "No, solo informan de la presencia de servicios", "Solo en autopistas"), 1, "Son señales informativas que indican ubicación de servicios sin obligar."),
            StaticQuestion("¿Qué indica un panel con el número de salida en autopista?", listOf("La velocidad máxima", "El número de la salida o enlace", "Kilómetros restantes"), 1, "Los paneles numerados identifican las salidas para facilitar la orientación.")
        ),

        "node_3_3_lesson" to listOf( // Marcas viales longitudinales
            StaticQuestion("¿Qué indica una línea continua en el centro de la calzada?", listOf("Se puede cruzar para adelantar", "Prohíbe cruzarla o circular sobre ella", "Es solo orientativa"), 1, "La línea continua prohíbe rebasarla o circular sobre ella."),
            StaticQuestion("¿Se puede cruzar una línea discontinua central?", listOf("Nunca", "Sí, para adelantar si hay visibilidad suficiente", "Solo en vías urbanas"), 1, "La línea discontinua permite cruzarla para adelantar si las condiciones son seguras."),
            StaticQuestion("¿Qué indica una línea continua + discontinua juntas?", listOf("Prohibido circular", "Solo puede cruzar quien tiene la discontinua a su lado", "Las dos se pueden cruzar"), 1, "La doble línea continua/discontinua permite cruzar solo a quien tiene la discontinua a su lado."),
            StaticQuestion("¿Las líneas de borde de calzada son de qué color?", listOf("Rojas", "Blancas", "Amarillas"), 1, "Las marcas viales de borde son líneas blancas continuas que delimitan la calzada."),
            StaticQuestion("¿Se puede circular sobre la línea de borde de calzada?", listOf("Sí, siempre", "No, excepto para salir de la calzada o en emergencia", "Solo de noche"), 1, "Solo se puede pisar para incorporarse, salir de la calzada o en situaciones excepcionales.")
        ),

        "node_3_4_lesson" to listOf( // Marcas transversales
            StaticQuestion("¿Qué indica la señal STOP pintada en el suelo?", listOf("Reducir velocidad", "Detenerse obligatoriamente", "Ceda el paso"), 1, "La marca STOP en el suelo obliga a detener completamente el vehículo.", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_r2.svg"),
            StaticQuestion("¿Qué indica un triángulo invertido en la calzada?", listOf("Peligro por obras", "Ceda el paso", "Zona escolar"), 1, "El triángulo invertido pintado indica ceda el paso.", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_r1.svg"),
            StaticQuestion("¿Qué son las líneas transversales de paso de peatones (cebra)?", listOf("Marcas decorativas", "Indican dónde deben cruzar los peatones con prioridad", "Solo son orientativas"), 1, "Las bandas del paso de cebra dan prioridad a los peatones que cruzan por ellas."),
            StaticQuestion("¿Qué indica una línea de detención (línea de STOP)?", listOf("Donde empieza un carril bus", "El punto exacto donde se debe detener el vehículo", "Una plaza de parking"), 1, "Es la línea continua transversal donde debe pararse el vehículo ante un STOP o semáforo en rojo."),
            StaticQuestion("¿Las marcas del paso de ciclistas tienen alguna particularidad?", listOf("Son iguales que las de peatones", "Suelen ser cuadrados o líneas discontinuas de otro color", "No existen"), 1, "Los pasos de ciclistas usan líneas discontinuas y pueden tener el símbolo de la bicicleta.", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_p7.svg")
        ),

        "node_3_5_lesson" to listOf( // Otras marcas: flechas y badenes
            StaticQuestion("¿Qué indica una flecha pintada en el suelo apuntando hacia delante?", listOf("Prohibición de girar", "Dirección del carril: solo seguir recto", "Carril para aparcar"), 1, "Las flechas de selección indican el movimiento obligatorio o permitido en ese carril."),
            StaticQuestion("¿Qué es un resalto o badén en la calzada?", listOf("Un bache", "Una elevación de la calzada para reducir la velocidad", "Un paso de peatones especial"), 1, "Los resaltos son elevaciones artificiales para obligar a reducir la velocidad."),
            StaticQuestion("¿Para qué sirven los chevrones (marcas en V) pintados en la calzada?", listOf("Indicar dirección prohibida", "Reforzar la prohibición de invadir esa zona", "Indicar zona de estacionamiento"), 1, "Los chevrones o cebreados señalizan zonas que no deben invadirse."),
            StaticQuestion("¿Qué indica una doble flecha pintada en el suelo?", listOf("Vía de doble sentido", "Carril que permite dos direcciones", "Prohibición de adelantar"), 1, "Las flechas dobles pueden indicar posibilidad de seguir recto o girar en un carril."),
            StaticQuestion("¿Las inscripciones en la calzada (BUS, TAXI) son obligatorias?", listOf("Solo para residentes", "Sí, indican reserva exclusiva de ese carril", "Son solo orientativas"), 1, "Las inscripciones BUS o TAXI reservan el carril exclusivamente para esos vehículos.")
        ),

        "node_3_6_lesson" to listOf( // Semáforos
            StaticQuestion("¿Qué indica un semáforo en ámbar fijo?", listOf("Puede pasar rápido", "Detenerse si puede hacerlo con seguridad; si no, pasar con precaución", "Carril libre"), 1, "El ámbar fijo obliga a detenerse salvo que no sea posible hacerlo con seguridad."),
            StaticQuestion("¿Qué indica un semáforo en ámbar intermitente?", listOf("Prohibido el paso", "Precaución, la vía no está regulada como con semáforo normal", "Vía libre sin precauciones"), 1, "El ámbar intermitente obliga a extremar la precaución; equivale a una intersección no regulada."),
            StaticQuestion("¿Qué indica una flecha verde en un semáforo?", listOf("Verde solo para peatones", "Permite avanzar únicamente en la dirección de la flecha", "Se puede girar a cualquier lado"), 1, "La flecha verde permite circular solo en el sentido que indica la flecha."),
            StaticQuestion("¿Qué indica un semáforo con un aspa roja y una flecha verde debajo?", listOf("Carril cortado y desvío", "El carril del aspa está cerrado; circular por el que indica la flecha", "Prohibido girar"), 1, "Estos semáforos de carril indican cuáles están abiertos (flecha verde) o cerrados (aspa roja)."),
            StaticQuestion("¿Qué indica un semáforo con dos luces rojas intermitentes alternas?", listOf("Semáforo estropeado", "Detención obligatoria; suele haber en pasos a nivel o acceso de bomberos", "Vía libre con precaución"), 1, "Las dos luces rojas alternas obligan a detenerse completamente (pasos a nivel, bomberos).")
        ),

        "node_3_7_lesson" to listOf( // Señales de los agentes
            StaticQuestion("¿Qué indica un agente con el brazo levantado verticalmente?", listOf("Que puede pasar", "Obligación de detenerse para todos los usuarios", "Que hay peligro adelante"), 1, "El brazo levantado del agente obliga a detenerse; equivale a semáforo en rojo."),
            StaticQuestion("¿Si el agente está de frente o de espaldas, ¿puede pasar?", listOf("Sí, siempre", "No, equivale a semáforo en rojo", "Solo los peatones"), 1, "Frente y espalda del agente significan detención; es equivalente a rojo."),
            StaticQuestion("¿Si el agente está de perfil (brazos en cruz), ¿qué se puede hacer?", listOf("Detenerse", "Pasar los que vienen paralelos a los brazos", "Solo los coches, no las motos"), 1, "Los usuarios que circulan paralelos a los brazos extendidos pueden pasar (equivale a verde)."),
            StaticQuestion("¿Un agente puede regular el tráfico sin uniforme?", listOf("Sí, siempre", "Debe ser identificable como autoridad", "No, nunca"), 1, "El agente debe ser identificable; normalmente lleva uniforme, chaleco reflectante y elementos distintivos."),
            StaticQuestion("¿Las indicaciones del agente siempre prevalecen?", listOf("No, el semáforo manda más", "Sí, prevalecen sobre cualquier otra señalización", "Solo en cruces"), 1, "Las instrucciones del agente tienen máxima prelación sobre todo lo demás.")
        ),

        // ══════════════════════════════════════════════════════════════════
        // SECCIÓN 4 — Normas I: Velocidad y Carriles
        // ══════════════════════════════════════════════════════════════════

        "node_4_0_lesson" to listOf( // Velocidades máximas por tipo de vía
            StaticQuestion("¿Cuál es la velocidad máxima para turismos en autopista?", listOf("130 km/h", "120 km/h", "110 km/h"), 1, "La velocidad máxima genérica para turismos en autopistas y autovías es 120 km/h."),
            StaticQuestion("¿Y en carretera convencional con un carril por sentido?", listOf("90 km/h", "100 km/h", "80 km/h"), 0, "En carreteras convencionales con un carril por sentido el límite es 90 km/h."),
            StaticQuestion("¿Y en vía urbana con un carril por sentido?", listOf("50 km/h", "30 km/h", "40 km/h"), 1, "En vías urbanas con un carril por sentido el límite general es 30 km/h."),
            StaticQuestion("¿Y en vías urbanas con dos o más carriles por sentido?", listOf("60 km/h", "50 km/h", "40 km/h"), 1, "En vías urbanas con dos o más carriles el límite genérico es 50 km/h."),
            StaticQuestion("¿Cuál es la velocidad máxima en vías de plataforma única (compartidas con peatones)?", listOf("30 km/h", "20 km/h", "10 km/h"), 1, "En vías residenciales de plataforma única el límite es 20 km/h.")
        ),

        "node_4_1_lesson" to listOf( // Velocidades específicas
            StaticQuestion("¿Los conductores noveles tienen un límite de velocidad diferente?", listOf("No, es igual para todos", "Sí, 80 km/h en carreteras convencionales", "Sí, 100 km/h en autopistas"), 0, "Actualmente los conductores noveles tienen los mismos límites genéricos que el resto."),
            StaticQuestion("¿Hay que reducir la velocidad cuando llueve?", listOf("No, si el coche tiene buenos neumáticos", "Sí, por la pérdida de adherencia y el riesgo de aquaplaning", "Solo con lluvia intensa"), 1, "La lluvia reduce la adherencia y puede causar aquaplaning; se debe reducir la velocidad."),
            StaticQuestion("¿Qué es el aquaplaning?", listOf("Un tipo de freno", "La pérdida de contacto del neumático con el asfalto por agua", "Un sistema de ayuda a la conducción"), 1, "El aquaplaning ocurre cuando el neumático flota sobre una lámina de agua y pierde adherencia."),
            StaticQuestion("¿Con niebla densa, qué velocidad es recomendable?", listOf("La máxima permitida", "Reducir a la que permita detenerse dentro de la distancia de visibilidad", "No hay recomendación especial"), 1, "Con niebla se debe poder frenar en la distancia que se alcanza a ver."),
            StaticQuestion("¿En zonas escolares suele haber limitación especial?", listOf("No, es igual que cualquier vía urbana", "Sí, normalmente 30 km/h o menos", "Solo a la entrada del colegio"), 1, "Las zonas escolares suelen tener limitación especial de 30 km/h o inferior.")
        ),

        "node_4_2_lesson" to listOf( // Velocidades mínimas
            StaticQuestion("¿Existe una velocidad mínima obligatoria?", listOf("No, se puede ir todo lo lento que se quiera", "Sí, no se debe circular por debajo de la mitad de la velocidad máxima sin causa justificada", "Solo en autopistas"), 1, "Está prohibido circular en condiciones normales por debajo de la mitad del límite máximo de la vía."),
            StaticQuestion("¿Cuál es la velocidad mínima en autopista?", listOf("80 km/h", "60 km/h", "40 km/h"), 1, "La velocidad mínima en autopistas y autovías es de 60 km/h."),
            StaticQuestion("¿Puede un vehículo lento circular por autopista?", listOf("Sí, por el arcén", "No, los vehículos que no alcancen 60 km/h no pueden circular por autopista", "Sí, por el carril derecho"), 1, "Si un vehículo no puede alcanzar los 60 km/h, no puede circular por autopistas ni autovías."),
            StaticQuestion("¿Circular demasiado lento es peligroso?", listOf("No, es más seguro", "Sí, puede provocar retenciones y accidentes por alcance", "Solo si no lleva luces"), 1, "Ir muy lento sin causa justificada obstruye la circulación y aumenta el riesgo de alcances."),
            StaticQuestion("¿Qué debe hacer un vehículo lento en carretera?", listOf("Ir por el centro del carril", "Circular lo más a la derecha posible y señalizar", "Poner las luces de emergencia siempre"), 1, "Los vehículos lentos deben circularceñidos a la derecha y facilitar el adelantamiento.")
        ),

        "node_4_3_lesson" to listOf( // Distancia de seguridad
            StaticQuestion("¿Qué es la distancia de seguridad?", listOf("La separación entre el coche y la acera", "La separación mínima con el vehículo de delante para poder frenar a tiempo", "La distancia al semáforo"), 1, "Es el espacio que permite detenerse con seguridad si el de delante frena bruscamente."),
            StaticQuestion("¿Cómo se calcula aproximadamente la distancia de seguridad?", listOf("1 metro por km/h", "La regla de los 2 segundos o más", "10 metros siempre"), 1, "La regla de los 2 segundos: elegir un punto fijo y si se tarda menos de 2s en llegar, estamos demasiado cerca."),
            StaticQuestion("¿Con lluvia o asfalto mojado, ¿la distancia de seguridad debe ser…?", listOf("La misma que en seco", "Mayor, porque la distancia de frenado aumenta", "Menor, porque el agua frena"), 1, "En mojado la distancia de frenado aumenta, por lo que la distancia de seguridad debe ser mayor."),
            StaticQuestion("¿Cuánto aumenta la distancia de frenado en mojado?", listOf("Un 10%", "Puede duplicarse o más", "No cambia"), 1, "En mojado la distancia de frenado puede multiplicarse por 2 o más."),
            StaticQuestion("¿Es sancionable no mantener la distancia de seguridad?", listOf("No, es solo una recomendación", "Sí, es infracción grave", "Solo en autopistas"), 1, "No guardar la distancia de seguridad es infracción grave con pérdida de puntos.")
        ),

        "node_4_4_lesson" to listOf( // Carriles en vías de varios sentidos
            StaticQuestion("¿Por qué carril se debe circular normalmente?", listOf("Por el izquierdo", "Por el más a la derecha que esté libre", "Por el central"), 1, "La norma general es circular por el carril más a la derecha posible."),
            StaticQuestion("¿Se puede usar el carril izquierdo para circular normalmente?", listOf("Sí, siempre", "No, solo para adelantar o girar a la izquierda", "Solo en autopistas"), 1, "El carril izquierdo se usa para adelantar, girar o descongestionar; no para circular habitualmente."),
            StaticQuestion("¿Qué ocurre si circula por el carril izquierdo sin adelantar?", listOf("Nada, es normal", "Es infracción y puede ser sancionado", "Solo es infracción en autopistas"), 1, "Circular por el carril izquierdo sin adelantar es infracción por no circular por el carril adecuado."),
            StaticQuestion("En una vía de tres carriles por sentido, ¿cuál se usa habitualmente?", listOf("El central", "El derecho", "Cualquiera"), 1, "Se debe usar el carril derecho y dejar el central y el izquierdo para adelantar o girar."),
            StaticQuestion("¿Puede un camión circular por el carril izquierdo en una autopista de tres carriles?", listOf("Sí, siempre", "No, los vehículos de más de 3.500 kg suelen tener prohibido el carril izquierdo", "Solo para adelantar"), 1, "En vías de tres carriles, los vehículos pesados no pueden usar el carril izquierdo.")
        ),

        "node_4_5_lesson" to listOf( // Carriles en vías de un solo sentido
            StaticQuestion("En una vía de sentido único, ¿por qué carril se circula?", listOf("Solo por el derecho", "Se puede circular por cualquier carril", "Solo por el izquierdo"), 1, "En vías de sentido único se puede circular por cualquier carril; sin embargo, es recomendable el derecho."),
            StaticQuestion("¿Qué diferencia un carril bus de un carril normal?", listOf("El color del asfalto", "Está reservado para autobuses y, a veces, taxis y motos", "Es más ancho"), 1, "El carril bus está reservado para transporte público y en algunos casos para taxis, motos y bicis."),
            StaticQuestion("¿Es infracción circular por un carril bus?", listOf("No, cualquiera puede usarlo", "Sí, está reservado y tiene multa", "Solo si hay autobuses"), 1, "Circular por el carril bus sin autorización es infracción que conlleva multa."),
            StaticQuestion("¿Qué indica una señal azul con una bicicleta?", listOf("Prohibido bicicletas", "Carril bici o vía ciclista obligatoria", "Precaución, zona de ciclistas"), 1, "La señal azul con bicicleta indica carril o vía obligatoria para ciclistas."),
            StaticQuestion("¿Se puede aparcar en un carril bici?", listOf("Sí, brevemente", "No, nunca", "Solo para carga y descarga"), 1, "Está prohibido estacionar y parar en un carril bici.")
        ),

        "node_4_6_lesson" to listOf( // Cambio de carril y señalización
            StaticQuestion("¿Qué debe hacer antes de cambiar de carril?", listOf("Acelerar y cambiar rápido", "Mirar retrovisores, señalizar y comprobar el ángulo muerto", "Tocar el claxon"), 1, "Retrovisor + intermitente + comprobación del ángulo muerto antes de cambiar."),
            StaticQuestion("¿Con cuánta antelación se debe señalizar el cambio de carril?", listOf("No es necesario señalizar", "Con la antelación suficiente para que otros conductores lo perciban", "Justo en el momento de cambiar"), 1, "Se debe señalizar con tiempo suficiente para que los demás prevean la maniobra."),
            StaticQuestion("¿Se puede cambiar de carril en una intersección?", listOf("Sí, siempre", "Debe evitarse salvo que las marcas viales lo permitan", "Solo con semáforo en verde"), 1, "En intersecciones debe mantenerse el carril salvo que la señalización indique lo contrario."),
            StaticQuestion("¿Quién tiene prioridad al cambiar de carril simultáneamente?", listOf("El que va más rápido", "El que circula por el carril que hay que cruzar (el que no cambia)", "El de la izquierda"), 1, "El vehículo que está en su carril tiene prioridad sobre el que quiere incorporarse."),
            StaticQuestion("¿Se puede cruzar una línea continua para cambiar de carril?", listOf("Sí, con intermitente", "No, la línea continua lo prohíbe", "Solo en emergencias"), 1, "La línea continua prohíbe cruzarla para cambiar de carril.")
        ),

        "node_4_7_lesson" to listOf( // Incorporación a la circulación
            StaticQuestion("¿Quién tiene prioridad al incorporarse a una vía?", listOf("El que se incorpora", "Los vehículos que ya circulan por la vía", "El de la derecha"), 1, "Los vehículos que ya circulan por la vía tienen prioridad sobre los que se incorporan."),
            StaticQuestion("¿Cómo se debe usar un carril de aceleración?", listOf("Para detenerse y esperar un hueco", "Para ganar velocidad y incorporarse al flujo de tráfico", "Para adelantar"), 1, "El carril de aceleración sirve para alcanzar la velocidad del tráfico y fusionarse."),
            StaticQuestion("¿Y un carril de deceleración?", listOf("Para incorporarse a la autopista", "Para reducir la velocidad al salir de la vía principal", "Para aparcar"), 1, "El carril de deceleración permite reducir velocidad sin entorpecer al tráfico que sigue."),
            StaticQuestion("¿Al incorporarse, qué señal se debe utilizar?", listOf("Las luces de emergencia", "El intermitente del lado hacia el que se incorpora", "El claxon"), 1, "Se debe señalizar con el intermitente correspondiente para advertir de la incorporación."),
            StaticQuestion("¿Se puede incorporarse a una rotonda sin ceder el paso?", listOf("Sí, los de dentro ceden", "No, los que se incorporan deben ceder el paso a los que circulan dentro", "Depende del número de carriles"), 1, "Al entrar en una rotonda se debe ceder el paso a los vehículos que ya circulan en ella.")
        ),

        // ══════════════════════════════════════════════════════════════════
        // SECCIÓN 5 — Normas II: Prioridad y Maniobras
        // ══════════════════════════════════════════════════════════════════

        "node_5_0_lesson" to listOf( // Prioridad en cruces sin señalizar
            StaticQuestion("En un cruce sin señalizar, ¿quién tiene prioridad?", listOf("El que viene por la izquierda", "El que viene por la derecha", "El que circula más rápido"), 1, "En intersecciones sin señalizar se aplica la regla de prioridad a la derecha."),
            StaticQuestion("¿Qué vehículos tienen siempre prioridad de paso?", listOf("Los taxis", "Los vehículos prioritarios en servicio de emergencia", "Los autobuses"), 1, "Ambulancias, bomberos y policía en servicio de emergencia siempre tienen prioridad."),
            StaticQuestion("Si llega a un cruce y hay un vehículo por su derecha, ¿qué hace?", listOf("Acelerar para pasar primero", "Ceder el paso al vehículo de la derecha", "Tocar el claxon"), 1, "Se debe ceder el paso al vehículo que se aproxima por la derecha."),
            StaticQuestion("¿Un vehículo que circula por una vía asfaltada tiene prioridad sobre uno que viene de un camino?", listOf("No, se aplica la derecha", "Sí, la vía asfaltada prevalece", "Depende del tamaño del vehículo"), 1, "Quien circula por vía asfaltada tiene prioridad sobre quien sale de un camino sin pavimentar."),
            StaticQuestion("¿Un vehículo sobre raíles (tranvía) tiene prioridad?", listOf("No, igual que el resto", "Sí, el tranvía tiene prioridad", "Solo en su carril"), 1, "Los vehículos sobre raíles (tranvías) tienen prioridad de paso.")
        ),

        "node_5_1_lesson" to listOf( // Prioridad en cruces señalizados
            StaticQuestion("¿Qué indica la señal de STOP?", listOf("Reducir velocidad", "Detención obligatoria y ceder el paso", "Prohibido el paso"), 1, "El STOP obliga a detenerse completamente y ceder el paso a los vehículos de la vía preferente.", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_r2.svg"),
            StaticQuestion("¿Qué indica la señal triangular invertida de ceda el paso?", listOf("Detenerse siempre", "Ceder el paso sin detenerse obligatoriamente, salvo que sea necesario", "Prioridad de paso"), 1, "El ceda el paso obliga a dejar pasar, pero no a detenerse si la vía está libre.", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_r1.svg"),
            StaticQuestion("¿Qué diferencia hay entre STOP y ceda el paso?", listOf("Ninguna", "En el STOP hay que detenerse siempre; en ceda el paso, solo si vienen vehículos", "El ceda es más importante"), 0, "El STOP exige parada total; el ceda permite continuar si no hay tráfico que impida el paso."),
            StaticQuestion("¿La señal de prioridad en intersección da prioridad solo una vez?", listOf("Sí, solo en ese cruce", "No, hasta que se indique lo contrario", "Solo si es una rotonda"), 0, "La señal de prioridad puntual aplica solo en la intersección inmediata."),
            StaticQuestion("¿Quién tiene prioridad en un cruce regulado por semáforo?", listOf("El de la derecha", "El que tiene la luz verde", "El más grande"), 1, "En cruces con semáforo, tiene prioridad quien dispone de la fase verde.")
        ),

        "node_5_2_lesson" to listOf( // Glorietas y rotondas
            StaticQuestion("¿Quién tiene prioridad en una glorieta?", listOf("El que se incorpora", "El que ya circula dentro de la glorieta", "El de la derecha"), 1, "Los vehículos que circulan dentro de la glorieta tienen prioridad sobre los que entran."),
            StaticQuestion("¿Por qué carril se sale de una rotonda?", listOf("Siempre por el izquierdo", "Por el carril exterior (derecho)", "Por cualquiera"), 1, "Se debe salir por el carril exterior derecho, señalizando con el intermitente derecho."),
            StaticQuestion("¿Es obligatorio señalizar la salida de la rotonda?", listOf("No, es evidente", "Sí, con el intermitente derecho", "Solo si hay peatones"), 1, "Se debe poner el intermitente derecho antes de abandonar la rotonda."),
            StaticQuestion("¿En qué sentido se circula dentro de una rotonda?", listOf("En sentido contrario a las agujas del reloj", "En sentido de las agujas del reloj", "Indistintamente"), 0, "En España se circula en sentido contrario a las agujas del reloj dentro de las rotondas."),
            StaticQuestion("¿Se puede cambiar de carril dentro de la rotonda?", listOf("No, nunca", "Sí, señalizando y con precaución", "Solo para salir"), 1, "Se puede cambiar de carril dentro de la rotonda señalizando y sin poner en peligro a otros.")
        ),

        "node_5_3_lesson" to listOf( // Adelantamiento: cuándo y cómo
            StaticQuestion("¿Por qué lado se debe adelantar?", listOf("Siempre por la derecha", "Siempre por la izquierda, salvo excepciones", "Por cualquier lado"), 1, "En España el adelantamiento se realiza por la izquierda, salvo excepciones concretas."),
            StaticQuestion("¿Se puede adelantar en una curva sin visibilidad?", listOf("Sí, si se va rápido", "No, está prohibido", "Solo en autopistas"), 1, "Adelantar sin visibilidad suficiente está prohibido y es causa de accidentes graves."),
            StaticQuestion("¿A qué distancia lateral mínima se debe adelantar a un ciclista?", listOf("0,5 metros", "1,5 metros", "1 metro"), 1, "Se debe dejar al menos 1,5 metros de separación lateral al adelantar a ciclistas."),
            StaticQuestion("¿Qué indica esta señal?", listOf("Prohibido adelantar en todo el tramo señalizado", "Solo prohibido adelantar camiones", "Adelantamiento permitido con precaución"), 0, "La R-308 prohíbe el adelantamiento de vehículos de motor en el tramo señalizado.", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_r308a.svg"),
            StaticQuestion("¿Debe el vehículo adelantado facilitar la maniobra?", listOf("No, puede mantener su velocidad", "Sí, debe facilitar el adelantamiento, sin acelerar", "Solo si va despacio"), 1, "El vehículo adelantado debe facilitar la maniobra y no debe acelerar.")
        ),

        "node_5_4_lesson" to listOf( // Prohibiciones de adelantamiento
            StaticQuestion("¿Se puede adelantar en un paso de peatones?", listOf("Sí, con precaución", "No, está prohibido adelantar en un paso de peatones", "Solo si no hay peatones"), 1, "Está prohibido adelantar en pasos de peatones y en sus proximidades inmediatas."),
            StaticQuestion("¿Se puede adelantar en un túnel de un solo carril por sentido?", listOf("Sí, con luces", "No, está prohibido", "Solo si el túnel está iluminado"), 1, "En túneles con un solo carril por sentido está prohibido adelantar."),
            StaticQuestion("¿Se puede adelantar en pasos a nivel?", listOf("Sí, si las barreras están abiertas", "No, está prohibido en pasos a nivel y sus proximidades", "Solo a bicis"), 1, "Está prohibido adelantar en pasos a nivel y en sus inmediaciones."),
            StaticQuestion("¿Se puede adelantar invadiendo el carril contrario si hay línea continua?", listOf("Sí, brevemente", "No, la línea continua lo prohíbe", "Solo a vehículos lentos"), 1, "La línea continua prohíbe invadir el sentido contrario para adelantar."),
            StaticQuestion("¿Cuándo se puede adelantar por la derecha?", listOf("Nunca", "Cuando el vehículo de delante ha señalizado giro a la izquierda", "Siempre en autopistas"), 1, "Se puede adelantar por la derecha cuando el de delante indica giro a la izquierda o en tráfico denso con carriles.")
        ),

        "node_5_5_lesson" to listOf( // Parada y prohibiciones
            StaticQuestion("¿Qué diferencia hay entre parada y estacionamiento?", listOf("Son lo mismo", "La parada es menos de 2 minutos sin abandonar el vehículo; el estacionamiento es más", "La parada es para cargar"), 1, "La parada es la inmovilización breve (menos de 2 min y sin abandonar el vehículo)."),
            StaticQuestion("¿Se puede parar en doble fila?", listOf("Sí, si es brevemente", "No, está prohibido siempre", "Solo para carga y descarga"), 1, "La doble fila está prohibida; obstaculiza la circulación y es infracción."),
            StaticQuestion("¿Se puede parar en un carril bici?", listOf("Sí, momentáneamente", "No, nunca", "Solo para dejar pasajeros"), 1, "Está prohibido parar en carriles bici y en pasos de ciclistas."),
            StaticQuestion("¿Se puede parar en un paso de peatones?", listOf("Sí, brevemente", "No, los pasos de peatones deben estar siempre libres", "Solo si no hay peatones"), 1, "Está prohibido parar o estacionar sobre un paso de peatones."),
            StaticQuestion("¿A qué distancia mínima se debe aparcar de una esquina?", listOf("1 metro", "5 metros", "No hay distancia mínima"), 1, "Se debe aparcar a al menos 5 metros de una intersección para no reducir la visibilidad.")
        ),

        "node_5_6_lesson" to listOf( // Estacionamiento: zonas reguladas
            StaticQuestion("¿Qué es la zona azul (ORA)?", listOf("Zona de aparcamiento libre", "Zona de estacionamiento regulado de duración limitada", "Zona para residentes"), 1, "La zona azul permite estacionar un tiempo limitado previo pago del ticket."),
            StaticQuestion("¿Qué indica una línea amarilla continua en el bordillo?", listOf("Se puede aparcar brevemente", "Prohibido estacionar y parar", "Solo para residentes"), 1, "La línea amarilla continua en el bordillo prohíbe la parada y el estacionamiento."),
            StaticQuestion("¿Se puede estacionar en zona de carga y descarga fuera del horario señalizado?", listOf("No, nunca", "Sí, cuando no está en horario de reserva", "Solo residentes"), 1, "Fuera del horario de reserva, la zona de carga y descarga puede usarse como estacionamiento normal."),
            StaticQuestion("¿Se puede estacionar en un vado permanente?", listOf("Sí, si la puerta está cerrada", "No, el vado garantiza acceso permanente", "Solo de noche"), 1, "Un vado permanente prohíbe estacionar ante él en cualquier momento del día."),
            StaticQuestion("¿Qué implica aparcar en plaza de movilidad reducida sin autorización?", listOf("Una multa leve", "Infracción grave con multa cuantiosa", "No pasa nada si es poco tiempo"), 1, "Aparcar en plaza reservada sin tarjeta es infracción grave y muy mal visto socialmente.")
        ),

        "node_5_7_lesson" to listOf( // Giros y cambio de sentido
            StaticQuestion("¿Cómo se debe señalizar un giro a la izquierda?", listOf("Con el intermitente derecho", "Con el intermitente izquierdo y acercándose al centro de la calzada", "No es necesario señalizar"), 1, "Para girar a la izquierda se señaliza con intermitente izquierdo y se ocupa la parte izquierda del carril."),
            StaticQuestion("¿Está permitido el cambio de sentido en cualquier lugar?", listOf("Sí, siempre con precaución", "No, debe hacerse en lugares permitidos y con visibilidad suficiente", "Solo en rotondas"), 1, "El cambio de sentido solo puede hacerse donde esté permitido y con visibilidad."),
            StaticQuestion("¿Se puede hacer un cambio de sentido en una autopista?", listOf("Sí, si no hay tráfico", "No, está totalmente prohibido", "Solo en los enlaces"), 1, "El cambio de sentido está prohibido en autopistas y autovías excepto en los lugares habilitados."),
            StaticQuestion("¿Al girar a la derecha, por qué parte del carril se debe circular?", listOf("Por el centro", "Lo más a la derecha posible", "Por la izquierda"), 1, "Para girar a la derecha hay que ceñirse al borde derecho de la calzada."),
            StaticQuestion("¿Quién tiene prioridad cuando un vehículo realiza un cambio de sentido?", listOf("El que cambia de sentido", "Los demás vehículos que circulan por la vía", "El de la derecha"), 1, "Al cambiar de sentido se debe ceder el paso a todos los demás vehículos.")
        ),

        "node_5_8_lesson" to listOf( // Marcha atrás
            StaticQuestion("¿Cuándo se puede circular marcha atrás?", listOf("Siempre que se quiera", "Solo como maniobra complementaria, con distancia máxima de 15 metros", "No está permitida nunca"), 1, "La marcha atrás solo es complementaria, recorriendo lo estrictamente necesario (máx. 15 m)."),
            StaticQuestion("¿Se puede dar marcha atrás en una autopista?", listOf("Sí, si se pasa la salida", "No, está prohibido en todo tipo de autovías y autopistas", "Solo por el arcén"), 1, "En autopistas y autovías está prohibida la marcha atrás en cualquier circunstancia."),
            StaticQuestion("¿Qué precauciones se deben tomar al dar marcha atrás?", listOf("Ninguna, es una maniobra sencilla", "Comprobar que no hay peatones ni vehículos detrás", "Solo mirar el retrovisor"), 1, "Hay que comprobar retrovisor, ángulo muerto y que no haya peatones ni obstáculos."),
            StaticQuestion("¿Se puede dar marcha atrás en un cruce?", listOf("Sí, con precaución", "No, en intersecciones está prohibida", "Solo si no hay tráfico"), 1, "La marcha atrás está prohibida en intersecciones y sus proximidades."),
            StaticQuestion("¿Quién tiene prioridad cuando un vehículo da marcha atrás?", listOf("El que da marcha atrás", "Los demás usuarios de la vía", "Los peatones solo"), 1, "Al dar marcha atrás, todos los demás usuarios de la vía tienen prioridad.")
        ),

        // ══════════════════════════════════════════════════════════════════
        // SECCIÓN 6 — Vías, Entorno y Conducción Especial
        // ══════════════════════════════════════════════════════════════════

        "node_6_0_lesson" to listOf( // Tipos de vía
            StaticQuestion("¿Cuál es la velocidad máxima en autopistas?", listOf("100 km/h", "120 km/h", "130 km/h"), 1, "El límite máximo para turismos en autopistas es 120 km/h."),
            StaticQuestion("¿Qué diferencia hay entre autopista y autovía?", listOf("La autopista siempre es de peaje", "La autopista no tiene cruces a nivel; la autovía puede tenerlos excepcionalmente", "No hay diferencia"), 1, "Las autopistas no tienen cruces a nivel ni accesos directos; las autovías pueden tener algún acceso."),
            StaticQuestion("¿Qué es una travesía?", listOf("Un tramo de autopista en obras", "Un tramo de carretera que discurre por dentro de una población", "Una rotonda"), 1, "Una travesía es el tramo de una carretera que pasa por el interior de un núcleo urbano."),
            StaticQuestion("¿Qué velocidad máxima rige en una vía convencional de dos carriles?", listOf("100 km/h", "90 km/h", "80 km/h"), 1, "En carreteras convencionales con un carril por sentido el límite es 90 km/h."),
            StaticQuestion("¿Los caminos rurales tienen límite de velocidad?", listOf("No, son libres", "Sí, la prudencia lo exige y normalmente es 30-50 km/h", "Solo si están señalizados"), 1, "Los caminos rurales están sometidos a las normas generales; la prudencia manda.")
        ),

        "node_6_1_lesson" to listOf( // Condiciones meteorológicas adversas
            StaticQuestion("¿Cómo afecta la lluvia a la conducción?", listOf("No afecta con buenos neumáticos", "Reduce la adherencia y la visibilidad", "Solo afecta la visibilidad"), 1, "La lluvia reduce la adherencia del asfalto y la visibilidad del conductor."),
            StaticQuestion("¿Qué es lo primero que debe hacer si hay niebla?", listOf("Poner luces largas", "Reducir velocidad y encender la luz de niebla delantera/trasera", "Detenerse inmediatamente"), 1, "Con niebla se reduce velocidad y se enciende la luz antiniebla delantera; la trasera, solo con niebla densa."),
            StaticQuestion("¿Con viento lateral fuerte, ¿qué vehículos son más vulnerables?", listOf("Los coches bajos", "Furgonetas, camiones y vehículos con gran superficie lateral", "Las motos"), 1, "Los vehículos con gran superficie lateral (furgonetas, camiones) son muy vulnerables al viento lateral."),
            StaticQuestion("¿Qué debe hacer si encuentra hielo en la calzada?", listOf("Frenar bruscamente", "Reducir velocidad suavemente y evitar maniobras bruscas", "Acelerar para pasar rápido"), 1, "En hielo se debe reducir velocidad de forma progresiva y evitar frenazos y volantazos."),
            StaticQuestion("¿Las luces antiniebla traseras se pueden usar siempre que llueve?", listOf("Sí, siempre", "No, solo con niebla densa, lluvia muy intensa o nubes de polvo", "Solo de noche"), 1, "Las antiniebla traseras solo se usan con visibilidad gravemente reducida; deslumbran si no.")
        ),

        "node_6_2_lesson" to listOf( // Conducción nocturna
            StaticQuestion("¿Qué luces se usan de noche en carretera bien iluminada?", listOf("Luces largas", "Luces de cruce", "Solo las de posición"), 1, "En vías bien iluminadas se usan las luces de cruce (cortas)."),
            StaticQuestion("¿Cuándo se cambia de luces largas a cruce?", listOf("Nunca", "Al cruzarse con otro vehículo o seguir a uno a corta distancia", "Solo en autopistas"), 1, "Se cambia a cruce para no deslumbrar al vehículo que viene de frente o al que se sigue."),
            StaticQuestion("¿Qué ocurre si le deslumbran?", listOf("Encender las luces largas como respuesta", "Mirar al borde derecho de la calzada y reducir velocidad", "Parar inmediatamente"), 1, "Se mira al borde derecho, se reduce velocidad y se espera a recuperar la visión normal."),
            StaticQuestion("¿Se pueden usar solo las luces de posición para circular?", listOf("Sí, dentro de poblado", "No, las luces de posición nunca son suficientes para circular", "Solo a menos de 30 km/h"), 1, "Para circular es obligatorio como mínimo las luces de cruce; las de posición solo complementan."),
            StaticQuestion("¿De noche se perciben peor los peatones con ropa oscura?", listOf("No, se ven igual", "Sí, son mucho más difíciles de ver", "Solo si no llevan reflectante"), 1, "Los peatones con ropa oscura son muy difíciles de detectar de noche.")
        ),

        "node_6_3_lesson" to listOf( // Túneles
            StaticQuestion("¿Qué luces se deben encender al entrar en un túnel?", listOf("Las luces largas", "Las luces de cruce", "Solo las de posición"), 1, "Al entrar en un túnel se encienden las luces de cruce, independientemente de la iluminación."),
            StaticQuestion("¿Se puede adelantar dentro de un túnel?", listOf("Sí, si hay dos carriles por sentido", "No, salvo que haya dos o más carriles por sentido y esté permitido", "Nunca"), 1, "Solo se puede adelantar en túneles con dos o más carriles por sentido y sin prohibición."),
            StaticQuestion("¿Qué debe hacer si su vehículo se avería dentro de un túnel?", listOf("Detenerse y esperar ayuda", "Intentar sacar el vehículo, señalizar, apagar motor y abandonar por las salidas de emergencia", "Llamar al seguro"), 1, "Se debe intentar apartar el vehículo, señalizarlo, apagar el motor y dirigirse a una salida de emergencia."),
            StaticQuestion("¿Se deben quitar las gafas de sol al entrar en un túnel?", listOf("No, no importa", "Sí, la reducción de luminosidad puede impedir ver correctamente", "Solo de noche"), 1, "Se deben quitar las gafas de sol antes de entrar para adaptarse a la iluminación del túnel."),
            StaticQuestion("¿Qué distancia de seguridad conviene en túneles?", listOf("La misma que en carretera", "Mayor, al menos 100 metros o 4 segundos", "No importa"), 1, "En túneles se recomienda aumentar la distancia de seguridad para tener más margen de reacción.")
        ),

        "node_6_4_lesson" to listOf( // Conducción eficiente
            StaticQuestion("¿Cómo se consigue una conducción más eficiente?", listOf("Acelerando rápido y frenando fuerte", "Anticipándose, manteniendo velocidad constante y reduciendo frenazos", "Usando marchas bajas"), 1, "La conducción eficiente se basa en anticipación, suavidad y velocidad estable."),
            StaticQuestion("¿En qué marcha se consume menos combustible?", listOf("En primera, para tener más control", "En la marcha más larga posible sin forzar el motor", "En punto muerto"), 1, "Circular en la marcha más alta posible reduce el consumo de combustible."),
            StaticQuestion("¿El aire acondicionado afecta al consumo?", listOf("No, es insignificante", "Sí, puede aumentar el consumo entre un 5% y un 20%", "Solo en coches viejos"), 1, "El aire acondicionado aumenta significativamente el consumo de combustible."),
            StaticQuestion("¿Es eficiente dejar el motor al ralentí durante paradas largas?", listOf("Sí, gasta poco", "No, es mejor apagar el motor si la parada supera 1-2 minutos", "Solo en diésel"), 1, "En paradas superiores a 1 minuto, es más eficiente apagar el motor (Start-Stop)."),
            StaticQuestion("¿La presión de los neumáticos afecta al consumo?", listOf("No", "Sí, neumáticos desinflados aumentan el consumo", "Solo en carretera"), 1, "Neumáticos con presión baja aumentan la resistencia a la rodadura y el consumo.")
        ),

        "node_6_5_lesson" to listOf( // Emisiones, etiquetas DGT y ZBE
            StaticQuestion("¿Cuántas etiquetas ambientales de la DGT existen?", listOf("3", "4: 0 Emisiones, ECO, C y B", "5"), 1, "La DGT otorga 4 etiquetas: 0 Emisiones (azul), ECO (azul-verde), C (verde) y B (amarilla).", "https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_s65a.svg"),
            StaticQuestion("¿Qué es una ZBE (Zona de Bajas Emisiones)?", listOf("Una zona de aparcamiento", "Un área urbana con restricción de acceso según la etiqueta ambiental", "Una autopista ecológica"), 1, "Las ZBE son áreas urbanas donde se restringe el acceso a vehículos según su nivel de emisiones."),
            StaticQuestion("¿Un vehículo sin etiqueta puede circular en una ZBE?", listOf("Sí, siempre", "Generalmente no, salvo excepciones", "Solo de noche"), 1, "Los vehículos sin etiqueta suelen tener acceso restringido o prohibido en las ZBE."),
            StaticQuestion("¿La etiqueta ambiental es obligatoria?", listOf("Solo en ZBE", "No es obligatoria, pero es necesaria para acceder a ZBE y evitar sanciones", "Sí, siempre"), 1, "No es obligatorio llevarla visible, pero sí necesaria para circular en zonas restringidas."),
            StaticQuestion("¿Qué etiqueta tiene un coche eléctrico puro?", listOf("ECO", "0 Emisiones (azul)", "C (verde)"), 1, "Los vehículos eléctricos puros, de pila de combustible y PHEV de largo alcance reciben la etiqueta 0.")
        ),

        "node_6_6_lesson" to listOf( // Accidentes: conducta PAS
            StaticQuestion("¿Qué significa PAS en primeros auxilios?", listOf("Policía, Ambulancia, Seguridad", "Proteger, Avisar, Socorrer", "Parar, Atender, Salir"), 1, "PAS: Proteger la zona, Avisar a emergencias (112) y Socorrer a los heridos."),
            StaticQuestion("¿Cuál es el número de emergencias en España?", listOf("091", "112", "080"), 1, "El 112 es el teléfono único de emergencias en toda la UE."),
            StaticQuestion("¿Se debe mover a un accidentado?", listOf("Sí, para sacarlo del coche", "No, salvo riesgo inminente (incendio, caída al agua)", "Solo si lo pide"), 1, "No se debe mover al herido a menos que haya peligro inminente (fuego, explosión)."),
            StaticQuestion("¿Se debe quitar el casco a un motorista accidentado?", listOf("Sí, siempre", "No, salvo que no respire y sea imprescindible", "Solo si está consciente"), 1, "El casco no se retira salvo que el herido no respire y sea imprescindible para asistirle."),
            StaticQuestion("¿Es obligatorio auxiliar a las víctimas de un accidente?", listOf("No, solo si se es sanitario", "Sí, la omisión del deber de socorro es delito", "Solo si se ha causado el accidente"), 1, "No auxiliar a las víctimas constituye delito de omisión del deber de socorro.")
        ),

        "node_6_7_lesson" to listOf( // Señalización de emergencia
            StaticQuestion("¿Cuántos triángulos de emergencia se llevan actualmente?", listOf("Dos", "Uno, pero se está sustituyendo por la luz V-16", "Ninguno"), 1, "Actualmente se llevan dos triángulos, pero se están sustituyendo por la luz V-16 homologada."),
            StaticQuestion("¿Qué es la luz V-16?", listOf("Una linterna", "Un dispositivo luminoso de preseñalización de peligro homologado", "Una luz de emergencia del vehículo"), 1, "La V-16 es una baliza luminosa geolocalizada que sustituirá a los triángulos."),
            StaticQuestion("¿Es obligatorio llevar chaleco reflectante?", listOf("Solo para conductores profesionales", "Sí, al menos uno por vehículo, y usarlo al salir en vía interurbana", "No, es opcional"), 1, "Es obligatorio llevar un chaleco reflectante y usarlo al salir del vehículo en vía interurbana."),
            StaticQuestion("¿A qué distancia se colocan los triángulos?", listOf("A 10 metros", "A 50 metros como mínimo, uno delante y otro detrás", "A 100 metros detrás"), 1, "Se colocan a al menos 50 metros del vehículo, uno delante y otro detrás (en doble sentido)."),
            StaticQuestion("¿Qué hacer antes de salir del vehículo en una avería en autopista?", listOf("Salir por el lado izquierdo", "Ponerse el chaleco, encender las luces de emergencia y salir por el lado derecho", "Esperar dentro"), 1, "Chaleco reflectante → luces de emergencia → salir por el lado opuesto al tráfico → refugiarse detrás de la barrera.")
        ),

        // ══════════════════════════════════════════════════════════════════
        // SECCIÓN 7 — Otros Usuarios de la Vía
        // ══════════════════════════════════════════════════════════════════

        "node_7_0_lesson" to listOf( // Peatones
            StaticQuestion("¿Tienen los peatones prioridad en un paso de cebra?", listOf("No, siempre mandan los coches", "Sí, los peatones que están cruzando o van a cruzar tienen prioridad", "Solo si el semáforo está en verde para ellos"), 1, "Los peatones tienen prioridad en los pasos de peatones señalizados."),
            StaticQuestion("¿Por dónde deben caminar los peatones en carretera?", listOf("Por la derecha", "Por la izquierda, de frente a los vehículos", "Por el centro"), 1, "En carretera sin arcén, los peatones caminan por la izquierda para ver los vehículos de frente."),
            StaticQuestion("¿Es obligatorio usar elementos reflectantes de noche para peatones?", listOf("No, es opcional", "Sí, en vías interurbanas es obligatorio llevar un elemento reflectante", "Solo para niños"), 1, "De noche, los peatones deben llevar un elemento reflectante visible en vías interurbanas."),
            StaticQuestion("¿Un peatón puede cruzar por donde quiera en poblado?", listOf("Sí, siempre", "No, debe usar los pasos de peatones si existen a menos de 50 metros", "Solo si no hay tráfico"), 1, "Los peatones deben usar el paso de peatones si hay uno a menos de 50 metros."),
            StaticQuestion("¿Qué debe hacer un conductor al acercarse a un paso de peatones?", listOf("Acelerar para pasar rápido", "Reducir velocidad y estar preparado para detenerse", "Tocar el claxon para avisar"), 1, "Al acercarse a un paso de peatones hay que reducir la velocidad y ceder el paso.")
        ),

        "node_7_1_lesson" to listOf( // Ciclistas
            StaticQuestion("¿Los ciclistas pueden circular por la acera?", listOf("Sí, siempre", "Solo los menores de 14 años en determinadas condiciones", "Sí, si van despacio"), 1, "Generalmente los ciclistas no pueden ir por aceras; los menores tienen excepciones locales."),
            StaticQuestion("¿Es obligatorio el casco para ciclistas?", listOf("Siempre", "En vías interurbanas sí; en urbanas solo para menores de 16 años", "Nunca"), 1, "El casco es obligatorio en interurbanas para todos y en urbanas para menores de 16 años."),
            StaticQuestion("¿Los ciclistas pueden circular en paralelo?", listOf("Nunca", "Sí, en grupo de dos en fila, y en grupo en columna de a uno", "Siempre que quieran"), 1, "Pueden ir en paralelo (de dos en fondo) pero en determinadas circunstancias deben ir en columna de a uno."),
            StaticQuestion("¿Un ciclista puede usar auriculares?", listOf("Sí, con un solo auricular", "No, está prohibido usar auriculares o cascos de audio", "Sí, si son inalámbricos"), 1, "Los ciclistas también tienen prohibido usar auriculares conectados a dispositivos de sonido."),
            StaticQuestion("¿Qué separación lateral mínima al adelantar a un ciclista?", listOf("1 metro", "1,5 metros", "2 metros"), 1, "Se debe dejar al menos 1,5 metros al adelantar a un ciclista.")
        ),

        "node_7_2_lesson" to listOf( // VMP: patinetes
            StaticQuestion("¿Qué es un VMP?", listOf("Un tipo de moto", "Vehículo de Movilidad Personal (patinete eléctrico y similares)", "Un velomotor pesado"), 1, "Los VMP son vehículos como patinetes eléctricos; no necesitan matriculación pero sí cumplir normas."),
            StaticQuestion("¿Pueden los VMP circular por aceras?", listOf("Sí, siempre", "No, está prohibido circular por aceras y zonas peatonales", "Solo a velocidad de peatón"), 1, "Los VMP no pueden circular por aceras, zonas peatonales ni vías interurbanas."),
            StaticQuestion("¿Cuál es la velocidad máxima de un VMP?", listOf("50 km/h", "25 km/h", "30 km/h"), 1, "Los VMP no pueden superar los 25 km/h."),
            StaticQuestion("¿Necesitan los VMP seguro obligatorio?", listOf("No", "Sí, algunas ordenanzas locales lo exigen", "Solo si superan 20 km/h"), 1, "La regulación local puede exigir seguro; la DGT recomienda tenerlo."),
            StaticQuestion("¿Pueden los VMP circular por autopistas o autovías?", listOf("Sí, por el arcén", "No, está totalmente prohibido", "Solo de día"), 1, "Los VMP tienen prohibida la circulación en autopistas, autovías y vías interurbanas.")
        ),

        "node_7_3_lesson" to listOf( // Motocicletas y ciclomotores
            StaticQuestion("¿Es obligatorio el casco para motos y ciclomotores?", listOf("Solo en carretera", "Sí, siempre, tanto conductor como pasajero", "Solo para el conductor"), 1, "El casco es obligatorio siempre, para conductor y pasajero, en todas las vías."),
            StaticQuestion("¿A partir de qué edad se puede conducir un ciclomotor?", listOf("14 años", "15 años", "16 años"), 1, "Se puede conducir un ciclomotor a partir de los 15 años con el permiso AM."),
            StaticQuestion("¿Cuál es la velocidad máxima de un ciclomotor?", listOf("50 km/h", "45 km/h", "60 km/h"), 1, "Los ciclomotores tienen limitada su velocidad a 45 km/h por construcción."),
            StaticQuestion("¿Pueden las motos circular entre filas de coches en retención?", listOf("Sí, siempre", "Con prudencia y a velocidad moderada, según normativa local", "No, nunca"), 1, "En determinadas condiciones de retención pueden filtrar entre filas con extrema precaución."),
            StaticQuestion("¿Es obligatorio llevar guantes en moto?", listOf("Sí, homologados", "No es obligatorio pero sí muy recomendable", "Solo en autopista"), 1, "Actualmente no es obligatorio por ley en España, pero se recomienda encarecidamente.")
        ),

        "node_7_4_lesson" to listOf( // Vehículos de emergencia
            StaticQuestion("¿Qué hacer cuando se acerca un vehículo de emergencia con sirena y luces?", listOf("Acelerar para apartarse rápido", "Facilitar el paso apartándose a la derecha o deteniéndose", "Seguir normal y dejar que pase"), 1, "Hay que facilitar el paso apartándose o deteniéndose lo antes posible."),
            StaticQuestion("¿Los vehículos de emergencia pueden saltarse los semáforos?", listOf("No, nunca", "Sí, cuando están en servicio de emergencia, con precaución", "Solo las ambulancias"), 1, "En servicio de emergencia pueden no respetar señales, pero deben extremar la precaución."),
            StaticQuestion("¿Puede un conductor particular usar sirena o luces azules?", listOf("Sí, si las compra", "No, es ilegal y sancionable", "Solo en emergencias reales"), 1, "El uso de sirenas y luces de emergencia está reservado a vehículos autorizados."),
            StaticQuestion("¿Los bomberos tienen las mismas prerrogativas que las ambulancias?", listOf("No, solo la policía", "Sí, policía, bomberos, ambulancias y protección civil", "Solo en incendios"), 1, "Todos los servicios de emergencia (policía, bomberos, sanitarios, protección civil) tienen prioridad."),
            StaticQuestion("¿Puede un vehículo de emergencia circular contrarirección?", listOf("Nunca", "Sí, en servicio urgente y con máxima precaución", "Solo en vías urbanas"), 1, "En servicio de emergencia pueden adoptar maniobras excepcionales con la debida precaución.")
        ),

        "node_7_5_lesson" to listOf( // Transporte escolar y de viajeros
            StaticQuestion("¿Qué debe hacer al ver un autobús escolar detenido con las luces de emergencia?", listOf("Adelantar rápido", "Extremar la precaución, hay niños subiendo o bajando", "No tiene relevancia"), 1, "Al ver un autobús escolar parado hay que reducir velocidad porque los niños pueden cruzar."),
            StaticQuestion("¿Los autobuses tienen prioridad al reincorporarse a la circulación?", listOf("No, deben esperar como todos", "Sí, en vías urbanas el autobús tiene prioridad al salir de las paradas", "Solo los escolares"), 1, "En poblado, los autobuses urbanos tienen prioridad para reincorporarse desde paradas señalizadas."),
            StaticQuestion("¿Qué señal lleva un autobús escolar?", listOf("Una señal de STOP", "Una señal cuadrada naranja con dos niños", "No lleva señal especial"), 1, "Los autobuses escolares llevan una señal identificativa con el dibujo de niños."),
            StaticQuestion("¿Se puede adelantar a un autobús escolar parado?", listOf("Sí, rápidamente", "Con máxima precaución y a velocidad reducida", "No, debe esperar"), 1, "Se puede pasar pero con extrema precaución y velocidad reducida por posible cruce de niños."),
            StaticQuestion("¿Los niños deben llevar cinturón en el autobús escolar?", listOf("No, van de pie", "Sí, es obligatorio si el autobús dispone de cinturones", "Solo los menores de 6 años"), 1, "Si el autobús dispone de cinturones de seguridad, su uso es obligatorio.")
        ),

        "node_7_6_lesson" to listOf( // Mercancías peligrosas
            StaticQuestion("¿Qué identifica a un vehículo que transporta mercancías peligrosas?", listOf("Solo el color del camión", "Paneles rectangulares naranjas y etiquetas de peligro", "Una sirena especial"), 1, "Los vehículos con MMPP llevan paneles naranjas reflectantes y etiquetas con pictogramas de peligro."),
            StaticQuestion("¿Qué distancia de seguridad se recomienda con un camión de mercancías peligrosas?", listOf("La normal", "Mayor de lo habitual, al menos 50 metros", "5 metros"), 1, "Conviene mantener una distancia de seguridad mayor con vehículos de MMPP."),
            StaticQuestion("¿Pueden los vehículos de MMPP circular por túneles?", listOf("Sí, siempre", "Algunos túneles los prohíben con señalización específica", "No, nunca"), 1, "Ciertos túneles restringen el paso de MMPP según su categoría y peligrosidad."),
            StaticQuestion("¿Qué debe hacer si se ve involucrado en un accidente con un camión de MMPP?", listOf("Acercarse para ayudar", "Alejarse, no tocar nada y llamar al 112 indicando el panel naranja", "Echar agua si hay fuego"), 1, "Hay que alejarse, no tocar la carga, y comunicar al 112 los números del panel naranja."),
            StaticQuestion("¿Qué información contiene el panel naranja de un camión de MMPP?", listOf("El teléfono del conductor", "El número de identificación de peligro y el número ONU de la materia", "La matrícula"), 1, "El panel contiene el código de peligro (arriba) y el número ONU de la sustancia (abajo).")
        ),

        // ══════════════════════════════════════════════════════════════════
        // SECCIÓN 8 — Infracciones y Sanciones
        // ══════════════════════════════════════════════════════════════════

        "node_8_0_lesson" to listOf( // Clasificación de infracciones
            StaticQuestion("¿Cómo se clasifican las infracciones de tráfico?", listOf("En leves, moderadas y graves", "En leves, graves y muy graves", "En menores y mayores"), 1, "Las infracciones se clasifican en leves, graves y muy graves."),
            StaticQuestion("¿Circular sin las luces obligatorias encendidas es infracción…?", listOf("Leve", "Grave", "Muy grave"), 1, "Circular sin luces obligatorias es infracción grave."),
            StaticQuestion("¿Conducir bajo los efectos del alcohol es infracción…?", listOf("Leve", "Grave", "Muy grave"), 2, "Conducir bajo los efectos del alcohol es infracción muy grave."),
            StaticQuestion("¿No respetar un semáforo en rojo es infracción…?", listOf("Leve", "Grave", "Muy grave"), 1, "Saltarse un semáforo en rojo es infracción grave."),
            StaticQuestion("¿Conducir hablando por el móvil sin manos libres es infracción…?", listOf("Leve", "Grave", "Muy grave"), 1, "Usar el móvil sin manos libres al conducir es infracción grave con pérdida de puntos.")
        ),

        "node_8_1_lesson" to listOf( // Sanciones: multas y retirada
            StaticQuestion("¿Cuál es la multa por exceso de velocidad grave?", listOf("100 euros", "De 300 a 600 euros", "50 euros"), 1, "Las multas por exceso de velocidad grave oscilan entre 300 y 600 euros."),
            StaticQuestion("¿Se puede pagar la multa con descuento?", listOf("No, nunca", "Sí, un 50% de descuento por pronto pago en 20 días naturales", "Solo las leves"), 1, "Se obtiene un 50% de descuento pagando en los 20 días naturales siguientes a la notificación."),
            StaticQuestion("¿Qué implica la retirada del permiso de conducir?", listOf("Solo una penalización económica", "No poder conducir durante el plazo fijado judicialmente", "Un curso de reeducación"), 1, "La retirada judicial prohíbe conducir durante el periodo que fije el juez."),
            StaticQuestion("¿Se puede recurrir una multa de tráfico?", listOf("No, es firme siempre", "Sí, mediante recurso de alzada o reclamación económico-administrativa", "Solo si es grave"), 1, "Toda sanción es recurrible dentro de los plazos legales establecidos."),
            StaticQuestion("¿Una multa de tráfico puede conllevar cárcel?", listOf("No, nunca", "Sí, si la infracción constituye delito contra la seguridad vial", "Solo en caso de accidente"), 1, "Conducir ebrio, a velocidad delictiva o sin permiso pueden ser delitos con pena de prisión.")
        ),

        "node_8_2_lesson" to listOf( // Pérdida y recuperación de puntos
            StaticQuestion("¿Con cuántos puntos empieza un conductor novel?", listOf("12 puntos", "8 puntos", "6 puntos"), 1, "Los conductores noveles comienzan con 8 puntos (suben a 12 tras 2 años sin infracciones)."),
            StaticQuestion("¿Y un conductor con experiencia?", listOf("8 puntos", "12 puntos", "15 puntos"), 1, "Los conductores con más de 2 años de experiencia disponen de 12 puntos."),
            StaticQuestion("¿Se pueden recuperar puntos?", listOf("No, una vez perdidos no se recuperan", "Sí, con cursos de recuperación y por el paso del tiempo sin infracciones", "Solo pagando una tasa"), 1, "Se pueden recuperar hasta 6 puntos con un curso, y todos por buena conducta durante 2-3 años."),
            StaticQuestion("¿Qué ocurre si se pierden todos los puntos?", listOf("Multa y ya está", "Se pierde el permiso de conducir y hay que hacer un curso completo", "Se reducen a la mitad"), 1, "Al llegar a 0 puntos se pierde el permiso y hay que superar un curso y un examen para recuperarlo."),
            StaticQuestion("¿Un conductor bonificado puede llegar a tener más de 12 puntos?", listOf("No, 12 es el máximo", "Sí, hasta 15 puntos por buen comportamiento", "Hasta 20 puntos"), 1, "Los conductores que no cometen infracciones en 3 años pueden alcanzar hasta 15 puntos.")
        ),

        "node_8_3_lesson" to listOf( // Seguro obligatorio
            StaticQuestion("¿Es obligatorio tener seguro de responsabilidad civil para circular?", listOf("No, es voluntario", "Sí, es obligatorio para todo vehículo a motor", "Solo en autopistas"), 1, "El seguro de responsabilidad civil (seguro a terceros) es obligatorio por ley."),
            StaticQuestion("¿Qué cubre el seguro a terceros?", listOf("Los daños del propio vehículo", "Los daños causados a terceras personas y sus bienes", "Nada, solo es un trámite"), 1, "El seguro a terceros cubre los daños que causemos a otras personas y sus propiedades."),
            StaticQuestion("¿Qué es un seguro a todo riesgo?", listOf("El más barato", "Cubre daños propios además de los de terceros", "Un seguro ilegal"), 1, "El seguro a todo riesgo incluye la cobertura de daños propios, además de los daños a terceros."),
            StaticQuestion("¿Se puede circular con el seguro vencido?", listOf("Sí, tiene un mes de gracia", "No, circular sin seguro en vigor es infracción muy grave", "Solo dentro de poblado"), 1, "Circular sin seguro vigente es infracción muy grave con multas de 601 a 3.005 euros."),
            StaticQuestion("¿Quién paga si un vehículo sin seguro causa un accidente?", listOf("Nadie", "El conductor responde personalmente y el Consorcio de Compensación de Seguros anticipa los pagos", "La DGT"), 1, "El CCS puede anticipar las indemnizaciones y luego reclamar al responsable.")
        ),

        "node_8_4_lesson" to listOf( // Responsabilidad civil y penal
            StaticQuestion("¿Qué es la responsabilidad civil del conductor?", listOf("La obligación de cumplir las normas", "La obligación de indemnizar los daños causados a terceros", "La responsabilidad penal"), 1, "La responsabilidad civil obliga a reparar económicamente los daños causados."),
            StaticQuestion("¿Qué es la responsabilidad penal del conductor?", listOf("Pagar las multas", "Responder ante los tribunales por delitos cometidos al volante", "Perder puntos"), 1, "La responsabilidad penal implica un proceso judicial con posibles penas de prisión, multa o privación de derechos."),
            StaticQuestion("¿Conducir sin permiso es delito?", listOf("No, solo infracción administrativa", "Sí, puede ser delito contra la seguridad vial", "Solo si se es reincidente"), 1, "Conducir sin haber obtenido nunca el permiso, o con el permiso retirado, puede ser delito."),
            StaticQuestion("¿Qué es el delito de omisión del deber de socorro?", listOf("No parar en un semáforo", "No ayudar a las víctimas de un accidente pudiendo hacerlo", "No llevar triángulos"), 1, "No auxiliar a las víctimas cuando se puede es delito de omisión del deber de socorro."),
            StaticQuestion("¿Un acompañante puede ser responsable de un accidente?", listOf("Nunca", "Sí, si contribuye a causarlo (ej: distraer al conductor, tirar del volante)", "Solo el conductor es responsable"), 1, "Un acompañante que contribuya al accidente puede ser considerado responsable.")
        ),

        "node_8_5_lesson" to listOf( // Alcoholemia: pruebas y consecuencias
            StaticQuestion("¿Quién puede someterse a una prueba de alcoholemia?", listOf("Solo los sospechosos", "Cualquier conductor puede ser requerido para una prueba", "Solo en caso de accidente"), 1, "Cualquier conductor puede ser requerido por los agentes para someterse a una prueba de alcoholemia."),
            StaticQuestion("¿Qué ocurre si se da positivo en la primera prueba?", listOf("Se retira el permiso inmediatamente", "Se realiza una segunda prueba tras 10 minutos y se puede solicitar análisis de sangre", "Se impone la multa directamente"), 1, "Tras un positivo, se hace una segunda prueba a los 10 minutos; el conductor puede pedir contraste en sangre."),
            StaticQuestion("¿Negarse a la prueba de alcoholemia es…?", listOf("Un derecho del conductor", "Un delito contra la seguridad vial", "Una infracción leve"), 1, "La negativa a someterse a las pruebas de detección es un delito penal."),
            StaticQuestion("¿A partir de qué tasa se considera delito contra la seguridad vial?", listOf("0,5 g/l", "0,6 g/l en sangre (o 0,30 mg/l en aire)", "Cualquier tasa"), 1, "Superar 0,60 g/l en sangre (0,30 mg/l en aire) es delito contra la seguridad vial."),
            StaticQuestion("¿Qué pena puede llevar un delito de alcoholemia?", listOf("Solo multa", "Prisión de 3 a 6 meses o multa, más retirada de permiso", "Solo pérdida de puntos"), 1, "Las penas pueden incluir prisión de 3 a 6 meses o multa, trabajos comunitarios y retirada del permiso.")
        )
    )

    /**
     * Returns up to [count] questions for the given [nodeId], shuffled.
     */
    fun getQuestionsForNode(nodeId: String, count: Int = 10): List<StaticQuestion> {
        val all = questionsPerNode[nodeId] ?: return emptyList()
        return all.shuffled().take(count)
    }
}
