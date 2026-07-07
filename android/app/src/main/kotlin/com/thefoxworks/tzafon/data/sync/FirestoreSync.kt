package com.thefoxworks.tzafon.data.sync

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * One synced collection: how to observe it locally, key it, and write it back.
 * `remove` deletes a Room row by the same doc id `idOf` produces.
 */
class SyncSpec<E : Any>(
    val name: String,
    val clazz: Class<E>,
    val observe: () -> Flow<List<E>>,
    val idOf: (E) -> String,
    val put: suspend (E) -> Unit,
    val remove: suspend (String) -> Unit,
)

/**
 * M9b — offline-first bidirectional mirror between Room and Firestore under
 * `users/{uid}/{collection}`. Room stays the UI's source of truth; Firestore
 * (with its own offline cache) syncs in the background when signed in.
 *
 * Design (safe against data loss, eventually consistent for a personal,
 * mostly-single-device account):
 *  - **push** is the sole owner of the `pushed` hashes. On each debounced Room
 *    change it uploads rows whose content changed and deletes remote docs that
 *    are in `pushed` but no longer local (i.e. a real local delete). It never
 *    deletes a doc it hasn't itself uploaded, so a remote-only doc not yet
 *    pulled is never wiped.
 *  - **pull** applies remote changes to Room: REMOVED always deletes locally;
 *    other changes upsert, with an `incoming` hash guard to swallow echoes of
 *    our own writes. A pulled remote row simply looks like a new local row to
 *    push, which re-uploads identical content once (a no-op set) and settles.
 *
 * Conflict policy is last-writer-by-arrival — adequate for one user; genuine
 * concurrent multi-device edits of the same row are out of scope for v1.
 */
class FirestoreSync(
    private val firestore: FirebaseFirestore,
    private val specs: List<SyncSpec<*>>,
) {
    private var scope: CoroutineScope? = null

    @Synchronized
    fun start(uid: String) {
        stop()
        val s = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = s
        val user = firestore.collection("users").document(uid)
        for (spec in specs) launchSpec(s, user.collection(spec.name), spec)
    }

    @Synchronized
    fun stop() {
        scope?.cancel()
        scope = null
    }

    @OptIn(FlowPreview::class)
    private fun <E : Any> launchSpec(
        s: CoroutineScope,
        col: com.google.firebase.firestore.CollectionReference,
        spec: SyncSpec<E>,
    ) {
        val pushed = HashMap<String, Int>()   // push-owned: docId -> content hash on Firestore
        val incoming = HashMap<String, Int>() // pull-owned: docId -> last content applied to Room

        // ── pull: remote → Room (sequential; one snapshot at a time) ──
        s.launch {
            snapshots(col).collect { snap ->
                for (change in snap.documentChanges) {
                    val id = change.document.id
                    if (change.type == DocumentChange.Type.REMOVED) {
                        spec.remove(id)
                        incoming.remove(id)
                    } else {
                        val entity = FirestoreCodec.fromMap(change.document.data, spec.clazz)
                        val h = entity.hashCode()
                        if (incoming[id] != h) {
                            spec.put(entity)
                            incoming[id] = h
                        }
                    }
                }
            }
        }

        // ── push: Room → remote (debounced content diff) ──
        s.launch {
            spec.observe().debounce(1500).collect { list ->
                val local = list.associateBy { spec.idOf(it) }
                val toSet = local.filter { (id, e) -> pushed[id] != e.hashCode() }
                val toDelete = pushed.keys - local.keys
                if (toSet.isEmpty() && toDelete.isEmpty()) return@collect

                val ops: List<Pair<String, E?>> =
                    toSet.map { it.key to it.value } + toDelete.map { it to null }
                for (chunk in ops.chunked(400)) {
                    val batch = firestore.batch()
                    for ((id, entity) in chunk) {
                        if (entity != null) batch.set(col.document(id), FirestoreCodec.toMap(entity))
                        else batch.delete(col.document(id))
                    }
                    runCatching { batch.commit().await() }.onSuccess {
                        for ((id, entity) in chunk) {
                            if (entity != null) pushed[id] = entity.hashCode() else pushed.remove(id)
                        }
                    }.onFailure {
                        return@collect  // offline / error: leave hashes so we retry next emit
                    }
                }
            }
        }
    }

    private fun snapshots(col: com.google.firebase.firestore.CollectionReference): Flow<QuerySnapshot> =
        callbackFlow {
            val reg = col.addSnapshotListener { snap, err ->
                if (err == null && snap != null) trySend(snap)
            }
            awaitClose { reg.remove() }
        }
}
