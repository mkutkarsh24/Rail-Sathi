package com.railsathi.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.railsathi.app.data.local.database.RailSathiDatabase
import com.railsathi.app.data.local.entity.CachedTrainStatusEntity
import com.railsathi.app.data.local.entity.SavedTrainEntity
import com.railsathi.app.data.local.entity.TrainSearchHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real Room DAOs against an in-memory SQLite database on a device/emulator. */
@RunWith(AndroidJUnit4::class)
class RoomDatabaseTest {

    private lateinit var db: RailSathiDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RailSathiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // ---- favourites ----

    @Test
    fun favourites_insertObserveAndDelete() = runBlocking {
        val dao = db.savedTrainDao()
        assertTrue(dao.observeAll().first().isEmpty())

        dao.upsert(SavedTrainEntity("12951", "Mumbai Rajdhani", savedAt = 100L))
        dao.upsert(SavedTrainEntity("12301", "Howrah Rajdhani", savedAt = 200L))

        // Newest first.
        assertEquals(listOf("12301", "12951"), dao.observeAll().first().map { it.trainNumber })
        assertTrue(dao.observeIsSaved("12951").first())

        dao.delete("12951")
        assertFalse(dao.observeIsSaved("12951").first())
        assertEquals(listOf("12301"), dao.observeAll().first().map { it.trainNumber })
    }

    @Test
    fun favourites_savingTheSameTrainTwice_keepsOneRow() = runBlocking {
        val dao = db.savedTrainDao()
        dao.upsert(SavedTrainEntity("12951", "Old name", savedAt = 1L))
        dao.upsert(SavedTrainEntity("12951", "New name", savedAt = 2L))

        val rows = dao.observeAll().first()
        assertEquals(1, rows.size)
        assertEquals("New name", rows.single().trainName)
    }

    // ---- search history ----

    @Test
    fun history_isNewestFirst_andSearchingAgainRefreshesTheTimestampInsteadOfDuplicating() = runBlocking {
        val dao = db.searchHistoryDao()
        dao.upsert(TrainSearchHistoryEntity("12951", "A", searchedAt = 1L))
        dao.upsert(TrainSearchHistoryEntity("12301", "B", searchedAt = 2L))
        dao.upsert(TrainSearchHistoryEntity("12951", "A", searchedAt = 3L))

        val rows = dao.observeRecent(10).first()
        assertEquals(listOf("12951", "12301"), rows.map { it.trainNumber })
        assertEquals(3L, rows.first().searchedAt)
    }

    @Test
    fun history_limitTrimAndClear() = runBlocking {
        val dao = db.searchHistoryDao()
        (1..6).forEach { i ->
            dao.upsert(TrainSearchHistoryEntity("1000$i", "Train $i", searchedAt = i.toLong()))
        }
        assertEquals(3, dao.observeRecent(3).first().size)

        dao.trimTo(2)
        assertEquals(listOf("10006", "10005"), dao.observeRecent(10).first().map { it.trainNumber })

        dao.delete("10006")
        assertEquals(listOf("10005"), dao.observeRecent(10).first().map { it.trainNumber })

        dao.clear()
        assertTrue(dao.observeRecent(10).first().isEmpty())
    }

    // ---- cached status ----

    @Test
    fun cachedStatus_roundTripsAndReplaces() = runBlocking {
        val dao = db.cachedTrainStatusDao()
        assertNull(dao.get("12951"))

        dao.upsert(cached("12951", refreshedAt = 10L, json = "{\"v\":1}"))
        dao.upsert(cached("12951", refreshedAt = 20L, json = "{\"v\":2}"))

        val row = dao.get("12951")
        assertNotNull(row)
        assertEquals(20L, row!!.lastSuccessfulRefresh)
        assertEquals("{\"v\":2}", row.payloadJson)
    }

    @Test
    fun cachedStatus_refreshTimesAreObservable_andOldRowsCanBePurged() = runBlocking {
        val dao = db.cachedTrainStatusDao()
        dao.upsert(cached("12951", refreshedAt = 10L))
        dao.upsert(cached("12301", refreshedAt = 50L))

        val times = dao.observeRefreshTimes().first().associate { it.trainNumber to it.lastSuccessfulRefresh }
        assertEquals(mapOf("12951" to 10L, "12301" to 50L), times)

        dao.deleteOlderThan(30L)
        assertNull(dao.get("12951"))
        assertNotNull(dao.get("12301"))

        dao.clear()
        assertNull(dao.get("12301"))
    }

    private fun cached(number: String, refreshedAt: Long, json: String = "{}") = CachedTrainStatusEntity(
        trainNumber = number,
        trainName = "Train $number",
        journeyDate = "2026-10-16",
        payloadJson = json,
        lastSuccessfulRefresh = refreshedAt,
        isSample = false,
    )
}
