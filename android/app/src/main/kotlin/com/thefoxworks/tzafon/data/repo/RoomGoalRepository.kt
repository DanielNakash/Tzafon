package com.thefoxworks.tzafon.data.repo

import com.thefoxworks.tzafon.data.db.GoalDao
import com.thefoxworks.tzafon.data.db.toDomain
import com.thefoxworks.tzafon.data.db.toEntity
import com.thefoxworks.tzafon.domain.attribution.Attribution
import com.thefoxworks.tzafon.domain.model.Contribution
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed GoalRepository — the DM-ATTR ledger made durable. */
class RoomGoalRepository(
    private val dao: GoalDao,
    private val now: () -> Long = { System.currentTimeMillis() },
) : GoalRepository {

    override fun observeGoals(): Flow<List<Goal>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeContributions(): Flow<List<Contribution>> =
        dao.observeContributions().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getGoal(id: String): Goal? = dao.get(id)?.toDomain()

    override suspend fun upsert(goal: Goal) = dao.upsert(goal.toEntity())

    override suspend fun delete(id: String) {
        dao.deleteContributionsForGoal(id)
        dao.delete(id)
    }

    override suspend fun advanceDirect(goalId: String, newCurrent: Double) {
        val g = dao.get(goalId)?.toDomain() ?: return
        dao.upsert(g.copy(currentQty = newCurrent, lastActivityAt = now()).toEntity())
    }

    override suspend fun applyForTask(taskId: String, contributions: List<Contribution>) {
        for (c in contributions) {
            val g = dao.get(c.goalId)?.toDomain() ?: continue
            dao.insertContribution(c.toEntity())
            dao.upsert(
                g.copy(currentQty = g.currentQty + c.amount, lastActivityAt = now()).toEntity(),
            )
        }
    }

    override suspend fun reverseForTask(taskId: String) {
        val rows = dao.contributionsFor(taskId).map { it.toDomain() }
        val deltas = Attribution.reversalDeltas(rows, taskId)
        for ((goalId, delta) in deltas) {
            val g = dao.get(goalId)?.toDomain() ?: continue
            dao.upsert(g.copy(currentQty = g.currentQty - delta).toEntity())
        }
        dao.deleteContributionsFor(taskId)
    }
}
