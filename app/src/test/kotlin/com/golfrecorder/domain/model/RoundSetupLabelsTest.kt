package com.golfrecorder.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RoundSetupLabelsTest {

    @Test
    fun `location source label names the device used for shot GPS`() {
        assertEquals("와치 GPS", locationSourceLabel(LocationSource.WATCH.name))
        assertEquals("핸드폰 GPS", locationSourceLabel(LocationSource.PHONE.name))
    }

    @Test
    fun `map provider label names the map used for the round`() {
        assertEquals("카카오맵", mapProviderLabel(MapProvider.KAKAO.name))
        assertEquals("구글맵", mapProviderLabel(MapProvider.GOOGLE.name))
    }

    @Test
    fun `setup note joins the location source and the map provider`() {
        assertEquals("와치 GPS · 카카오맵", roundSetupNote(LocationSource.WATCH.name, MapProvider.KAKAO.name))
        assertEquals("핸드폰 GPS · 구글맵", roundSetupNote(LocationSource.PHONE.name, MapProvider.GOOGLE.name))
    }

    @Test
    fun `unknown stored values fall back to the round defaults`() {
        assertEquals("핸드폰 GPS", locationSourceLabel(""))
        assertEquals("카카오맵", mapProviderLabel("something-else"))
    }
}
