package com.railsathi.app.util

import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Abstracts the clock so repositories and ViewModels are deterministic in tests. */
interface TimeProvider {
    fun now(): Long
    fun today(): LocalDate
}

/** Railway journey dates are always in Indian Standard Time, regardless of the device zone. */
@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
    override fun today(): LocalDate = LocalDate.now(IST)

    companion object {
        val IST: ZoneId = ZoneId.of("Asia/Kolkata")
    }
}
