package com.railsathi.app.data.remote.api

import com.railsathi.app.data.remote.dto.AvailabilityResponseDto
import com.railsathi.app.data.remote.dto.PnrRequestDto
import com.railsathi.app.data.remote.dto.PnrResponseDto
import com.railsathi.app.data.remote.dto.RunningStatusDto
import com.railsathi.app.data.remote.dto.TrainSearchResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The RailSathi data-service contract (docs/PROVIDER_SETUP.md). These are the endpoints the app
 * calls on the backend you configure via RAIL_API_BASE_URL. They are our own contract, not the
 * routes of any third-party provider: your backend adapts a real, licensed provider to this shape,
 * which keeps provider secrets out of the APK.
 */
interface RailApi {

    @GET("v1/trains/{number}/running-status")
    suspend fun getRunningStatus(
        @Path("number") trainNumber: String,
        @Query("date") date: String,
    ): RunningStatusDto

    @GET("v1/availability")
    suspend fun getAvailability(
        @Query("from") fromStation: String,
        @Query("to") toStation: String,
        @Query("date") date: String,
        @Query("class") travelClass: String?,
        @Query("quota") quota: String?,
    ): AvailabilityResponseDto

    /** POST so the PNR travels in the request body, not in a URL that intermediaries might log. */
    @POST("v1/pnr/status")
    suspend fun getPnrStatus(@Body request: PnrRequestDto): PnrResponseDto

    @GET("v1/trains/search")
    suspend fun searchTrains(@Query("q") query: String): TrainSearchResponseDto
}
