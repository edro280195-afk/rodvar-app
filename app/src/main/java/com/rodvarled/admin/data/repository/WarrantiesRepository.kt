package com.rodvarled.admin.data.repository

import com.rodvarled.admin.core.util.safeApiCall
import com.rodvarled.admin.data.remote.api.WarrantiesApi
import com.rodvarled.admin.data.remote.dto.MessageResponse
import com.rodvarled.admin.data.remote.dto.SignWarrantyRequest
import com.rodvarled.admin.data.remote.dto.CreateWarrantyClaimRequest
import com.rodvarled.admin.data.remote.dto.UpdateWarrantyRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WarrantiesRepository @Inject constructor(
    private val api: WarrantiesApi
) {
    suspend fun getWarranties() = safeApiCall { api.getWarranties() }
    suspend fun getWarranty(id: Int) = safeApiCall { api.getWarranty(id) }
    suspend fun createClaim(id: Int, request: CreateWarrantyClaimRequest) = safeApiCall { api.createClaim(id, request) }
    suspend fun sign(id: Int, signatureBase64: String): Result<MessageResponse> {
        val result = safeApiCall {
            api.signWarranty(id, SignWarrantyRequest(signatureBase64, acceptedTerms = true))
        }

        if (result.isSuccess) return result

        // Si la respuesta se perdió después de que la API guardó la firma, una segunda
        // consulta evita reportar un falso error y mandar al instalador al enlace web.
        val warrantyWasSigned = getWarranty(id).getOrNull()?.isSigned == true
        return if (warrantyWasSigned) {
            Result.success(MessageResponse("Garantía firmada correctamente."))
        } else {
            result
        }
    }
    suspend fun update(id: Int, request: UpdateWarrantyRequest) = safeApiCall { api.updateWarranty(id, request) }
    suspend fun revoke(id: Int) = safeApiCall { api.revokeWarranty(id) }
    suspend fun resetSignature(id: Int) = safeApiCall { api.resetSignature(id) }
    suspend fun delete(id: Int) = safeApiCall { api.deleteWarranty(id) }
}
