package com.autka.data.repository

import com.autka.core.model.VinDecodeResult
import com.autka.data.remote.vin.VpicApi
import com.autka.data.remote.vin.VpicDecoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultVinDecoderRepository @Inject constructor(
    private val api: VpicApi,
) : VinDecoderRepository {
    override suspend fun decode(vin: String): VinDecodeResult {
        val response = api.decodeVin(vin)
        return VpicDecoder.decode(response, fallbackVin = vin)
    }
}
