package com.greenicephoenix.traceledger.core.util

import android.content.Context
import com.greenicephoenix.traceledger.R

object ChangelogParser {

    fun load(context: Context): Map<String, String> {
        return try {
            val fullText = context.resources
                .openRawResource(R.raw.changelog)
                .bufferedReader()
                .use { it.readText() }

            val sections = fullText.split("# ")
                .filter { it.isNotBlank() }

            sections.associate { section ->
                val lines = section.lines()
                val version = lines.first().trim()
                val content = lines
                    .drop(1)
                    .joinToString("\n")
                    .trim()

                version to content
            }

        } catch (e: Exception) {
            emptyMap()
        }
    }
}