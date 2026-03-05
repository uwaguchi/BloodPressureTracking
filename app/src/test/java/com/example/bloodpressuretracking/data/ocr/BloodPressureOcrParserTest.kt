package com.example.bloodpressuretracking.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BloodPressureOcrParserTest {

    private lateinit var parser: BloodPressureOcrParser

    @Before
    fun setup() {
        parser = BloodPressureOcrParser()
    }

    // ---- スラッシュ区切りパターン ----

    @Test
    fun `スラッシュ区切りパターンで3値を正常解析する`() {
        val result = parser.parse("120/80 70")
        assertTrue(result is OcrResult.Success)
        val values = (result as OcrResult.Success).values
        assertEquals(120, values.systolic)
        assertEquals(80, values.diastolic)
        assertEquals(70, values.pulse)
    }

    @Test
    fun `ノイズテキストが混在してもスラッシュパターンを正常解析する`() {
        val result = parser.parse("血圧 120/80 脈拍70 bpm")
        assertTrue(result is OcrResult.Success)
        val values = (result as OcrResult.Success).values
        assertEquals(120, values.systolic)
        assertEquals(80, values.diastolic)
        assertEquals(70, values.pulse)
    }

    // ---- 3行表示パターン ----

    @Test
    fun `3行表示パターンで3値を正常解析する`() {
        val result = parser.parse("120\n80\n70")
        assertTrue(result is OcrResult.Success)
        val values = (result as OcrResult.Success).values
        assertEquals(120, values.systolic)
        assertEquals(80, values.diastolic)
        assertEquals(70, values.pulse)
    }

    // ---- バリデーション: 範囲外 ----

    @Test
    fun `収縮期血圧が有効範囲下限未満のときVALUES_OUT_OF_RANGEを返す`() {
        val result = parser.parse("50/80\n70")
        assertTrue(result is OcrResult.Failure)
        assertEquals(OcrFailureReason.VALUES_OUT_OF_RANGE, (result as OcrResult.Failure).reason)
    }

    @Test
    fun `収縮期血圧が有効範囲上限超過のときVALUES_OUT_OF_RANGEを返す`() {
        val result = parser.parse("310/80\n70")
        assertTrue(result is OcrResult.Failure)
        assertEquals(OcrFailureReason.VALUES_OUT_OF_RANGE, (result as OcrResult.Failure).reason)
    }

    @Test
    fun `拡張期血圧が有効範囲上限超過のときVALUES_OUT_OF_RANGEを返す`() {
        val result = parser.parse("120/210\n70")
        assertTrue(result is OcrResult.Failure)
        assertEquals(OcrFailureReason.VALUES_OUT_OF_RANGE, (result as OcrResult.Failure).reason)
    }

    @Test
    fun `脈拍が有効範囲下限未満のときVALUES_OUT_OF_RANGEを返す`() {
        val result = parser.parse("120/80\n10")
        assertTrue(result is OcrResult.Failure)
        assertEquals(OcrFailureReason.VALUES_OUT_OF_RANGE, (result as OcrResult.Failure).reason)
    }

    @Test
    fun `脈拍が有効範囲上限超過のときVALUES_OUT_OF_RANGEを返す`() {
        val result = parser.parse("120/80\n350")
        assertTrue(result is OcrResult.Failure)
        assertEquals(OcrFailureReason.VALUES_OUT_OF_RANGE, (result as OcrResult.Failure).reason)
    }

    // ---- バリデーション: 収縮期≦拡張期 ----

    @Test
    fun `収縮期が拡張期より小さいときINVALID_COMBINATIONを返す`() {
        val result = parser.parse("80/120\n70")
        assertTrue(result is OcrResult.Failure)
        assertEquals(OcrFailureReason.INVALID_COMBINATION, (result as OcrResult.Failure).reason)
    }

    @Test
    fun `収縮期と拡張期が等しいときINVALID_COMBINATIONを返す`() {
        val result = parser.parse("100/100\n70")
        assertTrue(result is OcrResult.Failure)
        assertEquals(OcrFailureReason.INVALID_COMBINATION, (result as OcrResult.Failure).reason)
    }

    // ---- テキスト未検出・数値不足 ----

    @Test
    fun `空文字のときNO_TEXT_DETECTEDを返す`() {
        val result = parser.parse("")
        assertTrue(result is OcrResult.Failure)
        assertEquals(OcrFailureReason.NO_TEXT_DETECTED, (result as OcrResult.Failure).reason)
    }

    @Test
    fun `数値を含まないテキストのときINSUFFICIENT_VALUESを返す`() {
        val result = parser.parse("abc def ghi")
        assertTrue(result is OcrResult.Failure)
        assertEquals(OcrFailureReason.INSUFFICIENT_VALUES, (result as OcrResult.Failure).reason)
    }

    @Test
    fun `数値が2つしかないときINSUFFICIENT_VALUESを返す`() {
        val result = parser.parse("120\n80")
        assertTrue(result is OcrResult.Failure)
        assertEquals(OcrFailureReason.INSUFFICIENT_VALUES, (result as OcrResult.Failure).reason)
    }

    @Test
    fun `スラッシュパターンで脈拍が見つからないときINSUFFICIENT_VALUESを返す`() {
        val result = parser.parse("120/80")
        assertTrue(result is OcrResult.Failure)
        assertEquals(OcrFailureReason.INSUFFICIENT_VALUES, (result as OcrResult.Failure).reason)
    }
}
