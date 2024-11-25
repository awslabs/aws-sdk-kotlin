/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.transcribestreaming.TranscribeStreamingClient
import aws.sdk.kotlin.services.transcribestreaming.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Paths
import javax.sound.sampled.AudioSystem
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TranscribeStreamingIntegrationTest {
    private fun resource(path: String): File {
        val url = this::class.java.classLoader.getResource(path) ?: error("failed to load test resource $path")
        return Paths.get(url.toURI()).toFile()
    }

    @Test
    fun testTranscribeEventStream(): Unit = runBlocking {
        val file = resource("hello-kotlin-8000.wav")
        val audioStream = audioStreamFromFile(file)
        TranscribeStreamingClient { region = "us-west-2" }.use { client ->
            val transcript = getTranscript(client, audioStream)
            assertTrue(transcript.startsWith("Hello from", true), "full transcript: $transcript")
        }
    }

    @Test
    fun testTranscribeEventStreamWithLongPause(): Unit = runBlocking {
        val file = resource("hello-kotlin-8000.wav")
        val audioStream = audioStreamFromFile(file, 20.seconds) // ~15 seconds should cause service to terminate stream
        TranscribeStreamingClient { region = "us-west-2" }.use { client ->
            assertThrows<BadRequestException> { getTranscript(client, audioStream) }
        }
    }
}

private const val FRAMES_PER_CHUNK = 4096

private fun audioStreamFromFile(file: File, finalDelay: Duration? = null): Flow<AudioStream> {
    val format = AudioSystem.getAudioFileFormat(file)
    val ais = AudioSystem.getAudioInputStream(file)
    val bytesPerFrame = ais.format.frameSize
    println("audio stream format of $file: $format; bytesPerFrame=$bytesPerFrame")

    return flow {
        while (true) {
            val frameBuffer = ByteArray(FRAMES_PER_CHUNK * bytesPerFrame)
            val rc = ais.read(frameBuffer)
            if (rc <= 0) {
                finalDelay?.let {
                    println("Artificially delaying for $finalDelay")
                    delay(it)
                }
                break
            }

            val chunk = if (rc < frameBuffer.size) frameBuffer.sliceArray(0 until rc) else frameBuffer
            val event = AudioStream.AudioEvent(
                AudioEvent {
                    audioChunk = chunk
                },
            )

            println("emitting event")
            emit(event)
        }
    }.flowOn(Dispatchers.IO)
}

private suspend fun getTranscript(client: TranscribeStreamingClient, audioStream: Flow<AudioStream>): String {
    val req = StartStreamTranscriptionRequest {
        languageCode = LanguageCode.EnUs
        mediaSampleRateHertz = 8000
        mediaEncoding = MediaEncoding.Pcm
        this.audioStream = audioStream
    }

    val transcript = client.startStreamTranscription(req) { resp ->
        val fullMessage = StringBuilder()
        resp
            .transcriptResultStream
            ?.collect { event ->
                when (event) {
                    is TranscriptResultStream.TranscriptEvent -> {
                        event.value.transcript?.results?.forEach { result ->
                            val transcript = result.alternatives?.firstOrNull()?.transcript
                            println("received TranscriptEvent: isPartial=${result.isPartial}; transcript=$transcript")
                            if (!result.isPartial) {
                                transcript?.let { fullMessage.append(it) }
                            }
                        }
                    }
                    else -> error("unknown event $event")
                }
            }
        fullMessage.toString()
    }

    return transcript
}
