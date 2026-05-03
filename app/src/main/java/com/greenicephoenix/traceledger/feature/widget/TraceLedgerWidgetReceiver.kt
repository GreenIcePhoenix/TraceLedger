package com.greenicephoenix.traceledger.feature.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * TraceLedgerWidgetReceiver is the entry point Android uses to communicate
 * with our widget.
 *
 * WHY WE EXTEND GlanceAppWidgetReceiver (not AppWidgetProvider directly):
 * GlanceAppWidgetReceiver handles the plumbing of calling GlanceAppWidget.update()
 * correctly in a coroutine. We just need to tell it which GlanceAppWidget to use.
 *
 * This receiver is registered in AndroidManifest.xml with:
 *   - action APPWIDGET_UPDATE (called when widget is added or needs refresh)
 *   - meta-data pointing to traceledger_widget_info.xml
 */
class TraceLedgerWidgetReceiver : GlanceAppWidgetReceiver() {

    // Tell the receiver which GlanceAppWidget class to delegate to
    override val glanceAppWidget: TraceLedgerWidget = TraceLedgerWidget()
}