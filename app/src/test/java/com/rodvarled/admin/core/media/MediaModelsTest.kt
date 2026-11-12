package com.rodvarled.admin.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaModelsTest {

    @Test
    fun stageCyclesThroughAllFourAndComesBack() {
        var stage = MediaStage.BEFORE
        val visited = mutableListOf(stage)
        repeat(4) {
            stage = stage.next()
            visited += stage
        }

        assertEquals(
            listOf(MediaStage.BEFORE, MediaStage.DURING, MediaStage.AFTER, MediaStage.OTHER, MediaStage.BEFORE),
            visited
        )
    }

    @Test
    fun stageParsesServerValuesWithoutAccents() {
        assertEquals(MediaStage.AFTER, MediaStage.fromApi("Despues"))
        assertEquals(MediaStage.BEFORE, MediaStage.fromApi("antes"))
        assertEquals(MediaStage.OTHER, MediaStage.fromApi("algo raro"))
        assertEquals(MediaStage.OTHER, MediaStage.fromApi(null))
    }

    @Test
    fun kindParsesServerValues() {
        assertEquals(MediaKind.VIDEO, MediaKind.fromApi("Video"))
        assertEquals(MediaKind.PHOTO, MediaKind.fromApi("Foto"))
        assertEquals(MediaKind.PHOTO, MediaKind.fromApi(null))
    }
}
