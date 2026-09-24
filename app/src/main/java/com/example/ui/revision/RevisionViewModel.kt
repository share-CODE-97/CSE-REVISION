package com.example.ui.revision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.NoteEntity
import com.example.data.entity.RevisionHistoryEntity
import com.example.data.entity.RevisionSubjectEntity
import com.example.data.entity.TopicEntity
import com.example.data.repository.RevisionRepository
import com.example.data.repository.StudyRepository
import com.example.domain.model.RevisionScheduleChoice
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RevisionLocalState(
    val selectedSchedule: RevisionScheduleChoice = RevisionScheduleChoice.None,
    val customTargetTimestamp: Long? = null,
    val errorMessage: String? = null,
    val isCompleteInProgress: Boolean = false
)

data class RevisionUiState(
    val subject: RevisionSubjectEntity? = null,
    val topics: List<TopicEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val history: List<RevisionHistoryEntity> = emptyList(),
    val selectedSchedule: RevisionScheduleChoice = RevisionScheduleChoice.None,
    val customTargetTimestamp: Long? = null,
    val errorMessage: String? = null,
    val isCompleteInProgress: Boolean = false
)

class RevisionViewModel(
    private val subjectId: String,
    private val revisionRepository: RevisionRepository,
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _localState = MutableStateFlow(RevisionLocalState())

    private val _navigationEvents = MutableSharedFlow<String>()
    val navigationEvents: SharedFlow<String> = _navigationEvents.asSharedFlow()

    val uiState: StateFlow<RevisionUiState> = combine(
        revisionRepository.getSubject(subjectId),
        studyRepository.getTopicsForSubject(subjectId),
        studyRepository.getNotesForSubject(subjectId),
        revisionRepository.getSubjectHistory(subjectId),
        _localState
    ) { subject, topics, notes, history, local ->
        RevisionUiState(
            subject = subject,
            topics = topics,
            notes = notes,
            history = history,
            selectedSchedule = local.selectedSchedule,
            customTargetTimestamp = local.customTargetTimestamp,
            errorMessage = local.errorMessage,
            isCompleteInProgress = local.isCompleteInProgress
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RevisionUiState()
    )

    fun selectSchedule(choice: RevisionScheduleChoice) {
        _localState.value = _localState.value.copy(
            selectedSchedule = choice,
            errorMessage = null
        )
    }

    /**
     * Set custom date and time with strict validation.
     * Prevents same-day bug: 08:30 when now is 08:27 is VALID.
     * Only strictly past timestamps are rejected.
     */
    fun setCustomDateTime(timestamp: Long) {
        val now = System.currentTimeMillis()
        if (!DateTimeUtils.isStrictlyFuture(timestamp, now)) {
            _localState.value = _localState.value.copy(
                errorMessage = "Selected time is in the past. Please choose a future time."
            )
            return
        }
        _localState.value = _localState.value.copy(
            errorMessage = null,
            customTargetTimestamp = timestamp,
            selectedSchedule = RevisionScheduleChoice.Custom(timestamp)
        )
    }

    fun clearError() {
        _localState.value = _localState.value.copy(errorMessage = null)
    }

    /**
     * Atomically commits completion ONLY when Complete is pressed.
     * Temporary selections do not write to Room until this method is called.
     */
    fun completeRevision() {
        val currentLocal = _localState.value
        if (currentLocal.isCompleteInProgress) return // Prevent double-clicks
        _localState.value = currentLocal.copy(isCompleteInProgress = true)

        val now = System.currentTimeMillis()
        val nextDue: Long? = when (val schedule = currentLocal.selectedSchedule) {
            is RevisionScheduleChoice.Plus24Hours -> DateTimeUtils.addHours(now, 24)
            is RevisionScheduleChoice.Plus48Hours -> DateTimeUtils.addHours(now, 48)
            is RevisionScheduleChoice.Custom -> {
                if (DateTimeUtils.isStrictlyFuture(schedule.targetTimestamp, now)) {
                    schedule.targetTimestamp
                } else {
                    _localState.value = currentLocal.copy(
                        errorMessage = "Selected scheduled time is in the past.",
                        isCompleteInProgress = false
                    )
                    return
                }
            }
            is RevisionScheduleChoice.None -> null // Marks as "No more revision needed"
        }

        viewModelScope.launch {
            revisionRepository.completeRevision(subjectId, nextDue)
            _localState.value = _localState.value.copy(isCompleteInProgress = false)
            _navigationEvents.emit("COMPLETED")
        }
    }

    fun undoLastComplete() {
        viewModelScope.launch {
            revisionRepository.undoLastComplete(subjectId)
        }
    }
}
