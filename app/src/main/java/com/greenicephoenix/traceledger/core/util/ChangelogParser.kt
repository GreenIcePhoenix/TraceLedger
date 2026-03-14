package com.greenicephoenix.traceledger.core.util

import android.content.Context
import com.greenicephoenix.traceledger.R

/**
 * A single feature entry within a version's changelog.
 *
 * Parsed from lines formatted as:
 *   [IconName] Feature Title :: Description text
 *
 * Example:
 *   [AccountBalance] Accounts :: Bank and Cash accounts with automatic balance tracking.
 *
 * The iconName maps to a Material Icon in ChangelogIconMapper.
 * Falls back to plain text rendering if the line doesn't match the format.
 */
data class ChangelogEntry(
    val iconName: String,   // Material icon name string, e.g. "AccountBalance"
    val title: String,      // Short feature name, e.g. "Accounts"
    val description: String // One-line description
)

/**
 * A full version block containing its version string and list of entries.
 */
data class VersionChangelog(
    val version: String,
    val entries: List<ChangelogEntry>,
    val rawText: String     // kept for fallback if entries is empty
)

object ChangelogParser {

    // Regex: [IconName] Title :: Description
    private val entryRegex = Regex("""^\[(\w+)]\s+(.+?)\s*::\s*(.+)$""")

    /**
     * Load and parse changelog from res/raw/changelog.txt.
     * Returns a list of VersionChangelog objects sorted newest-first.
     */
    fun loadVersioned(context: Context): List<VersionChangelog> {
        return try {
            val fullText = context.resources
                .openRawResource(R.raw.changelog)
                .bufferedReader()
                .use { it.readText() }

            fullText.split("# ")
                .filter { it.isNotBlank() }
                .map { section ->
                    val lines   = section.lines().filter { it.isNotBlank() }
                    val version = lines.first().trim()
                    val body    = lines.drop(1)

                    val entries = body.mapNotNull { line ->
                        val match = entryRegex.find(line.trim())
                        if (match != null) {
                            ChangelogEntry(
                                iconName    = match.groupValues[1],
                                title       = match.groupValues[2],
                                description = match.groupValues[3]
                            )
                        } else null
                    }

                    VersionChangelog(
                        version = version,
                        entries = entries,
                        rawText = body.joinToString("\n")
                    )
                }
                .sortedByDescending { it.version }

        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Legacy flat map — kept so existing call sites don't break.
     */
    fun load(context: Context): Map<String, String> {
        return loadVersioned(context).associate { it.version to it.rawText }
    }
}