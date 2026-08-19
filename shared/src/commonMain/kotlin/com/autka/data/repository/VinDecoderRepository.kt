package com.autka.data.repository

import com.autka.core.model.VinDecodeResult

interface VinDecoderRepository {
    suspend fun decode(vin: String): VinDecodeResult
}
