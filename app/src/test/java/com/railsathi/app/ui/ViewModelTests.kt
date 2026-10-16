package com.railsathi.app.ui

import com.google.gson.Gson
import com.railsathi.app.data.remote.api.ApiMode
import com.railsathi.app.data.repository.AvailabilityRepositoryImpl
import com.railsathi.app.data.repository.PnrRepositoryImpl
import com.railsathi.app.data.repository.RunningStatusRepositoryImpl
import com.railsathi.app.data.repository.TrainSearchRepositoryImpl
import com.railsathi.app.domain.model.ErrorType
import com.railsathi.app.domain.model.Train
import com.railsathi.app.domain.usecase.GetPnrStatusUseCase
import com.railsathi.app.domain.usecase.GetRunningStatusUseCase
import com.railsathi.app.domain.usecase.SearchAvailabilityUseCase
import com.railsathi.app.domain.usecase.SearchTrainsUseCase
import com.railsathi.app.domain.usecase.ToggleFavouriteUseCase
import com.railsathi.app.testutil.FakeCachedTrainStatusDao
import com.railsathi.app.testutil.FakeRailApi
import com.railsathi.app.testutil.FakeSavedTrainsRepository
import com.railsathi.app.testutil.FixedTimeProvider
import com.railsathi.app.testutil.MainDispatcherRule
import com.railsathi.app.testutil.httpException
import com.railsathi.app.testutil.ioException
import com.railsathi.app.ui.availability.AvailabilityViewModel
import com.railsathi.app.ui.home.HomeViewModel
import com.railsathi.app.ui.pnr.PnrViewModel
import com.railsathi.app.ui.runningstatus.RunningStatusViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ViewModelTests {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private lateinit var api: FakeRailApi
    private lateinit var saved: FakeSavedTrainsRepository
    private lateinit var time: FixedTimeProvider
    private val real = ApiMode.from("https://api.example.com")

    @Before
    fun setUp() {
        api = FakeRailApi()
        saved = FakeSavedTrainsRepository()
        time = FixedTimeProvider()
    }

    private fun runningStatusVm(): RunningStatusViewModel {
        val repo = RunningStatusRepositoryImpl(api, FakeCachedTrainStatusDao(), Gson(), time, real)
        return RunningStatusViewModel(GetRunningStatusUseCase(repo, saved), saved, ToggleFavouriteUseCase(saved), time)
    }

    // ---- Running status ----

    @Test
    fun runningStatus_initialState_defaultsDateToToday() {
        val vm = runningStatusVm()
        assertEquals("2026-10-16", vm.uiState.value.dateInput)
        assertNull(vm.uiState.value.status)
    }

    @Test
    fun runningStatus_invalidInput_showsErrorsAndDoesNotCallApi() {
        val vm = runningStatusVm()
        vm.onTrainNumberChange("12")
        vm.onDateChange("yesterday")

        vm.submit()

        val state = vm.uiState.value
        assertNotNull(state.trainNumberError)
        assertNotNull(state.dateError)
        assertFalse(state.isLoading)
        assertEquals(0, api.runningStatusCalls)
    }

    @Test
    fun runningStatus_trainNumberInput_isDigitsOnlyAndCappedAtFive() {
        val vm = runningStatusVm()
        vm.onTrainNumberChange("12a9-5 1999")
        assertEquals("12951", vm.uiState.value.trainNumberInput)
    }

    @Test
    fun runningStatus_success_loadsStatus_recordsHistory() = runTest {
        val vm = runningStatusVm()
        vm.onTrainNumberChange("12951")

        vm.submit()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("Sample Rajdhani", state.status?.trainName)
        assertEquals(listOf("12951"), saved.history.value.map { it.train.number })
    }

    @Test
    fun runningStatus_failureWithNoCache_showsErrorAndNoStatus() {
        api.runningStatusResult = Result.failure(ioException())
        val vm = runningStatusVm()
        vm.onTrainNumberChange("12951")

        vm.submit()

        val state = vm.uiState.value
        assertEquals(ErrorType.NETWORK, state.error?.type)
        assertNull(state.status)
        assertTrue(saved.history.value.isEmpty()) // failed lookups are not added to history
    }

    @Test
    fun runningStatus_failureAfterSuccess_keepsSavedCopyAlongsideError() {
        val vm = runningStatusVm()
        vm.onTrainNumberChange("12951")
        vm.submit()

        api.runningStatusResult = Result.failure(ioException())
        vm.submit()

        val state = vm.uiState.value
        assertEquals(ErrorType.NETWORK, state.error?.type)
        assertNotNull(state.status)
        assertTrue(state.status!!.isFromCache)
    }

    @Test
    fun runningStatus_stalePayload_isFlagged() {
        // Provider says 10:00 IST; device "now" is far later.
        time.nowMillis = java.time.OffsetDateTime.parse("2026-10-16T12:00:00+05:30").toInstant().toEpochMilli()
        val vm = runningStatusVm()
        vm.onTrainNumberChange("12951")

        vm.submit()

        assertTrue(vm.uiState.value.isStale)
    }

    @Test
    fun runningStatus_freshPayload_isNotFlaggedStale() {
        time.nowMillis = java.time.OffsetDateTime.parse("2026-10-16T10:05:00+05:30").toInstant().toEpochMilli()
        val vm = runningStatusVm()
        vm.onTrainNumberChange("12951")

        vm.submit()

        assertFalse(vm.uiState.value.isStale)
    }

    @Test
    fun runningStatus_toggleFavourite_addsThenRemoves() = runTest {
        val vm = runningStatusVm()
        vm.onTrainNumberChange("12951")
        vm.submit()

        vm.onToggleFavourite()
        assertTrue(vm.uiState.value.isFavourite)
        assertEquals(listOf("12951"), saved.favourites.value.map { it.train.number })

        vm.onToggleFavourite()
        assertFalse(vm.uiState.value.isFavourite)
        assertTrue(saved.favourites.value.isEmpty())
    }

    @Test
    fun runningStatus_openTrain_fillsInputAndFetches() {
        val vm = runningStatusVm()
        vm.openTrain("12951")
        assertEquals("12951", vm.uiState.value.trainNumberInput)
        assertEquals("Sample Rajdhani", vm.uiState.value.status?.trainName)
    }

    // ---- Availability ----

    private fun availabilityVm() = AvailabilityViewModel(
        SearchAvailabilityUseCase(AvailabilityRepositoryImpl(api, time, real)),
        time,
    )

    @Test
    fun availability_missingFields_showFieldErrors() {
        val vm = availabilityVm()
        vm.search()
        val state = vm.uiState.value
        assertNotNull(state.fromError)
        assertNotNull(state.toError)
        assertFalse(state.hasSearched)
    }

    @Test
    fun availability_sameStation_isRejected() {
        val vm = availabilityVm()
        vm.onFromChange("ndls")
        vm.onToChange("NDLS")
        vm.search()
        assertNotNull(vm.uiState.value.toError)
    }

    @Test
    fun availability_pastDate_isRejected() {
        val vm = availabilityVm()
        vm.onFromChange("NDLS")
        vm.onToChange("BPL")
        vm.onDateChange("2026-10-01")
        vm.search()
        assertNotNull(vm.uiState.value.dateError)
    }

    @Test
    fun availability_success_showsProviderStatusText() {
        val vm = availabilityVm()
        vm.onFromChange("ndls")
        vm.onToChange("bpl")
        vm.onQuickDate(1)

        vm.search()

        val state = vm.uiState.value
        assertTrue(state.hasSearched)
        assertFalse(state.isLoading)
        assertEquals("AVAILABLE-0042", state.result?.trains?.first()?.classes?.first()?.statusText)
    }

    @Test
    fun availability_emptyResult_isDistinctFromError() {
        api.availabilityResult = Result.success(com.railsathi.app.data.remote.dto.AvailabilityResponseDto(emptyList()))
        val vm = availabilityVm()
        vm.onFromChange("NDLS")
        vm.onToChange("BPL")

        vm.search()

        val state = vm.uiState.value
        assertNull(state.error)
        assertTrue(state.result!!.trains.isEmpty())
    }

    @Test
    fun availability_networkFailure_setsError() {
        api.availabilityResult = Result.failure(ioException())
        val vm = availabilityVm()
        vm.onFromChange("NDLS")
        vm.onToChange("BPL")

        vm.search()

        assertEquals(ErrorType.NETWORK, vm.uiState.value.error?.type)
        assertNull(vm.uiState.value.result)
    }

    // ---- PNR ----

    private fun pnrVm() = PnrViewModel(GetPnrStatusUseCase(PnrRepositoryImpl(api, time, real)))

    @Test
    fun pnr_inputIsDigitsOnlyAndCappedAtTen() {
        val vm = pnrVm()
        vm.onPnrChange("12-34 56789012345")
        assertEquals("1234567890", vm.uiState.value.pnrInput)
    }

    @Test
    fun pnr_invalidPnr_showsError_andSkipsNetwork() {
        val vm = pnrVm()
        vm.onPnrChange("123")
        vm.submit()
        assertNotNull(vm.uiState.value.inputError)
        assertNull(api.lastPnrRequest)
    }

    @Test
    fun pnr_success_showsStatus_thenClearWipesIt() {
        val vm = pnrVm()
        vm.onPnrChange("1234567890")

        vm.submit()
        assertEquals("12951", vm.uiState.value.status?.trainNumber)

        vm.clear()
        assertEquals("", vm.uiState.value.pnrInput)
        assertNull(vm.uiState.value.status)
    }

    @Test
    fun pnr_rateLimit_andNotFound_surfaceDistinctErrors() {
        val vm = pnrVm()
        vm.onPnrChange("1234567890")

        api.pnrResult = Result.failure(httpException(429))
        vm.submit()
        assertEquals(ErrorType.RATE_LIMITED, vm.uiState.value.error?.type)

        api.pnrResult = Result.failure(httpException(404))
        vm.submit()
        assertEquals(ErrorType.NOT_FOUND, vm.uiState.value.error?.type)
    }

    // ---- Home ----

    private fun homeVm() = HomeViewModel(saved, SearchTrainsUseCase(TrainSearchRepositoryImpl(api)))

    /**
     * HomeViewModel.uiState uses SharingStarted.WhileSubscribed, so its value only updates while
     * something collects it (as the real UI does). Tests that read uiState.value must subscribe.
     */
    private fun TestScope.homeVmWithSubscriber(): HomeViewModel = homeVm().also { vm ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
    }

    @Test
    fun home_fiveDigitQuery_opensTheTrainDirectly() {
        val vm = homeVm()
        var opened: String? = null
        vm.onQueryChange("12951")
        vm.onSearchSubmit { opened = it }
        assertEquals("12951", opened)
    }

    @Test
    fun home_wrongLengthNumber_showsError_andDoesNotOpen() = runTest {
        val vm = homeVmWithSubscriber()
        var opened: String? = null
        vm.onQueryChange("1295")
        vm.onSearchSubmit { opened = it }
        assertNull(opened)
        assertNotNull(vm.uiState.value.search.queryError)
    }

    @Test
    fun home_emptyQuery_showsError() = runTest {
        val vm = homeVmWithSubscriber()
        vm.onSearchSubmit { }
        assertNotNull(vm.uiState.value.search.queryError)
    }

    @Test
    fun home_nameSearch_returnsResults() = runTest {
        val vm = homeVmWithSubscriber()
        vm.onQueryChange("rajdhani")
        vm.onSearchSubmit { }
        assertEquals(listOf("12951"), vm.uiState.value.search.results?.map { it.number })
    }

    @Test
    fun home_nameSearch_failureSetsError() = runTest {
        api.searchResult = Result.failure(ioException())
        val vm = homeVmWithSubscriber()
        vm.onQueryChange("rajdhani")
        vm.onSearchSubmit { }
        assertEquals(ErrorType.NETWORK, vm.uiState.value.search.error?.type)
    }

    @Test
    fun home_exposesFavouritesAndHistory_andClearHistoryWorks() = runTest {
        saved.addFavourite(Train("12951", "Sample Rajdhani"))
        saved.recordSearch(Train("12301", "Howrah Rajdhani"))
        val vm = homeVm()
        // Keep a subscriber alive for the whole test so the WhileSubscribed upstream keeps running.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }

        val local = vm.uiState.first { !it.local.isLoading }.local
        assertEquals(1, local.favourites.size)
        assertEquals(1, local.history.size)

        vm.clearHistory()
        assertTrue(vm.uiState.first { it.local.history.isEmpty() }.local.history.isEmpty())
    }
}
