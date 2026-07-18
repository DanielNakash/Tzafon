package com.thefoxworks.tzafon.data.transfer

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.MalformedJsonException

/**
 * FR-DATA-2 — parse + validate a `DM-EXPORT-1` document, then hand the
 * validated document to a [DataTransferRepository] for the atomic apply.
 *
 * Rejection is typed so the UI can render precise messages
 * (`FR-DATA-2.3/2.4/2.5/2.6`). No partial write ever happens: the caller
 * only invokes [DataTransferRepository.importAll] after validation succeeds.
 */
class DataImporter(
    private val transfer: DataTransferRepository,
    private val supportedFormatVersion: Int = EXPORT_FORMAT_VERSION,
    private val gson: Gson = Gson(),
) {

    /** Result of parsing + validating (`parse`), before any write. */
    sealed class ParseResult {
        data class Ok(
            val document: ExportDocument,
            /** True iff any collection's actual size differs from the manifest counts. */
            val countMismatch: Boolean,
        ) : ParseResult()

        sealed class Rejected(val message: String) : ParseResult() {
            /** Not JSON, or the top-level shape is wrong (missing manifest/collections). */
            class Malformed(message: String = "That doesn't look like a Tzafon backup.") : Rejected(message)

            /** manifest.format != "tzafon.export". */
            class WrongFormat : Rejected("That doesn't look like a Tzafon backup.")

            /** manifest.formatVersion > supportedFormatVersion. */
            class NewerVersion : Rejected("This backup is from a newer version of Tzafon.")

            /** A row couldn't be deserialized to its domain type. */
            class BadContent(reason: String) : Rejected("This backup is unreadable — $reason")
        }
    }

    /** Parse + validate — pure, no side effects. */
    fun parse(json: String): ParseResult {
        val root = try {
            gson.fromJson(json, com.google.gson.JsonObject::class.java)
                ?: return ParseResult.Rejected.Malformed()
        } catch (_: JsonSyntaxException) {
            return ParseResult.Rejected.Malformed()
        } catch (_: MalformedJsonException) {
            return ParseResult.Rejected.Malformed()
        } catch (_: IllegalStateException) {
            return ParseResult.Rejected.Malformed()
        }

        val manifestEl = root.get("manifest") ?: return ParseResult.Rejected.Malformed()
        val collectionsEl = root.get("collections") ?: return ParseResult.Rejected.Malformed()
        if (!manifestEl.isJsonObject || !collectionsEl.isJsonObject) {
            return ParseResult.Rejected.Malformed()
        }

        val manifest = try {
            gson.fromJson(manifestEl, ExportManifest::class.java)
        } catch (_: JsonSyntaxException) {
            return ParseResult.Rejected.Malformed()
        } ?: return ParseResult.Rejected.Malformed()

        if (manifest.format != EXPORT_FORMAT) return ParseResult.Rejected.WrongFormat()
        if (manifest.formatVersion > supportedFormatVersion) return ParseResult.Rejected.NewerVersion()

        val document = try {
            gson.fromJson(root, ExportDocument::class.java)
        } catch (e: JsonSyntaxException) {
            return ParseResult.Rejected.BadContent(e.message ?: "invalid value")
        } catch (e: IllegalArgumentException) {
            return ParseResult.Rejected.BadContent(e.message ?: "invalid value")
        }

        val c = document.collections
        val counts = manifest.counts
        val mismatch = c.tasks.size != counts.tasks ||
            c.series.size != counts.series ||
            c.habits.size != counts.habits ||
            c.habitLogs.size != counts.habitLogs ||
            c.goals.size != counts.goals ||
            c.themes.size != counts.themes ||
            c.reviews.size != counts.reviews ||
            c.contributions.size != counts.contributions

        return ParseResult.Ok(document = document, countMismatch = mismatch)
    }

    /** True iff the local store is empty (`FR-DATA-2.8`). */
    suspend fun isStoreEmpty(): Boolean = transfer.isEmpty()

    /** Apply a validated document atomically under the chosen policy. */
    suspend fun apply(document: ExportDocument, policy: ConflictPolicy): ImportStats =
        transfer.importAll(document, policy)
}
