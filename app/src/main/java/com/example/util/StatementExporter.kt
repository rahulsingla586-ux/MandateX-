package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StatementExporter {

    fun exportPdfStatement(
        context: Context,
        transactions: List<TransactionEntity>,
        userName: String
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val pdfFile = File(exportDir, "MandateX_Statement_$dateStamp.pdf")

        val document = PdfDocument()
        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points

        val totalSpent = transactions.filter { !it.isCancelled }.sumOf { it.amount }
        val cardEmiTotal = transactions.filter { it.type == TransactionType.CARD_EMI }.sumOf { it.amount }
        val autoPayTotal = transactions.filter { it.type == TransactionType.AUTOPAY && !it.isCancelled }.sumOf { it.amount }
        val totalSaved = transactions.sumOf { it.savedAmount }

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0B2618")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#4F5B54")
            textSize = 11f
        }
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#E6F9EE")
        }
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#121212")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rowPaint = Paint().apply {
            color = Color.parseColor("#222222")
            textSize = 10f
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#DDDDDD")
            strokeWidth = 1f
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        // Header block
        canvas.drawRect(32f, 32f, pageWidth - 32f, 118f, headerBgPaint)
        canvas.drawText("MandateX — Financial & CA Statement", 48f, 64f, titlePaint)
        canvas.drawText(
            "Prepared for: ${userName.ifBlank { "Account Holder" }}  |  Generated: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())}",
            48f,
            84f,
            subtitlePaint
        )
        canvas.drawText(
            "All Expenses: ₹${totalSpent.toInt()}   |   Cards & EMIs: ₹${cardEmiTotal.toInt()}   |   AutoPay: ₹${autoPayTotal.toInt()}   |   Saved: ₹${totalSaved.toInt()}",
            48f,
            104f,
            sectionPaint.apply { textSize = 10.5f }
        )

        var y = 148f
        canvas.drawText("Itemized Ledger (${transactions.size} entries)", 36f, y, sectionPaint.apply { textSize = 13f })
        y += 18f
        canvas.drawLine(36f, y, pageWidth - 36f, y, linePaint)
        y += 16f

        // Table Header
        val boldRow = Paint(rowPaint).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText("Date", 36f, y, boldRow)
        canvas.drawText("Merchant / UPI", 118f, y, boldRow)
        canvas.drawText("Type / Category", 290f, y, boldRow)
        canvas.drawText("A/c", 430f, y, boldRow)
        canvas.drawText("Amount", 490f, y, boldRow)
        y += 8f
        canvas.drawLine(36f, y, pageWidth - 36f, y, linePaint)
        y += 16f

        transactions.forEach { tx ->
            if (y > pageHeight - 56f) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 48f
            }

            val dateStr = CurrencyFormatter.formatDateOnly(tx.timestamp)
            val merchantStr = tx.merchantName.take(24) + if (tx.isCancelled) " [CANCELLED]" else ""
            val typeStr = "${tx.type.label} • ${tx.category.label}".take(24)
            val acctStr = "XX${tx.accountLast4}"
            val amtStr = "₹${tx.amount.toInt()}"

            canvas.drawText(dateStr, 36f, y, rowPaint)
            canvas.drawText(merchantStr, 118f, y, rowPaint)
            canvas.drawText(typeStr, 290f, y, rowPaint)
            canvas.drawText(acctStr, 430f, y, rowPaint)
            canvas.drawText(amtStr, 490f, y, boldRow)
            y += 18f
        }

        document.finishPage(page)
        FileOutputStream(pdfFile).use { out -> document.writeTo(out) }
        document.close()
        return pdfFile
    }

    fun exportCsvStatement(
        context: Context,
        transactions: List<TransactionEntity>
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val csvFile = File(exportDir, "MandateX_Statement_$dateStamp.csv")

        val totalSpent = transactions.filter { !it.isCancelled }.sumOf { it.amount }
        val cardEmiTotal = transactions.filter { it.type == TransactionType.CARD_EMI }.sumOf { it.amount }
        val totalSaved = transactions.sumOf { it.savedAmount }

        val sb = StringBuilder()
        sb.appendLine("MandateX CA & Expense Export")
        sb.appendLine("Summary,Total Spent (INR),Cards & EMIs (INR),Money Saved (INR)")
        sb.appendLine("Totals,${totalSpent.toInt()},${cardEmiTotal.toInt()},${totalSaved.toInt()}")
        sb.appendLine()
        sb.appendLine("Date,Merchant,UPI_ID,Type,Category,Bank,Account_Last4,Amount_INR,Is_Trial,Status,Source")

        transactions.forEach { tx ->
            val date = CurrencyFormatter.formatTimestamp(tx.timestamp).replace(",", "")
            val status = if (tx.isCancelled) "Cancelled" else "Active"
            sb.appendLine(
                listOf(
                    escapeCsv(date),
                    escapeCsv(tx.merchantName),
                    escapeCsv(tx.upiId.orEmpty()),
                    escapeCsv(tx.type.label),
                    escapeCsv(tx.category.label),
                    escapeCsv(tx.bankName),
                    "XX${tx.accountLast4}",
                    tx.amount.toString(),
                    tx.isTrial.toString(),
                    status,
                    escapeCsv(tx.source)
                ).joinToString(",")
            )
        }

        csvFile.writeText(sb.toString())
        return csvFile
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "MandateX Financial Statement — ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Statement via WhatsApp or Email"))
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return "\"$clean\""
    }
}
