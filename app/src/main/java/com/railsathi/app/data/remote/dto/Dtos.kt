package com.railsathi.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/*
 * Wire format of the RailSathi data-service contract described in docs/PROVIDER_SETUP.md.
 *
 * This is the contract the app expects from the backend YOU connect. It is NOT the schema of any
 * specific third-party provider. Every field is nullable because the app must cope with partial
 * data and must never invent values for missing fields. Date-times are ISO-8601 with offset,
 * e.g. 2026-10-16T14:30:00+05:30; dates are YYYY-MM-DD.
 */

data class RunningStatusDto(
    @SerializedName("train_number") val trainNumber: String? = null,
    @SerializedName("train_name") val trainName: String? = null,
    @SerializedName("journey_date") val journeyDate: String? = null,
    @SerializedName("status_text") val statusText: String? = null,
    @SerializedName("last_reported_station") val lastReportedStation: String? = null,
    @SerializedName("last_reported_at") val lastReportedAt: String? = null,
    @SerializedName("delay_minutes") val delayMinutes: Int? = null,
    @SerializedName("expected_destination_arrival") val expectedDestinationArrival: String? = null,
    @SerializedName("stops") val stops: List<StopDto>? = null,
    @SerializedName("last_updated_at") val lastUpdatedAt: String? = null,
)

data class StopDto(
    @SerializedName("station_code") val stationCode: String? = null,
    @SerializedName("station_name") val stationName: String? = null,
    @SerializedName("scheduled_arrival") val scheduledArrival: String? = null,
    @SerializedName("estimated_arrival") val estimatedArrival: String? = null,
    @SerializedName("scheduled_departure") val scheduledDeparture: String? = null,
    @SerializedName("estimated_departure") val estimatedDeparture: String? = null,
    @SerializedName("delay_minutes") val delayMinutes: Int? = null,
    /** PASSED, CURRENT or UPCOMING. */
    @SerializedName("state") val state: String? = null,
)

data class AvailabilityResponseDto(
    @SerializedName("trains") val trains: List<TrainAvailabilityDto>? = null,
    @SerializedName("last_updated_at") val lastUpdatedAt: String? = null,
)

data class TrainAvailabilityDto(
    @SerializedName("train_number") val trainNumber: String? = null,
    @SerializedName("train_name") val trainName: String? = null,
    @SerializedName("from_station_code") val fromStationCode: String? = null,
    @SerializedName("to_station_code") val toStationCode: String? = null,
    @SerializedName("departure") val departure: String? = null,
    @SerializedName("arrival") val arrival: String? = null,
    @SerializedName("duration_minutes") val durationMinutes: Int? = null,
    @SerializedName("classes") val classes: List<ClassAvailabilityDto>? = null,
)

data class ClassAvailabilityDto(
    @SerializedName("travel_class") val travelClass: String? = null,
    /** Provider's own wording, e.g. "AVAILABLE-0012", "GNWL 5/WL 3", "RAC 2". Shown verbatim. */
    @SerializedName("status_text") val statusText: String? = null,
    @SerializedName("fare_rupees") val fareRupees: Int? = null,
)

data class PnrRequestDto(
    @SerializedName("pnr") val pnr: String,
)

data class PnrResponseDto(
    @SerializedName("train_number") val trainNumber: String? = null,
    @SerializedName("train_name") val trainName: String? = null,
    @SerializedName("journey_date") val journeyDate: String? = null,
    @SerializedName("boarding_station") val boardingStation: String? = null,
    @SerializedName("destination_station") val destinationStation: String? = null,
    @SerializedName("travel_class") val travelClass: String? = null,
    @SerializedName("chart_prepared") val chartPrepared: Boolean? = null,
    @SerializedName("passengers") val passengers: List<PassengerDto>? = null,
    @SerializedName("last_updated_at") val lastUpdatedAt: String? = null,
)

data class PassengerDto(
    @SerializedName("serial") val serial: Int? = null,
    @SerializedName("booking_status") val bookingStatus: String? = null,
    @SerializedName("current_status") val currentStatus: String? = null,
    @SerializedName("coach") val coach: String? = null,
    @SerializedName("berth") val berth: String? = null,
)

data class TrainSearchResponseDto(
    @SerializedName("trains") val trains: List<TrainDto>? = null,
)

data class TrainDto(
    @SerializedName("number") val number: String? = null,
    @SerializedName("name") val name: String? = null,
)
