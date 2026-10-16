package com.railsathi.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.railsathi.app.domain.model.AppError
import com.railsathi.app.domain.model.AvailabilityKind
import com.railsathi.app.domain.model.AvailabilityResult
import com.railsathi.app.domain.model.ClassAvailability
import com.railsathi.app.domain.model.ErrorType
import com.railsathi.app.domain.model.PassengerStatus
import com.railsathi.app.domain.model.PnrStatus
import com.railsathi.app.domain.model.RunningStatus
import com.railsathi.app.domain.model.SavedTrain
import com.railsathi.app.domain.model.SearchHistoryItem
import com.railsathi.app.domain.model.StationStop
import com.railsathi.app.domain.model.StopState
import com.railsathi.app.domain.model.Train
import com.railsathi.app.domain.model.TrainAvailability
import com.railsathi.app.ui.availability.AvailabilityScreen
import com.railsathi.app.ui.availability.AvailabilityUiState
import com.railsathi.app.ui.home.HomeScreen
import com.railsathi.app.ui.home.HomeSearchState
import com.railsathi.app.ui.home.HomeUiState
import com.railsathi.app.ui.home.LocalHomeState
import com.railsathi.app.ui.pnr.PnrScreen
import com.railsathi.app.ui.pnr.PnrUiState
import com.railsathi.app.ui.runningstatus.RunningStatusScreen
import com.railsathi.app.ui.runningstatus.RunningStatusUiState
import com.railsathi.app.ui.theme.RailSathiTheme
import java.time.LocalDate
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screen-level Compose tests. They render the stateless screen composables with hand-built UI
 * state, so they need no ViewModel, database or network.
 */
