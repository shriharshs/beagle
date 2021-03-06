package com.pandulapeter.beagle.core.view.bugReport

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pandulapeter.beagle.BeagleCore
import com.pandulapeter.beagle.common.configuration.Text
import com.pandulapeter.beagle.commonBase.currentTimestamp
import com.pandulapeter.beagle.core.list.delegates.LifecycleLogListDelegate
import com.pandulapeter.beagle.core.util.extension.createBugReportTextFile
import com.pandulapeter.beagle.core.util.extension.createLogFile
import com.pandulapeter.beagle.core.util.extension.createZipFile
import com.pandulapeter.beagle.core.util.extension.getBugReportsFolder
import com.pandulapeter.beagle.core.util.extension.getLogsFolder
import com.pandulapeter.beagle.core.util.extension.getScreenCapturesFolder
import com.pandulapeter.beagle.core.util.extension.getUriForFile
import com.pandulapeter.beagle.core.util.extension.text
import com.pandulapeter.beagle.core.util.model.CrashLogEntry
import com.pandulapeter.beagle.core.util.model.RestoreModel
import com.pandulapeter.beagle.core.view.bugReport.list.BugReportListItem
import com.pandulapeter.beagle.core.view.bugReport.list.CrashLogItemViewHolder
import com.pandulapeter.beagle.core.view.bugReport.list.DescriptionViewHolder
import com.pandulapeter.beagle.core.view.bugReport.list.GalleryViewHolder
import com.pandulapeter.beagle.core.view.bugReport.list.HeaderViewHolder
import com.pandulapeter.beagle.core.view.bugReport.list.LifecycleLogItemViewHolder
import com.pandulapeter.beagle.core.view.bugReport.list.LogItemViewHolder
import com.pandulapeter.beagle.core.view.bugReport.list.MetadataItemViewHolder
import com.pandulapeter.beagle.core.view.bugReport.list.NetworkLogItemViewHolder
import com.pandulapeter.beagle.core.view.bugReport.list.ShowMoreViewHolder
import com.pandulapeter.beagle.core.view.networkLogDetail.NetworkLogDetailDialogViewModel
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

