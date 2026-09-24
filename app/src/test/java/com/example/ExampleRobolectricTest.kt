package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.DateTimeUtils
import com.example.data.importer.QuestionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches Revision Tracker`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Revision Tracker", appName)
    }

    @Test
    fun `test date time is strictly future prevents same day bug`() {
        // Current time: 08:27
        val nowCal = Calendar.getInstance()
        nowCal.set(2026, Calendar.SEPTEMBER, 24, 8, 27, 0)
        val now = nowCal.timeInMillis

        // Future target: 08:30 on same day
        val targetCal = Calendar.getInstance()
        targetCal.set(2026, Calendar.SEPTEMBER, 24, 8, 30, 0)
        val targetFuture = targetCal.timeInMillis

        // Past target: 08:20 on same day
        val pastCal = Calendar.getInstance()
        pastCal.set(2026, Calendar.SEPTEMBER, 24, 8, 20, 0)
        val targetPast = pastCal.timeInMillis

        assertTrue("08:30 when now is 08:27 MUST be strictly future", DateTimeUtils.isStrictlyFuture(targetFuture, now))
        assertFalse("08:20 when now is 08:27 MUST NOT be future", DateTimeUtils.isStrictlyFuture(targetPast, now))
    }

    @Test
    fun `test add hours utility`() {
        val now = 1000000L
        val plus24 = DateTimeUtils.addHours(now, 24)
        val plus48 = DateTimeUtils.addHours(now, 48)

        assertEquals(now + 24L * 3600L * 1000L, plus24)
        assertEquals(now + 48L * 3600L * 1000L, plus48)
    }

    @Test
    fun `test format remaining time`() {
        val now = System.currentTimeMillis()
        val overdueTime = now - 3600_000L
        val formattedOverdue = DateTimeUtils.formatRemainingTime(overdueTime, now)
        assertEquals("Overdue", formattedOverdue)

        val futureTime = now + 7200_000L // 2 hours
        val formattedFuture = DateTimeUtils.formatRemainingTime(futureTime, now)
        assertEquals("02:00:00", formattedFuture)
    }

    @Test
    fun `test streak calculation with today and yesterday`() {
        val cal = Calendar.getInstance()
        val today = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val dayBeforeYesterday = cal.timeInMillis

        val streak = DateTimeUtils.calculateStreak(listOf(today, yesterday, dayBeforeYesterday))
        assertEquals(3, streak)
    }

    @Test
    fun `test QuestionParser parses user js mcq sample correctly`() {
        val jsSample = """
            // Computer Networks 2026 Question Bank
            const questions = [
              {
                id: "CN-NETWO-0631",
                subject: "Computer Network",
                subtopic: "Network Layer",
                question: "Which of the following is true for ARP (Address Resolution Protocol)?",
                options: ["Maps IP addresses to MAC addresses", "Works at the data link layer", "Is used in IPv6 networks", "More than one of the above", "None of the above"],
                answer: "Maps IP addresses to MAC addresses",
                explanation: "ARP maps network layer IP addresses to data link layer MAC addresses.",
              }
            ];
        """.trimIndent()

        val parsed = QuestionParser.parseQuestions(jsSample, fallbackSubject = "CSE")
        assertEquals(1, parsed.size)

        val q = parsed.first()
        assertEquals("CN-NETWO-0631", q.id)
        assertEquals("Computer Network", q.subjectTitle)
        assertEquals("Network Layer", q.subtopicTitle)
        assertEquals("Which of the following is true for ARP (Address Resolution Protocol)?", q.questionText)
        assertEquals(5, q.options.size)
        assertEquals("Maps IP addresses to MAC addresses", q.options[0])
        assertEquals(0, q.correctAnswerIndex) // Option 0 matches answer string
        assertEquals("ARP maps network layer IP addresses to data link layer MAC addresses.", q.explanation)
    }

    @Test
    fun `test QuestionParser resolves different answer types`() {
        val options = listOf("Option 1", "Option 2", "Option 3", "Option 4")

        // Exact text match
        assertEquals(2, QuestionParser.resolveCorrectAnswerIndex("Option 3", options))

        // Letter match
        assertEquals(0, QuestionParser.resolveCorrectAnswerIndex("A", options))
        assertEquals(1, QuestionParser.resolveCorrectAnswerIndex("B", options))
        assertEquals(2, QuestionParser.resolveCorrectAnswerIndex("C", options))
        assertEquals(3, QuestionParser.resolveCorrectAnswerIndex("D", options))

        // Numeric index
        assertEquals(1, QuestionParser.resolveCorrectAnswerIndex(1, options))
        assertEquals(0, QuestionParser.resolveCorrectAnswerIndex("0", options))
    }
}
