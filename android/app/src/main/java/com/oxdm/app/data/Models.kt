package com.oxdm.app.data

enum class JobPhase {
    Queued,
    Evaluating,
    Downloading,
    Paused,
    Finished,
    Failed,
    Canceled
}

enum class JobCategory {
    All,
    Compressed,
    Video,
    Audio,
    Document,
    Program,
    Other
}

data class PartUiModel(
    val id: String,
    val offset: Long,
    val size: Long,
    val downloaded: Long,
    val speedBps: Long,
    val isFinished: Boolean
)

data class JobUiModel(
    val id: String,
    val url: String,
    val filename: String,
    val category: JobCategory,
    val phase: JobPhase,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBps: Long,
    val etaSeconds: Long,
    val parts: List<PartUiModel>,
    val errorMessage: String? = null
) {
    val progressFraction: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedDownloadedSize: String
        get() = formatBytes(downloadedBytes)

    val formattedTotalSize: String
        get() = if (totalBytes > 0) formatBytes(totalBytes) else "Unknown size"

    val formattedSpeed: String
        get() = if (phase == JobPhase.Downloading && speedBps > 0) "${formatBytes(speedBps)}/s" else "—"

    val formattedEta: String
        get() = when {
            phase == JobPhase.Finished -> "Completed"
            phase == JobPhase.Paused -> "Paused"
            phase == JobPhase.Queued -> "Queued"
            phase == JobPhase.Downloading && etaSeconds > 0 -> formatDuration(etaSeconds)
            else -> "—"
        }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
            val pre = "KMGTPE"[exp - 1]
            return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
        }

        fun formatDuration(seconds: Long): String {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
        }
    }
}
