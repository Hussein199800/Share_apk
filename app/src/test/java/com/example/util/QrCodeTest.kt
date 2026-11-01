package com.example.util

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Test

class QrCodeTest {
    @Test fun generatedCodeDecodesToTheUrl() {
        val url = "http://192.168.120.61:8787"
        val m = QrCodeGenerator.matrix(url, 300)
        val pixels = IntArray(m.width * m.height) { i -> if (m.get(i % m.width, i / m.width)) 0xFF000000.toInt() else -1 }
        val bmp = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(m.width, m.height, pixels)))
        assertEquals(url, QRCodeReader().decode(bmp).text)
    }
}
