package com.railsathi.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A train the user marked as a favourite. */
@Entity(tableName = "saved_trains")
data class SavedTrainEntity(
    @PrimaryKey val trainNumber: String,
    val trainName: String,
    val savedAt: Long,
)

/** One row per train; searching the same train again just refreshes [searchedAt]. */
@Entity(tableName = "train_search_history")
data class TrainSearchHistoryEntity(
    @PrimaryKey val trainNumber: String,
    val trainName: String,
    val searchedAt: Long,
)

/**
 * The most recent successful running-status response for a train, kept so it can be shown offline.
 * [payloadJson] is the provider response (a RunningStatusDto) as JSON. It contains train data
 * only: no passenger or PNR information is ever cached.
 */
@Entity(tableName = "cached_train_status")
data class CachedTrainStatusEntity(
    @PrimaryKey val trainNumber: String,
    val trainName: String,
    val journeyDate: String,
    val payloadJson: String,
    val lastSuccessfulRefresh: Long,
    val isSample: Boolean,
)