@RunWith(AndroidJUnit4::class)
class ScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val ist = "+05:30"
    private fun at(time: String): OffsetDateTime = OffsetDateTime.parse("2026-10-16T$time$ist")

    // ---- Home ----

    private fun home(
        state: HomeUiState,
        onOpenTrain: (String) -> Unit = {},
        onSearch: () -> Unit = {},
        onRetryLoadSaved: () -> Unit = {},
    ) {
        composeRule.setContent {
            RailSathiTheme {
                HomeScreen(
                    state = state,
                    onQueryChange = {},
                    onSearch = onSearch,
                    onOpenTrain = onOpenTrain,
                    onOpenStatusShortcut = {},
                    onOpenAvailability = {},
                    onOpenPnr = {},
                    onOpenPrivacy = {},
                    onRemoveFavourite = {},
                    onClearHistory = {},
                    onRetryLoadSaved = onRetryLoadSaved,
                )
            }
        }
    }

    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }

    @Test
    fun home_showsBrandSearchAndShortcuts() {
        home(HomeUiState(local = LocalHomeState(isLoading = false)))

        composeRule.onNodeWithText("RailSathi").assertIsDisplayed()
        composeRule.onNodeWithText("Train number or name").assertIsDisplayed()
        composeRule.onNodeWithText("Live status").assertIsDisplayed()
        composeRule.onNodeWithText("Seat availability").assertIsDisplayed()
        composeRule.onNodeWithText("PNR status").assertIsDisplayed()
    }

    @Test
    fun home_emptyStates_whenNothingIsSaved() {
        home(HomeUiState(local = LocalHomeState(isLoading = false)))

        scrollTo("No favourites yet")
        composeRule.onNodeWithText("No favourites yet").assertIsDisplayed()
        scrollTo("No recent searches")
        composeRule.onNodeWithText("No recent searches").assertIsDisplayed()
    }

    @Test
    fun home_errorState_offersRetry() {
        var retried = false
        home(
            HomeUiState(local = LocalHomeState(isLoading = false, hasError = true)),
            onRetryLoadSaved = { retried = true },
        )

        scrollTo("Couldn't load your saved trains.")
        composeRule.onNodeWithText("Retry").performClick()
        assertTrue(retried)
    }

    @Test
    fun home_loadingState_isShownWhileSavedDataLoads() {
        home(HomeUiState(local = LocalHomeState(isLoading = true)))

        scrollTo("Loading saved trains…")
        composeRule.onNodeWithText("Loading saved trains…").assertIsDisplayed()
    }

    @Test
    fun home_favouritesAndHistory_areListed_andTappingOpensTheTrain() {
        var opened: String? = null
        home(
            HomeUiState(
                local = LocalHomeState(
                    isLoading = false,
                    favourites = listOf(SavedTrain(Train("12951", "Mumbai Rajdhani"), 1L)),
                    history = listOf(SearchHistoryItem(Train("12301", "Howrah Rajdhani"), 2L)),
                ),
            ),
            onOpenTrain = { opened = it },
        )

        scrollTo("Mumbai Rajdhani")
        composeRule.onNodeWithText("Mumbai Rajdhani").performClick()
        assertEquals("12951", opened)

        scrollTo("Howrah Rajdhani")
        composeRule.onNodeWithText("Howrah Rajdhani").performClick()
        assertEquals("12301", opened)
    }

    @Test
    fun home_searchError_isShownWithRetry() {
        var searched = false
        home(
            HomeUiState(
                local = LocalHomeState(isLoading = false),
                search = HomeSearchState(query = "raj", error = AppError.of(ErrorType.NETWORK)),
            ),
            onSearch = { searched = true },
        )

        composeRule.onNodeWithText(AppError.defaultMessage(ErrorType.NETWORK)).assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        assertTrue(searched)
    }

    // ---- Running status ----

    private fun sampleStatus(
        isFromCache: Boolean = false,
        isSample: Boolean = false,
        providerUpdatedAt: OffsetDateTime? = at("10:00:00"),
    ) = RunningStatus(
        trainNumber = "12951",
        trainName = "Mumbai Rajdhani",
        journeyDate = LocalDate.of(2026, 10, 16),
        statusText = "Running",
        lastReportedStation = "Agra Cantt",
        lastReportedAt = at("09:55:00"),
        delayMinutes = 12,
        expectedDestinationArrival = at("16:42:00"),
        stops = listOf(
            StationStop("NDLS", "New Delhi", null, null, at("06:05:00"), at("06:05:00"), 0, StopState.PASSED),
            StationStop("AGC", "Agra Cantt", at("08:00:00"), at("08:12:00"), at("08:05:00"), at("08:17:00"), 12, StopState.CURRENT),
            StationStop("BPL", "Bhopal Jn", at("16:30:00"), at("16:42:00"), null, null, 12, StopState.UPCOMING),
        ),
        providerUpdatedAt = providerUpdatedAt,
        fetchedAtMillis = 1_760_000_000_000L,
        isFromCache = isFromCache,
        isSample = isSample,
    )

    private fun runningStatus(state: RunningStatusUiState, onSubmit: () -> Unit = {}) {
        composeRule.setContent {
            RailSathiTheme {
                RunningStatusScreen(
                    state = state,
                    onTrainNumberChange = {},
                    onDateChange = {},
                    onQuickDate = {},
                    onSubmit = onSubmit,
                    onToggleFavourite = {},
                )
            }
        }
    }

    @Test
    fun runningStatus_idle_showsPromptAndInputs() {
        runningStatus(RunningStatusUiState(dateInput = "2026-10-16"))

        composeRule.onNodeWithText("Live running status").assertIsDisplayed()
        composeRule.onNodeWithText("Train number").assertIsDisplayed()
        composeRule.onNodeWithText("Get status").assertIsDisplayed()
        scrollTo("Where is my train?")
    }

    @Test
    fun runningStatus_showsTheProvidersFields_andStopSections() {
        runningStatus(RunningStatusUiState(trainNumberInput = "12951", dateInput = "2026-10-16", status = sampleStatus()))

        scrollTo("Mumbai Rajdhani")
        composeRule.onNodeWithText("Mumbai Rajdhani").assertIsDisplayed()
        scrollTo("12 min late")
        composeRule.onNodeWithText("12 min late").assertIsDisplayed()
        scrollTo("Previous stations")
        scrollTo("Current station")
        scrollTo("Upcoming stations")
    }

    @Test
    fun runningStatus_cachedData_isMarkedAsSavedWithItsTimestamp() {
        runningStatus(
            RunningStatusUiState(
                dateInput = "2026-10-16",
                status = sampleStatus(isFromCache = true),
                error = AppError.of(ErrorType.NETWORK),
            ),
        )

        composeRule.onNode(hasText("Showing saved data from", substring = true)).assertIsDisplayed()
    }

    @Test
    fun runningStatus_staleData_showsWarning() {
        runningStatus(RunningStatusUiState(dateInput = "2026-10-16", status = sampleStatus(), isStale = true))

        composeRule.onNode(hasText("This update may be out of date", substring = true)).assertIsDisplayed()
    }

    @Test
    fun runningStatus_sampleData_isLabelledAsSample() {
        runningStatus(RunningStatusUiState(dateInput = "2026-10-16", status = sampleStatus(isSample = true)))

        composeRule.onNodeWithText("SAMPLE DATA: not a real train position.").assertIsDisplayed()
    }

    @Test
    fun runningStatus_errorWithNothingCached_showsMessageAndRetry() {
        var submitted = 0
        runningStatus(
            RunningStatusUiState(dateInput = "2026-10-16", error = AppError.of(ErrorType.RATE_LIMITED)),
            onSubmit = { submitted++ },
        )

        scrollTo(AppError.defaultMessage(ErrorType.RATE_LIMITED))
        composeRule.onNodeWithText("Retry").performClick()
        assertEquals(1, submitted)
    }

    @Test
    fun runningStatus_validationErrors_areShownOnTheFields() {
        runningStatus(
            RunningStatusUiState(
                trainNumberInput = "12",
                dateInput = "nope",
                trainNumberError = "Enter a valid 5-digit train number.",
                dateError = "Enter the date as YYYY-MM-DD.",
            ),
        )

        composeRule.onNodeWithText("Enter a valid 5-digit train number.").assertIsDisplayed()
        composeRule.onNodeWithText("Enter the date as YYYY-MM-DD.").assertIsDisplayed()
    }

    // ---- PNR ----

    @Test
    fun pnr_validationMessage_andClearButton() {
        var cleared = false
        composeRule.setContent {
            RailSathiTheme {
                PnrScreen(
                    state = PnrUiState(pnrInput = "123", inputError = "Enter a valid 10-digit PNR number."),
                    onPnrChange = {},
                    onSubmit = {},
                    onClear = { cleared = true },
                )
            }
        }

        composeRule.onNodeWithText("Enter a valid 10-digit PNR number.").assertIsDisplayed()
        composeRule.onNodeWithText("Clear").performClick()
        assertTrue(cleared)
    }

    @Test
    fun pnr_result_showsBookingAndPassengerDetails_andNeverShowsThePnrBackToBeStored() {
        val status = PnrStatus(
            trainNumber = "12951",
            trainName = "Mumbai Rajdhani",
            journeyDate = LocalDate.of(2026, 10, 20),
            boardingStation = "NDLS",
            destinationStation = "BPL",
            travelClass = "3A",
            chartPrepared = false,
            passengers = listOf(PassengerStatus(1, "CNF/B2/21", "CNF", "B2", "21")),
            providerUpdatedAt = at("10:00:00"),
            fetchedAtMillis = 1L,
            isSample = true,
        )
        composeRule.setContent {
            RailSathiTheme {
                PnrScreen(
                    state = PnrUiState(pnrInput = "1234567890", status = status),
                    onPnrChange = {},
                    onSubmit = {},
                    onClear = {},
                )
            }
        }

        scrollTo("SAMPLE DATA: this is not a real booking.")
        scrollTo("Chart not prepared yet")
        scrollTo("Passenger 1")
        scrollTo("CNF/B2/21")
    }

    @Test
    fun pnr_error_showsMessageWithRetry() {
        var retried = false
        composeRule.setContent {
            RailSathiTheme {
                PnrScreen(
                    state = PnrUiState(pnrInput = "1234567890", error = AppError.of(ErrorType.NOT_FOUND)),
                    onPnrChange = {},
                    onSubmit = { retried = true },
                    onClear = {},
                )
            }
        }

        scrollTo(AppError.defaultMessage(ErrorType.NOT_FOUND))
        composeRule.onNodeWithText("Retry").performClick()
        assertTrue(retried)
    }

    // ---- Availability ----

    @Test
    fun availability_showsProviderStatusTextVerbatim() {
        val result = AvailabilityResult(
            trains = listOf(
                TrainAvailability(
                    train = Train("12951", "Mumbai Rajdhani"),
                    fromStationCode = "NDLS",
                    toStationCode = "BPL",
                    departure = at("16:35:00"),
                    arrival = at("02:35:00"),
                    durationMinutes = 600,
                    classes = listOf(
                        ClassAvailability("SL", "AVAILABLE-0042", AvailabilityKind.AVAILABLE, 740),
                        ClassAvailability("3A", "GNWL 14/WL 9", AvailabilityKind.WAITLIST, null),
                    ),
                ),
            ),
            providerUpdatedAt = at("10:00:00"),
            fetchedAtMillis = 1L,
            isSample = false,
        )
        composeRule.setContent {
            RailSathiTheme {
                AvailabilityScreen(
                    state = AvailabilityUiState(
                        fromInput = "NDLS",
                        toInput = "BPL",
                        dateInput = "2026-10-17",
                        result = result,
                        hasSearched = true,
                    ),
                    onFromChange = {},
                    onToChange = {},
                    onDateChange = {},
                    onQuickDate = {},
                    onClassSelected = {},
                    onQuotaSelected = {},
                    onSearch = {},
                )
            }
        }

        scrollTo("AVAILABLE-0042")
        composeRule.onNodeWithText("AVAILABLE-0042").assertIsDisplayed()
        scrollTo("GNWL 14/WL 9")
        composeRule.onNodeWithText("GNWL 14/WL 9").assertIsDisplayed()
        scrollTo("Fare n/a")
    }

    @Test
    fun availability_emptyResult_saysNoTrainsFound_ratherThanInventingAny() {
        val empty = AvailabilityResult(emptyList(), null, 1L, isSample = false)
        composeRule.setContent {
            RailSathiTheme {
                AvailabilityScreen(
                    state = AvailabilityUiState(dateInput = "2026-10-17", result = empty, hasSearched = true),
                    onFromChange = {},
                    onToChange = {},
                    onDateChange = {},
                    onQuickDate = {},
                    onClassSelected = {},
                    onQuotaSelected = {},
                    onSearch = {},
                )
            }
        }

        scrollTo("No trains found")
    }

    @Test
    fun availability_validationErrors_areShown() {
        composeRule.setContent {
            RailSathiTheme {
                AvailabilityScreen(
                    state = AvailabilityUiState(
                        fromError = "Enter the source station code.",
                        toError = "Enter the destination station code.",
                    ),
                    onFromChange = {},
                    onToChange = {},
                    onDateChange = {},
                    onQuickDate = {},
                    onClassSelected = {},
                    onQuotaSelected = {},
                    onSearch = {},
                )
            }
        }

        composeRule.onNodeWithText("Enter the source station code.").assertIsDisplayed()
        composeRule.onNodeWithText("Enter the destination station code.").assertIsDisplayed()
    }
}
