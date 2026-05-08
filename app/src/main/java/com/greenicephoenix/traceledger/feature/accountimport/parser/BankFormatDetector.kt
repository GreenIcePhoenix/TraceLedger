package com.greenicephoenix.traceledger.feature.accountimport.parser

import android.content.Context
import android.net.Uri
import com.greenicephoenix.traceledger.feature.accountimport.model.BankFormat

object BankFormatDetector {

    // ── CSV bank fingerprints ────────────────────────────────────────────────
    // Checked only after we know the file is CSV (text, no magic bytes).
    private val CSV_FINGERPRINTS = listOf(
        BankFormat.HDFC_CSV     to "narration",
        BankFormat.ICICI_CSV    to "transaction date",
        BankFormat.SBI_CSV      to "txn date",
        BankFormat.AXIS_CSV     to "particulars",
        BankFormat.KOTAK_CSV    to "withdrawal (dr)",
        BankFormat.YES_CSV      to "withdrawal amt",
        BankFormat.INDUSIND_CSV to "dr/cr"
    )

    // ── File signature bytes ─────────────────────────────────────────────────
    // These 4-byte signatures identify the true file type regardless of MIME.
    private val ZIP_MAGIC  = byteArrayOf(0x50, 0x4B, 0x03, 0x04)  // XLSX (ZIP)
    private val OLE2_MAGIC = byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11.toByte(), 0xE0.toByte()) // XLS / encrypted XLSX

    fun detect(context: Context, uri: Uri): BankFormat {
        val mimeType = context.contentResolver.getType(uri)?.lowercase() ?: ""

        // PDF is reliable via MIME alone — PDFBox will validate it
        if (mimeType == "application/pdf") return BankFormat.PDF

        // For everything else, read the first 4 bytes to determine real format.
        // File managers often assign wrong MIME types to XLS/XLSX files.
        val magic = readMagicBytes(context, uri)

        return when {
            magic.startsWith(ZIP_MAGIC)  -> BankFormat.XLSX
            magic.startsWith(OLE2_MAGIC) -> BankFormat.XLS   // also catches encrypted XLSX
            mimeType == "application/pdf" || uri.path?.endsWith(".pdf", ignoreCase = true) == true -> BankFormat.PDF
            // Plaintext — try CSV fingerprinting
            else -> detectCsvFormat(context, uri)
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun readMagicBytes(context: Context, uri: Uri): ByteArray {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buf = ByteArray(4)
                stream.read(buf)
                buf
            } ?: ByteArray(0)
        } catch (e: Exception) { ByteArray(0) }
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        return prefix.indices.all { this[it] == prefix[it] }
    }

    private fun detectCsvFormat(context: Context, uri: Uri): BankFormat {
        val headerLines = readFirstLines(context, uri, 5)
        if (headerLines.isEmpty()) return BankFormat.UNKNOWN
        for (line in headerLines) {
            val n = line.normalise()
            for ((format, fingerprint) in CSV_FINGERPRINTS) {
                if (fingerprint in n) return format
            }
        }
        return if (headerLines.any { it.contains(",") }) BankFormat.GENERIC_CSV
        else BankFormat.UNKNOWN
    }

    private fun readFirstLines(context: Context, uri: Uri, maxLines: Int): List<String> {
        return try {
            context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.use { reader ->
                    (1..maxLines).mapNotNull { reader.readLine() }
                } ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private fun String.normalise() =
        lowercase().replace("\"", "").replace(Regex("\\s+"), " ").trim()
}