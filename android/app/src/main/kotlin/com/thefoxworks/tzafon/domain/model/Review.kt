package com.thefoxworks.tzafon.domain.model

import kotlinx.coroutines.flow.Flow

enum class ReviewKind { WEEKLY, MONTHLY }

/** DM-REVIEW-4 — dismissible, resumable; skipped is never a failure. */
enum class ReviewStatus { PENDING, PARTIAL, DONE, SKIPPED }

/**
 * DM-REVIEW-5 — the persisted per-period artifact. The id IS the period
 * start, so a week can never hold two reviews (DM-REVIEW-1).
 */
data class Review(
    val id: String,                    // == periodStart (ISO week-start date)
    val kind: ReviewKind = ReviewKind.WEEKLY,
    val periodStart: String,
    val periodEnd: String,
    val status: ReviewStatus = ReviewStatus.PENDING,
    val reflectNote: String? = null,
    /** the mirror numbers, computed once at completion: done·aligned·rates */
    val snapshot: String = "",
    val completedAt: Long? = null,
)

interface ReviewRepository {
    fun observeReviews(): Flow<List<Review>>
    suspend fun get(id: String): Review?
    suspend fun upsert(review: Review)
}
