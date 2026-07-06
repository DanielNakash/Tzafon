package com.thefoxworks.tzafon.data.repo

import com.thefoxworks.tzafon.data.db.ReviewDao
import com.thefoxworks.tzafon.data.db.toDomain
import com.thefoxworks.tzafon.data.db.toEntity
import com.thefoxworks.tzafon.domain.model.Review
import com.thefoxworks.tzafon.domain.model.ReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed ReviewRepository (DM-REVIEW-5 artifacts). */
class RoomReviewRepository(
    private val dao: ReviewDao,
) : ReviewRepository {

    override fun observeReviews(): Flow<List<Review>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun get(id: String): Review? = dao.get(id)?.toDomain()

    override suspend fun upsert(review: Review) = dao.upsert(review.toEntity())
}
