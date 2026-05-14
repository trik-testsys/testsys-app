package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.recordingData
import tech.testsys.domain.model.task.Recording
import tech.testsys.domain.model.task.RecordingData

class RecordingBuilderTests : DomainEntityBuilderTests<Recording, RecordingData, RecordingDataBuilder>(
    RecordingBuilder(),
    RecordingDataBuilder()
) {
    override fun buildDataWithAllFields() = listOf(recordingData {
        file("recording.mp4", byteArrayOf(1, 2, 3))
    })
}
