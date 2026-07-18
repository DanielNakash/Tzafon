package com.thefoxworks.tzafon.data.transfer

import com.thefoxworks.tzafon.domain.model.Contribution
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitLog
import com.thefoxworks.tzafon.domain.model.Review
import com.thefoxworks.tzafon.domain.model.Series
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.Theme

/**
 * DM-EXPORT-1 — the on-disk backup document. Domain-typed by design (the
 * DECISION in v2.8.0 §1: readable + lossless via the domain shapes, not the
 * flat Room rows). Serialized with a pretty-printing Gson (`DM-EXPORT-1.6`).
 */

const val EXPORT_FORMAT = "tzafon.export"
const val EXPORT_FORMAT_VERSION = 1

data class ExportManifest(
    val format: String = EXPORT_FORMAT,
    val formatVersion: Int = EXPORT_FORMAT_VERSION,
    val appVersion: String,
    val appVersionCode: Int,
    val exportedAt: Long,
    val exportedAtIso: String,
    val counts: ExportCounts,
)

data class ExportCounts(
    val tasks: Int,
    val series: Int,
    val habits: Int,
    val habitLogs: Int,
    val goals: Int,
    val themes: Int,
    val reviews: Int,
    val contributions: Int,
)

data class ExportCollections(
    val tasks: List<Task>,
    val series: List<Series>,
    val habits: List<Habit>,
    val habitLogs: List<HabitLog>,
    val goals: List<Goal>,
    val themes: List<Theme>,
    val reviews: List<Review>,
    val contributions: List<Contribution>,
)

data class ExportDocument(
    val manifest: ExportManifest,
    val collections: ExportCollections,
)
