package com.greenicephoenix.traceledger.feature.accountimport.parser

import android.content.Context
import android.net.Uri
import com.greenicephoenix.traceledger.feature.accountimport.model.BankFormat
import com.greenicephoenix.traceledger.feature.accountimport.model.ParsedTransaction
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

sealed class PdfParseResult {
    data class Success(
        val transactions: List<ParsedTransaction>,
        val format:       BankFormat
    ) : PdfParseResult()
    data class Error(val message: String) : PdfParseResult()
    object NeedsPassword : PdfParseResult()
}

object PdfStatementParser {

    /**
     * Extract text from PDF and parse transactions using UniversalPdfParser.
     * Works for any bank worldwide — no bank-specific logic needed.
     *
     * @param password  Null on first call. If result is NeedsPassword,
     *                  prompt user and retry with entered password.
     */
    fun parse(context: Context, uri: Uri, password: String? = null): PdfParseResult {
        PDFBoxResourceLoader.init(context)

        val text = try {
            extractText(context, uri, password)
        } catch (e: Exception) {
            val isPasswordError =
                e.javaClass.simpleName.contains("InvalidPassword", ignoreCase = true) ||
                        e.javaClass.simpleName.contains("CryptographyException", ignoreCase = true) ||
                        (e.message?.contains("password",  ignoreCase = true) == true) ||
                        (e.message?.contains("encrypted", ignoreCase = true) == true) ||
                        (e.message?.contains("decrypt",   ignoreCase = true) == true)

            return if (isPasswordError) PdfParseResult.NeedsPassword
            else PdfParseResult.Error(
                "Could not read PDF: ${e.message ?: "unknown error"}.\n\n" +
                        "Make sure this is a text-based PDF. Scanned (image-only) PDFs are not supported."
            )
        }

        if (text.isBlank()) {
            return PdfParseResult.Error(
                "No text found in this PDF.\n\n" +
                        "This is likely a scanned (image-based) PDF which cannot be parsed directly.\n\n" +
                        "Try: Open the PDF → Print → Save as PDF to create a text-based copy."
            )
        }

        // UniversalPdfParser handles any bank — no per-bank logic
        val transactions = UniversalPdfParser.parse(text)

        return if (transactions.isEmpty()) {
            PdfParseResult.Error(
                "No transactions found in this PDF.\n\n" +
                        "The statement may be empty, or the format may be unusual.\n\n" +
                        "Try downloading an Excel (.xlsx) statement instead — most banks offer this."
            )
        } else {
            PdfParseResult.Success(transactions, BankFormat.PDF)
        }
    }

    private fun extractText(context: Context, uri: Uri, password: String?): String {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open file")

        return stream.use {
            val doc: PDDocument = if (!password.isNullOrEmpty())
                PDDocument.load(it, password)
            else
                PDDocument.load(it)

            doc.use { d ->
                PDFTextStripper().apply {
                    // CRITICAL: sortByPosition preserves column reading order.
                    // Without this, PDFBox reads text in byte-stream order
                    // which scrambles multi-column layouts like bank statements.
                    sortByPosition = true
                }.getText(d)
            }
        }
    }
}