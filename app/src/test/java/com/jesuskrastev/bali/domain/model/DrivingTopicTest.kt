package com.jesuskrastev.bali.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DrivingTopicTest {

    @Test
    fun `the ten topic names the AI is told to use map to their own topic`() {
        val names = listOf(
            "Alumbrado", "Prioridad", "Maniobras", "Velocidad", "El conductor",
            "Mecánica", "Documentación", "Usuarios de la vía", "Señales", "Marcas viales"
        )

        val topics = names.map { DrivingTopic.fromCategory(it) }

        assertThat(topics).containsExactlyElementsIn(DrivingTopic.entries)
        names.forEachIndexed { index, name ->
            assertThat(topics[index]?.displayName).isEqualTo(name)
        }
    }

    @Test
    fun `matching ignores case and accents`() {
        assertThat(DrivingTopic.fromCategory("SEÑALES DE TRÁFICO")).isEqualTo(DrivingTopic.SIGNS)
        assertThat(DrivingTopic.fromCategory("documentacion del vehiculo")).isEqualTo(DrivingTopic.DOCUMENTS)
    }

    @Test
    fun `a learning-path title that names a topic is classified`() {
        assertThat(DrivingTopic.fromCategory("Adelantamientos seguros")).isEqualTo(DrivingTopic.MANEUVERS)
        assertThat(DrivingTopic.fromCategory("Cedas el paso en glorietas")).isEqualTo(DrivingTopic.RIGHT_OF_WAY)
        assertThat(DrivingTopic.fromCategory("Alcohol y fatiga al volante")).isEqualTo(DrivingTopic.DRIVER)
    }

    @Test
    fun `road markings win over signs when a title mentions both`() {
        assertThat(DrivingTopic.fromCategory("Señales y marcas viales")).isEqualTo(DrivingTopic.ROAD_MARKINGS)
    }

    @Test
    fun `categories that name no topic are not classified`() {
        assertThat(DrivingTopic.fromCategory("Lección")).isNull()
        assertThat(DrivingTopic.fromCategory("")).isNull()
        assertThat(DrivingTopic.fromCategory("   ")).isNull()
        assertThat(DrivingTopic.fromCategory(ExamRules.OFFICIAL_EXAM_CATEGORY)).isNull()
    }
}
