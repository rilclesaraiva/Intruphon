package com.intruphon.app.data.download

import org.junit.Assert.assertEquals
import org.junit.Test

class FileDetailsTest {
    @Test fun pathDropsTheVolumeOfADocumentId() {
        assertEquals("Music/Intruphon/Audio/a.m4a", FileDetails.pathFromDocumentId("primary:Music/Intruphon/Audio/a.m4a"))
        assertEquals("a.m4a", FileDetails.pathFromDocumentId("a.m4a"))
    }

    @Test fun knownCodecsGetShortNames() {
        assertEquals("AAC", FileDetails.codecName("audio/mp4a-latm"))
        assertEquals("H.264", FileDetails.codecName("video/avc"))
        assertEquals("MP3", FileDetails.codecName("AUDIO/MPEG"))
    }

    @Test fun unknownCodecKeepsItsMimeType() = assertEquals("audio/x-weird", FileDetails.codecName("audio/x-weird"))
}
