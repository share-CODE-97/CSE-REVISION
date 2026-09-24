package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.RevisionHistoryEntity
import com.example.data.entity.RevisionSubjectEntity
import com.example.data.repository.RevisionRepository
import com.example.domain.model.DashboardOverviewMetrics
import com.example.util.DateTimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val todaySubjects: List<RevisionSubjectEntity> = emptyList(),
    val overdueSubjects: List<RevisionSubjectEntity> = emptyList(),
    val next6HoursSubjects: List<RevisionSubjectEntity> = emptyList(),
    val allActiveSubjects: List<RevisionSubjectEntity> = emptyList(),
    val noMoreSubjects: List<RevisionSubjectEntity> = emptyList(),
    val metrics: DashboardOverviewMetrics = DashboardOverviewMetrics(0, 0, 0, 0),
    val globalHistory: List<RevisionHistoryEntity> = emptyList(),
    val currentTimeMillis: Long = System.currentTimeMillis()
)

class DashboardViewModel(
    private val revisionRepository: RevisionRepository
) : ViewModel() {

    private val _currentTime = MutableStateFlow(System.currentTimeMillis())

    init {
        // Ticker for countdowns (updates UI every second while screen is active)
        viewModelScope.launch {
            while (true) {
                _currentTime.value = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        revisionRepository.allSubjects,
        revisionRepository.globalHistory,
        revisionRepository.allCompletionTimestamps,
        _currentTime
    ) { subjects, history, completionTimestamps, now ->
        val todayList = mutableListOf<RevisionSubjectEntity>()
        val overdueList = mutableListOf<RevisionSubjectEntity>()
        val next6HoursList = mutableListOf<RevisionSubjectEntity>()
        val activeList = mutableListOf<RevisionSubjectEntity>()
        val noMoreList = mutableListOf<RevisionSubjectEntity>()

        var totalCompletedCount = 0
        var overdueCount = 0

        subjects.forEach { subject ->
            totalCompletedCount += subject.completedCount
            val due = subject.nextDueAt

            if (due != null) {
                activeList.add(subject)

                if (due < now) {
                    overdueList.add(subject)
                    overdueCount++
                } else {
                    if (DateTimeUtils.isToday(due)) {
                        todayList.add(subject)
                    }
                    if (DateTimeUtils.isWithinNextHours(due, 6, now)) {
                        next6HoursList.add(subject)
                    }
                }
            } else if (subject.completedCount > 0) {
                noMoreList.add(subject)
            } else {
                // Not started, still shows under All Revisions
                activeList.add(subject)
            }
        }

        // Sort overdue oldest first
        overdueList.sortBy { it.nextDueAt ?: Long.MAX_VALUE }

        // Completions today
        val completedTodayCount = completionTimestamps.count { DateTimeUtils.isToday(it) }
        val streak = DateTimeUtils.calculateStreak(completionTimestamps)

        val metrics = DashboardOverviewMetrics(
            totalRevisions = totalCompletedCount,
            completedToday = completedTodayCount,
            overdue = overdueCount,
            currentStreak = streak
        )

        DashboardUiState(
            todaySubjects = todayList,
            overdueSubjects = overdueList,
            next6HoursSubjects = next6HoursList,
            allActiveSubjects = activeList,
            noMoreSubjects = noMoreList,
            metrics = metrics,
            globalHistory = history,
            currentTimeMillis = now
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun refreshTime() {
        _currentTime.value = System.currentTimeMillis()
    }

    fun renameSubject(subjectId: String, newName: String) {
        viewModelScope.launch {
            revisionRepository.renameSubject(subjectId, newName)
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            revisionRepository.deleteSubject(subjectId)
        }
    }
}
