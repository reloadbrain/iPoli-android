package io.ipoli.android.growth.usecase

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.ipoli.android.TestUtil
import io.ipoli.android.common.datetime.*
import io.ipoli.android.planday.usecase.CalculateAwesomenessScoreUseCase
import io.ipoli.android.planday.usecase.CalculateFocusDurationUseCase
import io.ipoli.android.quest.Color
import io.ipoli.android.quest.Icon
import io.ipoli.android.quest.Quest
import io.ipoli.android.quest.TimeRange
import io.ipoli.android.tag.Tag
import org.amshove.kluent.`should be empty`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.any
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import org.threeten.bp.temporal.TemporalAdjusters

class CalculateGrowthStatsUseCaseSpek : Spek({

    val completedQuest = TestUtil.quest.copy(
        experience = 50,
        coins = 50,
        completedAtDate = LocalDate.now(),
        completedAtTime = Time.now()
    )

    describe("CalculateGrowthStatsUseCase") {

        fun executeUseCase(
            completedQuests: List<Quest> = listOf(),
            currentDate: LocalDate = LocalDate.now(),
            firstDayOfWeek: DayOfWeek = DateUtils.firstDayOfWeek,
            lastDayOfWeek: DayOfWeek = DateUtils.lastDayOfWeek
        ) =
            CalculateGrowthStatsUseCase(
                CalculateAwesomenessScoreUseCase(mock()),
                CalculateFocusDurationUseCase(mock()),
                mock {
                    on {
                        findOriginallyScheduledOrCompletedInPeriod(
                            any(),
                            any()
                        )
                    } doReturn completedQuests
                },
                mock()
            ).execute(
                CalculateGrowthStatsUseCase.Params(
                    includeAppUsageStats = false,
                    currentDate = currentDate,
                    firstDayOfWeek = firstDayOfWeek,
                    lastDayOfWeek = lastDayOfWeek
                )
            )

        describe("WeeklyGrowth") {

            it("should return start & end of week according to locale") {
                val res = executeUseCase()
                res.weeklyGrowth.startDate.daysBetween(res.weeklyGrowth.endDate)
                    .`should equal`(7)
                res.weeklyGrowth.startDate.dayOfWeek.`should equal`(DateUtils.firstDayOfWeek)
                res.weeklyGrowth.endDate.dayOfWeek.`should equal`(DateUtils.lastDayOfWeek)
            }

            it("should return week progress entries according to locale") {
                val res = executeUseCase()
                res.weeklyGrowth.progressEntries.forEachIndexed { i, week ->
                    week.date.dayOfWeek.`should equal`(res.weeklyGrowth.startDate.plusDays(i.toLong()).dayOfWeek)
                }
            }

            it("should return 0 for progress when no Quests are found") {
                val res = executeUseCase()
                res.weeklyGrowth.progressEntries.forEach {
                    it.productiveMinutes.`should equal`(0.minutes)
                }
            }

            it("should return 1 productive hour for today") {
                val res = executeUseCase(
                    listOf(
                        completedQuest.copy(
                            timeRanges = listOf(
                                TimeRange(
                                    TimeRange.Type.COUNTDOWN,
                                    60
                                )
                            )
                        )
                    )
                )
                val progressEntries = res.weeklyGrowth.progressEntries
                progressEntries.size.`should equal`(7)
                val todayProgress = progressEntries.first { it.date == LocalDate.now() }
                todayProgress.productiveMinutes.`should equal`(60.minutes)
                (progressEntries - todayProgress).forEach {
                    it.productiveMinutes.`should equal`(0.minutes)
                }
            }

            it("should return sum of productive for today quests") {
                val quest = completedQuest.copy(
                    timeRanges = listOf(
                        TimeRange(
                            TimeRange.Type.COUNTDOWN,
                            60
                        )
                    )
                )
                val res = executeUseCase(
                    listOf(
                        quest,
                        quest
                    )
                )
                val progressEntries = res.weeklyGrowth.progressEntries
                val todayProgress = progressEntries.first { it.date == LocalDate.now() }
                todayProgress.productiveMinutes.`should equal`(2.hours.asMinutes)
            }
        }

        it("should return productive hour from quest with timer for today") {
            val res = executeUseCase(
                listOf(
                    completedQuest.copy(
                        timeRanges = listOf(
                            TimeRange(
                                TimeRange.Type.COUNTDOWN,
                                60
                            )
                        )
                    ),
                    completedQuest
                )
            )
            val progressEntries = res.weeklyGrowth.progressEntries
            val todayProgress = progressEntries.first { it.date == LocalDate.now() }
            todayProgress.productiveMinutes.`should equal`(60.minutes)
        }

        it("should return productive hours for quests in different days") {
            val date =
                LocalDate.now().with(TemporalAdjusters.previousOrSame(DateUtils.firstDayOfWeek))
            val res = executeUseCase(
                listOf(
                    completedQuest.copy(
                        timeRanges = listOf(
                            TimeRange(
                                TimeRange.Type.COUNTDOWN,
                                60
                            )
                        ),
                        completedAtDate = date
                    ),
                    completedQuest.copy(
                        timeRanges = listOf(
                            TimeRange(
                                TimeRange.Type.COUNTDOWN,
                                30
                            )
                        ),
                        completedAtDate = date.plusDays(1)
                    )
                )
            )
            val progressEntries = res.weeklyGrowth.progressEntries
            val firstProgress = progressEntries.first { it.date == date }
            val secondProgress = progressEntries.first { it.date == date.plusDays(1) }
            firstProgress.productiveMinutes.`should equal`(60.minutes)
            secondProgress.productiveMinutes.`should equal`(30.minutes)
        }

        describe("MonthlyGrowth") {

            it("should return 4 equal periods") {
                val res = executeUseCase(
                    currentDate = LocalDate.of(2015, Month.FEBRUARY, 10),
                    firstDayOfWeek = DayOfWeek.SUNDAY,
                    lastDayOfWeek = DayOfWeek.SATURDAY
                )

                val progressEntries = res.monthlyGrowth.progressEntries
                progressEntries.size.`should equal`(4)
                progressEntries.forEach {
                    it.weekStart.daysBetween(it.weekEnd).`should equal`(7)
                    it.weekStart.dayOfWeek.`should equal`(DayOfWeek.SUNDAY)
                    it.weekEnd.dayOfWeek.`should equal`(DayOfWeek.SATURDAY)
                }
                progressEntries[0].weekStart.dayOfMonth.`should equal`(1)
                progressEntries[1].weekStart.dayOfMonth.`should equal`(8)
                progressEntries[2].weekStart.dayOfMonth.`should equal`(15)
                progressEntries[3].weekStart.dayOfMonth.`should equal`(22)
            }

            it("should return 6 periods") {
                val res = executeUseCase(
                    currentDate = LocalDate.of(2015, Month.MAY, 10),
                    firstDayOfWeek = DayOfWeek.SUNDAY,
                    lastDayOfWeek = DayOfWeek.SATURDAY
                )

                val progressEntries = res.monthlyGrowth.progressEntries
                progressEntries.size.`should equal`(6)
                progressEntries[0].weekStart.dayOfMonth.`should equal`(26)
                progressEntries[0].weekStart.month.`should equal`(Month.APRIL)
                progressEntries[5].weekEnd.dayOfMonth.`should equal`(6)
                progressEntries[5].weekEnd.month.`should equal`(Month.JUNE)
            }

            it("should return 5 periods") {
                val res = executeUseCase(
                    currentDate = LocalDate.of(2015, Month.DECEMBER, 10),
                    firstDayOfWeek = DayOfWeek.SUNDAY,
                    lastDayOfWeek = DayOfWeek.SATURDAY
                )

                val progressEntries = res.monthlyGrowth.progressEntries
                progressEntries.size.`should equal`(5)
            }

            it("should return 1 hour for first week") {
                val res = executeUseCase(
                    completedQuests = listOf(
                        completedQuest.copy(
                            timeRanges = listOf(
                                TimeRange(
                                    TimeRange.Type.COUNTDOWN,
                                    70
                                )
                            ),
                            completedAtDate = LocalDate.of(2015, Month.DECEMBER, 1)
                        )
                    ),
                    currentDate = LocalDate.of(2015, Month.DECEMBER, 10)
                )

                val progressEntries = res.monthlyGrowth.progressEntries
                progressEntries.first().productiveMinutes.`should equal`(10.minutes)
            }

            it("should return 1 hour for first week & second week") {
                val res = executeUseCase(
                    completedQuests = listOf(
                        completedQuest.copy(
                            timeRanges = listOf(
                                TimeRange(
                                    TimeRange.Type.COUNTDOWN,
                                    70
                                )
                            ),
                            completedAtDate = LocalDate.of(2015, Month.DECEMBER, 1)
                        ),
                        completedQuest.copy(
                            timeRanges = listOf(
                                TimeRange(
                                    TimeRange.Type.COUNTDOWN,
                                    140
                                )
                            ),
                            completedAtDate = LocalDate.of(2015, Month.DECEMBER, 8)
                        )
                    ),
                    currentDate = LocalDate.of(2015, Month.DECEMBER, 10)
                )

                val progressEntries = res.monthlyGrowth.progressEntries
                progressEntries[0].productiveMinutes.`should equal`(10.minutes)
                progressEntries[1].productiveMinutes.`should equal`(20.minutes)
                progressEntries[2].productiveMinutes.`should equal`(0.minutes)
                progressEntries[3].productiveMinutes.`should equal`(0.minutes)
            }
        }

        describe("TodayGrowth") {

            describe("Challenge progress") {

                it("should return for 1 completed Quest") {
                    val res = executeUseCase(
                        completedQuests = listOf(
                            completedQuest.copy(
                                timeRanges = listOf(
                                    TimeRange(
                                        TimeRange.Type.COUNTDOWN,
                                        70
                                    )
                                ),
                                challengeId = "123",
                                completedAtTime = Time.atHours(5)
                            )
                        )
                    )
                    val challengeProgress = res.todayGrowth.challengeProgress
                    challengeProgress.size.`should equal`(1)
                    val p = challengeProgress.first()
                    p.timeSpent.`should equal`(70.minutes)
                    p.progressPercent.`should equal`(100)
                    p.totalQuestCount.`should equal`(1)
                    p.completeQuestCount.`should equal`(1)
                }

                it("should return for 2 Quests from different Challenges") {
                    val res = executeUseCase(
                        completedQuests = listOf(
                            completedQuest.copy(
                                duration = 70,
                                challengeId = "123",
                                completedAtTime = Time.atHours(5)
                            ),
                            TestUtil.quest.copy(
                                challengeId = "1234"
                            )
                        )
                    )
                    val challengeProgress = res.todayGrowth.challengeProgress
                    challengeProgress.size.`should equal`(2)
                    val p = challengeProgress.first()
                    p.timeSpent.`should equal`(70.minutes)
                    p.progressPercent.`should equal`(100)
                    p.totalQuestCount.`should equal`(1)
                    p.completeQuestCount.`should equal`(1)

                    val p2 = challengeProgress[1]
                    p2.timeSpent.`should equal`(0.minutes)
                    p2.progressPercent.`should equal`(0)
                    p2.totalQuestCount.`should equal`(1)
                    p2.completeQuestCount.`should equal`(0)
                }
            }

            describe("progress entries") {

                it("should return 7") {
                    val res = executeUseCase()
                    res.todayGrowth.progressEntries.size.`should equal`(7)
                }

                it("should return 1 hour for first entry") {
                    val res = executeUseCase(
                        completedQuests = listOf(
                            completedQuest.copy(
                                timeRanges = listOf(
                                    TimeRange(
                                        TimeRange.Type.COUNTDOWN,
                                        70
                                    )
                                ),
                                completedAtTime = Time.atHours(5)
                            )
                        )
                    )
                    res.todayGrowth.progressEntries.first()
                        .productiveMinutes.`should equal`(70.minutes)
                }

                it("should return 1 hour for second entry") {
                    val res = executeUseCase(
                        completedQuests = listOf(
                            completedQuest.copy(
                                timeRanges = listOf(
                                    TimeRange(
                                        TimeRange.Type.COUNTDOWN,
                                        70
                                    )
                                ),
                                completedAtTime = Time.atHours(6)
                            )
                        )
                    )
                    res.todayGrowth.progressEntries[1].productiveMinutes.`should equal`(70.minutes)
                }

                it("should return cumulative productive minutes") {
                    val res = executeUseCase(
                        completedQuests = listOf(
                            completedQuest.copy(
                                timeRanges = listOf(
                                    TimeRange(
                                        TimeRange.Type.COUNTDOWN,
                                        70
                                    )
                                ),
                                completedAtTime = Time.atHours(5)
                            ),
                            completedQuest.copy(
                                timeRanges = listOf(
                                    TimeRange(
                                        TimeRange.Type.COUNTDOWN,
                                        70
                                    )
                                ),
                                completedAtTime = Time.atHours(6)
                            )
                        )
                    )
                    res.todayGrowth.progressEntries[1].productiveMinutes.`should equal`(140.minutes)
                }
            }
        }

        describe("tag time spent") {

            it("should be empty for not complete Quest") {
                val res = executeUseCase(
                    listOf(
                        TestUtil.quest.copy(
                            tags = listOf(
                                Tag(
                                    id = "123",
                                    name = "Wellness",
                                    color = Color.RED,
                                    icon = Icon.FLOWER,
                                    isFavorite = true
                                )
                            )
                        )
                    )
                )
                res.todayGrowth.tagProgress.`should be empty`()
            }

            it("should be empty for complete Quest without Tag") {
                executeUseCase(listOf(completedQuest)).todayGrowth.tagProgress.`should be empty`()
            }

            it("should be empty for completed Quest without tags") {
                executeUseCase(listOf(TestUtil.quest)).todayGrowth.tagProgress.`should be empty`()
            }

            it("should be 100% for completed Quest with Tag") {
                val res = executeUseCase(
                    listOf(
                        completedQuest.copy(
                            duration = 30,
                            tags = listOf(
                                Tag(
                                    id = "123",
                                    name = "Wellness",
                                    color = Color.RED,
                                    icon = Icon.FLOWER,
                                    isFavorite = true
                                )
                            )
                        )
                    )
                )

                val p = res.todayGrowth.tagProgress
                p.size.`should equal`(1)
                val tp = p.first()
                tp.timeSpent.`should equal`(30.minutes)
            }

            it("should be sorted based on time spent") {
                val res = executeUseCase(
                    listOf(
                        completedQuest.copy(
                            duration = 30,
                            tags = listOf(
                                Tag(
                                    id = "123",
                                    name = "Wellness",
                                    color = Color.RED,
                                    icon = Icon.FLOWER,
                                    isFavorite = true
                                )
                            )
                        ),
                        completedQuest.copy(
                            duration = 60,
                            tags = listOf(
                                Tag(
                                    id = "1234",
                                    name = "Work",
                                    color = Color.RED,
                                    icon = Icon.FLOWER,
                                    isFavorite = true
                                )
                            )
                        ),
                        completedQuest.copy(
                            duration = 15,
                            tags = listOf(
                                Tag(
                                    id = "12345",
                                    name = "Personal",
                                    color = Color.RED,
                                    icon = Icon.FLOWER,
                                    isFavorite = true
                                )
                            )
                        )
                    )
                )

                val p = res.todayGrowth.tagProgress
                p.size.`should equal`(3)
                val tp = p.first()
                tp.timeSpent.`should equal`(60.minutes)

                val tp1 = p[1]
                tp1.timeSpent.`should equal`(30.minutes)

                val tp2 = p[2]
                tp2.timeSpent.`should equal`(15.minutes)
            }

            it("should be contain percentage of total time") {
                val res = executeUseCase(
                    listOf(
                        completedQuest.copy(
                            duration = 30,
                            tags = listOf(
                                Tag(
                                    id = "123",
                                    name = "Wellness",
                                    color = Color.RED,
                                    icon = Icon.FLOWER,
                                    isFavorite = true
                                )
                            )
                        ),
                        completedQuest.copy(
                            duration = 60,
                            tags = listOf(
                                Tag(
                                    id = "1234",
                                    name = "Work",
                                    color = Color.RED,
                                    icon = Icon.FLOWER,
                                    isFavorite = true
                                )
                            )
                        ),
                        completedQuest.copy(
                            duration = 10,
                            tags = listOf(
                                Tag(
                                    id = "12345",
                                    name = "Personal",
                                    color = Color.RED,
                                    icon = Icon.FLOWER,
                                    isFavorite = true
                                )
                            )
                        )
                    )
                )

                val p = res.todayGrowth.tagProgress
                p.size.`should equal`(3)
                val tp = p.first()
                tp.timeSpentPercent.`should equal`(60)

                val tp1 = p[1]
                tp1.timeSpentPercent.`should equal`(30)

                val tp2 = p[2]
                tp2.timeSpentPercent.`should equal`(10)
            }
        }
    }
})