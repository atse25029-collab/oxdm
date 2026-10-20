package com.oxdm.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class OxdmEvent {
    data class JobAdded(val job: JobUiModel) : OxdmEvent()
    data class JobPhaseChanged(val jobId: String, val newPhase: JobPhase) : OxdmEvent()
    data class JobProgress(val jobId: String, val downloaded: Long, val speedBps: Long) : OxdmEvent()
    data class JobFinished(val jobId: String) : OxdmEvent()
    data class JobFailed(val jobId: String, val error: String) : OxdmEvent()
}

/**
 * Singleton bridge interface between Android / Kotlin and the native Rust oxdm engine.
 */
object OxdmBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _jobs = MutableStateFlow<List<JobUiModel>>(emptyList())
    val jobs: StateFlow<List<JobUiModel>> = _jobs.asStateFlow()

    private val _events = MutableSharedFlow<OxdmEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<OxdmEvent> = _events.asSharedFlow()

    private var isNativeLoaded = false

    fun initialize(dbDirectory: String) {
        try {
            System.loadLibrary("oxdm_mobile")
            nativeInit(dbDirectory)
            isNativeLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            // Log fallback when developing in IDE without precompiled ndk binaries
            e.printStackTrace()
        }
    }

    fun addJob(
        url: String,
        suggestedFilename: String? = null,
        category: JobCategory = JobCategory.Other,
        partsCount: Int = 8
    ): String {
        val id = UUID.randomUUID().toString()
        val filename = suggestedFilename ?: url.substringAfterLast("/").substringBefore("?").ifEmpty { "download" }

        val newJob = JobUiModel(
            id = id,
            url = url,
            filename = filename,
            category = category,
            phase = JobPhase.Queued,
            downloadedBytes = 0,
            totalBytes = 0,
            speedBps = 0,
            etaSeconds = 0,
            parts = emptyList()
        )

        _jobs.value = _jobs.value + newJob
        scope.launch {
            _events.emit(OxdmEvent.JobAdded(newJob))
        }

        if (isNativeLoaded) {
            nativeAddJob(id, url, filename, partsCount)
        }
        return id
    }

    fun pauseJob(id: String) {
        updateJob(id) { it.copy(phase = JobPhase.Paused, speedBps = 0) }
        scope.launch { _events.emit(OxdmEvent.JobPhaseChanged(id, JobPhase.Paused)) }
        if (isNativeLoaded) nativePauseJob(id)
    }

    fun resumeJob(id: String) {
        updateJob(id) { it.copy(phase = JobPhase.Downloading) }
        scope.launch { _events.emit(OxdmEvent.JobPhaseChanged(id, JobPhase.Downloading)) }
        if (isNativeLoaded) nativeResumeJob(id)
    }

    fun cancelJob(id: String) {
        updateJob(id) { it.copy(phase = JobPhase.Canceled, speedBps = 0) }
        scope.launch { _events.emit(OxdmEvent.JobPhaseChanged(id, JobPhase.Canceled)) }
        if (isNativeLoaded) nativeCancelJob(id)
    }

    fun deleteJob(id: String, deleteFile: Boolean) {
        _jobs.value = _jobs.value.filterNot { it.id == id }
        if (isNativeLoaded) nativeDeleteJob(id, deleteFile)
    }

    private fun updateJob(id: String, transform: (JobUiModel) -> JobUiModel) {
        _jobs.value = _jobs.value.map { if (it.id == id) transform(it) else it }
    }

    // Native JNI functions declared in oxdm-mobile
    private external fun nativeInit(dbPath: String)
    private external fun nativeAddJob(id: String, url: String, filename: String, parts: Int)
    private external fun nativePauseJob(id: String)
    private external fun nativeResumeJob(id: String)
    private external fun nativeCancelJob(id: String)
    private external fun nativeDeleteJob(id: String, deleteFile: Boolean)
}