internal class BugReportViewModel(
    application: Application,
    restoreModel: RestoreModel?,
    crashLogEntryToShow: CrashLogEntry?,
    val buildInformation: CharSequence,
    val deviceInformation: CharSequence,
    private val textInputTitles: List<Text>,
    textInputDescriptions: List<Text>
) : AndroidViewModel(application) {

    private val pageSize = BeagleCore.implementation.behavior.bugReportingBehavior.pageSize
    private val shouldShowGallerySection = BeagleCore.implementation.behavior.bugReportingBehavior.shouldShowGallerySection
    private val shouldShowCrashLogsSection = BeagleCore.implementation.behavior.bugReportingBehavior.shouldShowCrashLogsSection
    private val shouldShowNetworkLogsSection = BeagleCore.implementation.behavior.bugReportingBehavior.shouldShowNetworkLogsSection
    private val logLabelSectionsToShow = BeagleCore.implementation.behavior.bugReportingBehavior.logLabelSectionsToShow
    private val lifecycleSectionEventTypes = BeagleCore.implementation.behavior.bugReportingBehavior.lifecycleSectionEventTypes
    private val shouldShowMetadataSection = BeagleCore.implementation.behavior.bugReportingBehavior.shouldShowMetadataSection

    private val getCrashLogFileName get() = BeagleCore.implementation.behavior.bugReportingBehavior.getCrashLogFileName
    private val getNetworkLogFileName get() = BeagleCore.implementation.behavior.networkLogBehavior.getFileName
    private val getLogFileName get() = BeagleCore.implementation.behavior.logBehavior.getFileName
    private val getLifecycleLogFileName get() = BeagleCore.implementation.behavior.lifecycleLogBehavior.getFileName
    private val getBugReportFileName get() = BeagleCore.implementation.behavior.bugReportingBehavior.getBugReportFileName
    private val onBugReportReady get() = BeagleCore.implementation.behavior.bugReportingBehavior.onBugReportReady

    private val _items = MutableLiveData(emptyList<BugReportListItem>())
    val items: LiveData<List<BugReportListItem>> = _items
    private val _shouldShowLoadingIndicator = MutableLiveData(true)
    val shouldShowLoadingIndicator: LiveData<Boolean> = _shouldShowLoadingIndicator

    private var mediaFiles = emptyList<File>()
    private var selectedMediaFileIds = emptyList<String>()

    var allCrashLogEntries: List<CrashLogEntry>? = null
        private set
    private var lastCrashLogIndex = pageSize
    private var selectedCrashLogIds = emptyList<String>()
    private fun getCrashLogEntries() = allCrashLogEntries?.take(lastCrashLogIndex).orEmpty()
    private fun areThereMoreCrashLogEntries() = (allCrashLogEntries?.size ?: 0) > getCrashLogEntries().size

    val allNetworkLogEntries by lazy { BeagleCore.implementation.getNetworkLogEntries() }
    private var lastNetworkLogIndex = pageSize
    private var selectedNetworkLogIds = emptyList<String>()
    private fun getNetworkLogEntries() = allNetworkLogEntries.take(lastNetworkLogIndex)
    private fun areThereMoreNetworkLogEntries() = allNetworkLogEntries.size > getNetworkLogEntries().size

    private val allLogEntries by lazy { logLabelSectionsToShow.map { label -> label to BeagleCore.implementation.getLogEntries(label) }.toMap() }
    private val lastLogIndex = logLabelSectionsToShow.map { label -> label to pageSize }.toMap().toMutableMap()
    private val selectedLogIds = logLabelSectionsToShow.map { label -> label to emptyList<String>() }.toMap().toMutableMap()
    private fun getLogEntries(label: String?) = allLogEntries[label]?.take(lastLogIndex[label] ?: 0).orEmpty()
    private fun areThereMoreLogEntries(label: String?) = allLogEntries[label]?.size ?: 0 > getLogEntries(label).size

    val allLifecycleLogEntries by lazy { BeagleCore.implementation.getLifecycleLogEntries(lifecycleSectionEventTypes) }
    private var lastLifecycleLogIndex = pageSize
    private var selectedLifecycleLogIds = emptyList<String>()
    private fun getLifecycleLogEntries() = allLifecycleLogEntries.take(lastLifecycleLogIndex)
    private fun areThereMoreLifecycleEntries() = allLifecycleLogEntries.size > getLifecycleLogEntries().size

    private var shouldAttachBuildInformation = true
    private var shouldAttachDeviceInformation = true

    private val textInputValues = textInputDescriptions.map(application::text).toMutableList()

    private val context = getApplication<Application>()
    private val listManagerContext = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
    private val _isSendButtonEnabled = MutableLiveData(false)
    val isSendButtonEnabled: LiveData<Boolean> = _isSendButtonEnabled
    private var isPreparingData = false
        set(value) {
            field = value
            _shouldShowLoadingIndicator.postValue(value)
            refreshSendButton()
        }
    val zipFileUriToShare = MutableLiveData<Uri?>()

    init {
        if (crashLogEntryToShow != null) {
            BeagleCore.implementation.logCrash(crashLogEntryToShow)
        }
        refresh(restoreModel)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun refresh(restoreModel: RestoreModel? = null) {
        _shouldShowLoadingIndicator.postValue(true)
        viewModelScope.launch(listManagerContext) {
            restoreModel?.let(BeagleCore.implementation::restoreAfterCrash)
            mediaFiles = context.getScreenCapturesFolder().listFiles().orEmpty().toList().sortedByDescending { it.lastModified() }
            selectedMediaFileIds = selectedMediaFileIds.filter { id -> mediaFiles.any { it.name == id } }
            if (allCrashLogEntries == null) {
                allCrashLogEntries = BeagleCore.implementation.getCrashLogEntries()
            }
            refreshContent()
        }
    }

    fun onMediaFileLongTapped(fileName: String) = onMediaFileSelectionChanged(fileName)

    fun onCrashLogLongTapped(id: String) = onCrashLogSelectionChanged(id)

    fun onNetworkLogLongTapped(id: String) = onNetworkLogSelectionChanged(id)

    fun onLogLongTapped(id: String, label: String?) = onLogSelectionChanged(id, label)

    fun onLifecycleLogLongTapped(id: String) = onLifecycleLogSelectionChanged(id)

    fun onShowMoreTapped(type: ShowMoreViewHolder.Type) {
        viewModelScope.launch(listManagerContext) {
            when (type) {
                ShowMoreViewHolder.Type.CrashLog -> lastCrashLogIndex += pageSize
                ShowMoreViewHolder.Type.NetworkLog -> lastNetworkLogIndex += pageSize
                is ShowMoreViewHolder.Type.Log -> lastLogIndex[type.label] = (lastLogIndex[type.label] ?: 0) + pageSize
                ShowMoreViewHolder.Type.LifecycleLog -> lastLifecycleLogIndex += pageSize
            }
            refreshContent()
        }
    }

    fun onDescriptionChanged(index: Int, newValue: CharSequence) {
        textInputValues[index] = newValue
        refreshSendButton()
    }

    fun onMetadataItemSelectionChanged(type: MetadataType) {
        viewModelScope.launch(listManagerContext) {
            when (type) {
                MetadataType.BUILD_INFORMATION -> shouldAttachBuildInformation = !shouldAttachBuildInformation
                MetadataType.DEVICE_INFORMATION -> shouldAttachDeviceInformation = !shouldAttachDeviceInformation
            }
            refreshContent()
        }
    }

    fun onSendButtonPressed() {
        if (isSendButtonEnabled.value == true && _shouldShowLoadingIndicator.value == false) {
            viewModelScope.launch {
                isPreparingData = true
                val filePaths = mutableListOf<String>()

                // Media files
                filePaths.addAll(
                    selectedMediaFileIds
                        .map { fileName -> context.getUriForFile(context.getScreenCapturesFolder().resolve(fileName)) }
                        .toPaths(context.getScreenCapturesFolder())
                )

                // Crash logs
                filePaths.addAll(
                    selectedCrashLogIds
                        .mapNotNull { id ->
                            allCrashLogEntries?.firstOrNull { it.id == id }?.let { entry ->
                                context.createLogFile(
                                    fileName = "${getCrashLogFileName(currentTimestamp, entry.id)}.txt",
                                    content = entry.getFormattedContents(BeagleCore.implementation.appearance.logShortTimestampFormatter).toString()
                                )
                            }
                        }
                        .toPaths(context.getLogsFolder())
                )

                // Network log files
                filePaths.addAll(
                    selectedNetworkLogIds
                        .mapNotNull { id ->
                            allNetworkLogEntries.firstOrNull { it.id == id }?.let { entry ->
                                context.createLogFile(
                                    fileName = "${getNetworkLogFileName(currentTimestamp, entry.id)}.txt",
                                    content = NetworkLogDetailDialogViewModel.createLogFileContents(
                                        title = NetworkLogDetailDialogViewModel.createTitle(
                                            isOutgoing = entry.isOutgoing,
                                            url = entry.url
                                        ),
                                        metadata = NetworkLogDetailDialogViewModel.createMetadata(
                                            context = context,
                                            headers = entry.headers,
                                            timestamp = entry.timestamp,
                                            duration = entry.duration
                                        ),
                                        formattedJson = NetworkLogDetailDialogViewModel.formatJson(
                                            json = entry.payload,
                                            indentation = 4
                                        )
                                    )
                                )
                            }
                        }
                        .toPaths(context.getLogsFolder())
                )

                // Log files
                filePaths.addAll(
                    allLogEntries[null]?.let { allLogEntries ->
                        selectedLogIds.flatMap { it.value }.distinct().mapNotNull { id -> allLogEntries.firstOrNull { it.id == id } }
                    }.orEmpty().mapNotNull { entry ->
                        context.createLogFile(
                            fileName = "${getLogFileName(currentTimestamp, entry.id)}.txt",
                            content = entry.getFormattedContents(BeagleCore.implementation.appearance.logShortTimestampFormatter).toString()
                        )
                    }.toPaths(context.getLogsFolder())
                )

                // Lifecycle logs
                filePaths.addAll(
                    selectedLifecycleLogIds
                        .mapNotNull { id ->
                            allLifecycleLogEntries.firstOrNull { it.id == id }?.let { entry ->
                                context.createLogFile(
                                    fileName = "${getLifecycleLogFileName(currentTimestamp, entry.id)}.txt",
                                    content = LifecycleLogListDelegate.format(
                                        entry = entry,
                                        formatter = BeagleCore.implementation.appearance.logShortTimestampFormatter,
                                        shouldDisplayFullNames = BeagleCore.implementation.behavior.lifecycleLogBehavior.shouldDisplayFullNames
                                    ).toString()
                                )
                            }
                        }
                        .toPaths(context.getLogsFolder())
                )

                // Build information
                var content = ""
                if (shouldShowMetadataSection && shouldAttachBuildInformation && buildInformation.isNotBlank()) {
                    content = buildInformation.toString()
                }

                // Device information
                if (shouldShowMetadataSection && shouldAttachDeviceInformation) {
                    content = if (content.isBlank()) deviceInformation.toString() else "$content\n\n$deviceInformation"
                }

                // Text inputs
                textInputValues.forEachIndexed { index, textInputValue ->
                    if (textInputValue.trim().isNotBlank()) {
                        val text = "${context.text(textInputTitles[index])}\n${textInputValue.trim()}"
                        content = if (content.isBlank()) text else "$content\n\n$text"
                    }
                }

                // Create the log file
                if (content.isNotBlank()) {
                    context.createBugReportTextFile(
                        fileName = "${getBugReportFileName(currentTimestamp)}.txt",
                        content = content
                    )?.let { uri -> filePaths.add(uri.toPath(context.getBugReportsFolder())) }
                }

                // Create the zip file
                val uri = context.createZipFile(
                    filePaths = filePaths,
                    zipFileName = "${getBugReportFileName(currentTimestamp)}.zip",
                )
                onBugReportReady.let { onBugReportReady ->
                    if (onBugReportReady == null) {
                        zipFileUriToShare.postValue(uri)
                    } else {
                        onBugReportReady(uri)
                    }
                }
                isPreparingData = false
            }
        }
    }

    private fun refreshSendButton() {
        _isSendButtonEnabled.postValue(
            !isPreparingData && (selectedMediaFileIds.isNotEmpty() ||
                    selectedCrashLogIds.isNotEmpty() ||
                    selectedNetworkLogIds.isNotEmpty() ||
                    selectedLogIds.keys.any { label -> selectedLogIds[label]?.isNotEmpty() == true } ||
                    selectedLifecycleLogIds.isNotEmpty() ||
                    textInputValues.any { it.trim().isNotEmpty() })
        )
    }

    private fun onMediaFileSelectionChanged(id: String) {
        viewModelScope.launch(listManagerContext) {
            selectedMediaFileIds = if (selectedMediaFileIds.contains(id)) {
                selectedMediaFileIds.filterNot { it == id }
            } else {
                (selectedMediaFileIds + id)
            }.distinct()
            refreshContent()
        }
    }

    private fun onCrashLogSelectionChanged(id: String) {
        viewModelScope.launch(listManagerContext) {
            selectedCrashLogIds = if (selectedCrashLogIds.contains(id)) {
                selectedCrashLogIds.filterNot { it == id }
            } else {
                (selectedCrashLogIds + id)
            }.distinct()
            refreshContent()
        }
    }

    private fun onNetworkLogSelectionChanged(id: String) {
        viewModelScope.launch(listManagerContext) {
            selectedNetworkLogIds = if (selectedNetworkLogIds.contains(id)) {
                selectedNetworkLogIds.filterNot { it == id }
            } else {
                (selectedNetworkLogIds + id)
            }.distinct()
            refreshContent()
        }
    }

    private fun onLogSelectionChanged(id: String, label: String?) {
        viewModelScope.launch(listManagerContext) {
            selectedLogIds[label] = if (selectedLogIds[label]?.contains(id) == true) {
                selectedLogIds[label].orEmpty().filterNot { it == id }
            } else {
                (selectedLogIds[label].orEmpty() + id)
            }.distinct()
            if (label != null && logLabelSectionsToShow.contains(null)) {
                selectedLogIds[null] = if (selectedLogIds[null]?.contains(id) == true) {
                    selectedLogIds[null].orEmpty().filterNot { it == id }
                } else {
                    (selectedLogIds[null].orEmpty() + id)
                }.distinct()
            }
            refreshContent()
        }
    }

    private fun onLifecycleLogSelectionChanged(id: String) {
        viewModelScope.launch(listManagerContext) {
            selectedLifecycleLogIds = if (selectedLifecycleLogIds.contains(id)) {
                selectedLifecycleLogIds.filterNot { it == id }
            } else {
                (selectedLifecycleLogIds + id)
            }.distinct()
            refreshContent()
        }
    }

    private suspend fun refreshContent() = withContext(listManagerContext) {
        _items.postValue(mutableListOf<BugReportListItem>().apply {
            // Media files
            if (shouldShowGallerySection && mediaFiles.isNotEmpty()) {
                add(
                    HeaderViewHolder.UiModel(
                        id = "headerGallery",
                        text = BeagleCore.implementation.appearance.bugReportTexts.gallerySectionTitle(selectedMediaFileIds.size)
                    )
                )
                add(GalleryViewHolder.UiModel(mediaFiles.map { it.name to it.lastModified() }, selectedMediaFileIds))
            }

            // Crash logs
            getCrashLogEntries().let { crashLogEntries ->
                if (shouldShowCrashLogsSection && crashLogEntries.isNotEmpty()) {
                    add(
                        HeaderViewHolder.UiModel(
                            id = "headerCrashLogs",
                            text = BeagleCore.implementation.appearance.bugReportTexts.crashLogsSectionTitle(selectedCrashLogIds.size)
                        )
                    )
                    addAll(crashLogEntries.map { entry ->
                        CrashLogItemViewHolder.UiModel(
                            entry = entry,
                            isSelected = selectedCrashLogIds.contains(entry.id)
                        )
                    })
                    if (areThereMoreCrashLogEntries()) {
                        add(ShowMoreViewHolder.UiModel(ShowMoreViewHolder.Type.CrashLog))
                    }
                }
            }

            // Network logs
            getNetworkLogEntries().let { networkLogEntries ->
                if (shouldShowNetworkLogsSection && networkLogEntries.isNotEmpty()) {
                    add(
                        HeaderViewHolder.UiModel(
                            id = "headerNetworkLogs",
                            text = BeagleCore.implementation.appearance.bugReportTexts.networkLogsSectionTitle(selectedNetworkLogIds.size)
                        )
                    )
                    addAll(networkLogEntries.map { entry ->
                        NetworkLogItemViewHolder.UiModel(
                            entry = entry,
                            isSelected = selectedNetworkLogIds.contains(entry.id)
                        )
                    })
                    if (areThereMoreNetworkLogEntries()) {
                        add(ShowMoreViewHolder.UiModel(ShowMoreViewHolder.Type.NetworkLog))
                    }
                }
            }

            // Logs
            logLabelSectionsToShow.distinct().forEach { label ->
                getLogEntries(label).let { logEntries ->
                    if (logEntries.isNotEmpty()) {
                        add(
                            HeaderViewHolder.UiModel(
                                id = "headerLogs_$label",
                                text = BeagleCore.implementation.appearance.bugReportTexts.logsSectionTitle(label, selectedLogIds[label]?.size ?: 0)
                            )
                        )
                        addAll(logEntries.map { entry ->
                            LogItemViewHolder.UiModel(
                                entry = entry,
                                isSelected = selectedLogIds[label].orEmpty().contains(entry.id)
                            )
                        })
                        if (areThereMoreLogEntries(label)) {
                            add(ShowMoreViewHolder.UiModel(ShowMoreViewHolder.Type.Log(label)))
                        }
                    }
                }
            }

            // Lifecycle logs
            getLifecycleLogEntries().let { lifecycleLogEntries ->
                if (lifecycleLogEntries.isNotEmpty()) {
                    add(
                        HeaderViewHolder.UiModel(
                            id = "headerLifecycleLogs",
                            text = BeagleCore.implementation.appearance.bugReportTexts.lifecycleLogsSectionTitle(selectedLifecycleLogIds.size)
                        )
                    )
                    addAll(lifecycleLogEntries.map { entry ->
                        LifecycleLogItemViewHolder.UiModel(
                            entry = entry,
                            isSelected = selectedLifecycleLogIds.contains(entry.id)
                        )
                    })
                    if (areThereMoreLifecycleEntries()) {
                        add(ShowMoreViewHolder.UiModel(ShowMoreViewHolder.Type.LifecycleLog))
                    }
                }
            }

            // Metadata
            if (shouldShowMetadataSection) {
                add(
                    HeaderViewHolder.UiModel(
                        id = "headerMetadata",
                        text = BeagleCore.implementation.appearance.bugReportTexts.metadataSectionTitle
                    )
                )
                if (buildInformation.isNotBlank()) {
                    add(
                        MetadataItemViewHolder.UiModel(
                            type = MetadataType.BUILD_INFORMATION,
                            isSelected = shouldAttachBuildInformation
                        )
                    )
                }
                add(
                    MetadataItemViewHolder.UiModel(
                        type = MetadataType.DEVICE_INFORMATION,
                        isSelected = shouldAttachDeviceInformation
                    )
                )
            }

            // Input fields
            textInputTitles.forEachIndexed { index, title ->
                add(
                    HeaderViewHolder.UiModel(
                        id = "textInputTitle_$index",
                        text = title
                    )
                )
                add(
                    DescriptionViewHolder.UiModel(
                        index = index,
                        text = textInputValues[index]
                    )
                )
            }
        })
        refreshSendButton()
        _shouldShowLoadingIndicator.postValue(false)
    }

    private fun Uri.toPath(folder: File): String = "${folder.canonicalPath}/$realPath"

    private fun List<Uri>.toPaths(folder: File): List<String> = folder.canonicalPath.let { path -> map { "$path/${it.realPath}" } }

    private val Uri.realPath get() = path?.split("/")?.lastOrNull()

    enum class MetadataType {
        BUILD_INFORMATION,
        DEVICE_INFORMATION
    }
}