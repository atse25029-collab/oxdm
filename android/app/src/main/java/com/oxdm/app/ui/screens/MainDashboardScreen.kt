package com.oxdm.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oxdm.app.data.JobPhase
import com.oxdm.app.data.OxdmBridge
import com.oxdm.app.ui.components.JobItemCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    onOpenBrowser: () -> Unit,
    modifier: Modifier = Modifier
) {
    val jobs by OxdmBridge.jobs.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddSheet by remember { mutableStateOf(false) }

    val tabs = listOf("All", "Downloading", "Completed", "Queued")
    val filteredJobs = when (selectedTab) {
        1 -> jobs.filter { it.phase == JobPhase.Downloading }
        2 -> jobs.filter { it.phase == JobPhase.Finished }
        3 -> jobs.filter { it.phase == JobPhase.Queued || it.phase == JobPhase.Paused }
        else -> jobs
    }

    val activeCount = jobs.count { it.phase == JobPhase.Downloading }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("oxdm") },
                actions = {
                    IconButton(onClick = onOpenBrowser) {
                        Icon(Icons.Default.Language, contentDescription = "In-App Browser")
                    }
                    if (activeCount > 0) {
                        IconButton(onClick = { jobs.forEach { if (it.phase == JobPhase.Downloading) OxdmBridge.pauseJob(it.id) } }) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause All")
                        }
                    } else {
                        IconButton(onClick = { jobs.forEach { if (it.phase == JobPhase.Paused) OxdmBridge.resumeJob(it.id) } }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume All")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Download")
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (filteredJobs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No downloads in this category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(top = 8.dp)
                ) {
                    items(filteredJobs, key = { it.id }) { job ->
                        JobItemCard(
                            job = job,
                            onPause = { OxdmBridge.pauseJob(job.id) },
                            onResume = { OxdmBridge.resumeJob(job.id) },
                            onCancel = { OxdmBridge.cancelJob(job.id) },
                            onClick = {}
                        )
                    }
                }
            }
        }

        if (showAddSheet) {
            AddDownloadSheet(
                onDismiss = { showAddSheet = false },
                onConfirm = { url, filename, category, parts ->
                    OxdmBridge.addJob(url, filename, category, parts)
                    showAddSheet = false
                }
            )
        }
    }
}
