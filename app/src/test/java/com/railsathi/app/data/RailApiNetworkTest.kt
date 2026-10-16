package com.railsathi.app.data

import com.google.gson.Gson
import com.railsathi.app.data.remote.api.ApiMode
import com.railsathi.app.data.remote.api.RailApi
import com.railsathi.app.data.remote.api.RetryInterceptor
import com.railsathi.app.data.remote.dto.PnrRequestDto
import com.railsathi.app.data.repository.apiCall
import com.railsathi.app.data.repository.toAppError
import com.railsathi.app.domain.model.ErrorType
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Exercises the real Retrofit + OkHttp stack against a local MockWebServer (no network needed). */
class RailApiNetworkTest {

    private lateinit var server: MockWebServer
    private lateinit var api: RailApi
    private val sleeps = mutableListOf<Long>()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .addInterceptor(RetryInterceptor(maxRetries = 2, backoffMillis = 10L, sleeper = { sleeps += it }))
            .build()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
            .create(RailApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun json(body: String, code: Int = 200) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private fun resource(name: String): String =
        javaClass.classLoader!!.getResourceAsStream("mock/$name")!!.bufferedReader().use { it.readText() }

    @Test
    fun runningStatus_parsesSuccessfulResponse() = runTest {
        server.enqueue(json(resource("running_status.json")))

        val dto = api.getRunningStatus("12951", "2026-10-16")

        assertEquals("12951", dto.trainNumber)
        assertEquals(3, dto.stops?.size)
        val request = server.takeRequest()
        assertEquals("/v1/trains/12951/running-status?date=2026-10-16", request.path)
    }

    @Test
    fun availability_sendsOptionalParametersOnlyWhenSet() = runTest {
        server.enqueue(json(resource("availability.json")))
        api.getAvailability("NDLS", "BPL", "2026-10-17", travelClass = null, quota = "GN")

        val path = server.takeRequest().path!!
        assertTrue(path.contains("from=NDLS"))
        assertTrue(path.contains("quota=GN"))
        assertFalse(path.contains("class="))
    }

    @Test
    fun pnr_isSentInBodyNotInUrl() = runTest {
        server.enqueue(json(resource("pnr.json")))
        api.getPnrStatus(PnrRequestDto("1234567890"))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/pnr/status", request.path)
        assertFalse(request.path!!.contains("1234567890"))
        assertTrue(request.body.readUtf8().contains("1234567890"))
    }

    @Test
    fun http404_mapsToNotFound() = runTest {
        server.enqueue(json("{}", 404))
        val error = apiCall { api.getRunningStatus("12951", "2026-10-16") }.exceptionOrNull()!!.toAppError()
        assertEquals(ErrorType.NOT_FOUND, error.type)
    }

    @Test
    fun http429_mapsToRateLimited_andIsNotRetried() = runTest {
        server.enqueue(json("{}", 429))
        val error = apiCall { api.getRunningStatus("12951", "2026-10-16") }.exceptionOrNull()!!.toAppError()
        assertEquals(ErrorType.RATE_LIMITED, error.type)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun http401_mapsToUnauthorized() = runTest {
        server.enqueue(json("{}", 401))
        val error = apiCall { api.searchTrains("raj") }.exceptionOrNull()!!.toAppError()
        assertEquals(ErrorType.UNAUTHORIZED, error.type)
    }

    @Test
    fun transient503_isRetriedThenSucceeds() = runTest {
        server.enqueue(json("{}", 503))
        server.enqueue(json("{}", 503))
        server.enqueue(json(resource("running_status.json")))

        val dto = api.getRunningStatus("12951", "2026-10-16")

        assertEquals("12951", dto.trainNumber)
        assertEquals(3, server.requestCount)
        assertEquals(listOf(10L, 20L), sleeps) // linear back-off
    }

    @Test
    fun persistent503_givesUpAfterRetries_andMapsToServerError() = runTest {
        repeat(3) { server.enqueue(json("{}", 503)) }
        val error = apiCall { api.getRunningStatus("12951", "2026-10-16") }.exceptionOrNull()!!.toAppError()
        assertEquals(ErrorType.SERVER, error.type)
        assertEquals(3, server.requestCount) // 1 attempt + 2 retries
    }

    @Test
    fun postRequests_areNeverRetried() = runTest {
        server.enqueue(json("{}", 503))
        server.enqueue(json(resource("pnr.json")))

        val error = apiCall { api.getPnrStatus(PnrRequestDto("1234567890")) }.exceptionOrNull()!!.toAppError()

        assertEquals(ErrorType.SERVER, error.type)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun malformedBody_mapsToParseError() = runTest {
        server.enqueue(json("{ definitely not json"))
        val error = apiCall { api.getRunningStatus("12951", "2026-10-16") }.exceptionOrNull()!!.toAppError()
        assertEquals(ErrorType.PARSE, error.type)
    }

    @Test
    fun unreachableServer_mapsToNetworkError() = runTest {
        server.shutdown()
        val error = apiCall { api.getRunningStatus("12951", "2026-10-16") }.exceptionOrNull()!!.toAppError()
        assertEquals(ErrorType.NETWORK, error.type)
    }

    @Test
    fun apiMode_onlyTrustsHttpsUrls() {
        assertTrue(ApiMode.from("").isMock)
        assertTrue(ApiMode.from("http://insecure.example.com").isMock)
        assertTrue(ApiMode.from("https://").isMock)
        val real = ApiMode.from("https://api.example.com/rail")
        assertFalse(real.isMock)
        assertEquals("https://api.example.com/rail/", real.baseUrl)
    }
}
