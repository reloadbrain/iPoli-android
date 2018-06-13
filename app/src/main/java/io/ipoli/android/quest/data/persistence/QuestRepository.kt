package io.ipoli.android.quest.data.persistence

import android.content.SharedPreferences
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import io.ipoli.android.common.datetime.*
import io.ipoli.android.common.persistence.BaseCollectionFirestoreRepository
import io.ipoli.android.common.persistence.CollectionRepository
import io.ipoli.android.common.persistence.FirestoreModel
import io.ipoli.android.dailychallenge.data.DailyChallenge
import io.ipoli.android.pet.Food
import io.ipoli.android.quest.*
import io.ipoli.android.quest.subquest.SubQuest
import io.ipoli.android.tag.Tag
import kotlinx.coroutines.experimental.channels.Channel
import org.threeten.bp.*
import java.util.concurrent.ExecutorService
import kotlin.coroutines.experimental.CoroutineContext

interface QuestRepository : CollectionRepository<Quest> {

    suspend fun listenForScheduledBetween(
        startDate: LocalDate,
        endDate: LocalDate,
        channel: Channel<List<Quest>>
    ): Channel<List<Quest>>

    suspend fun listenForScheduledAt(
        date: LocalDate,
        channel: Channel<List<Quest>>
    ): Channel<List<Quest>>

    suspend fun listenByTag(tagId: String, channel: Channel<List<Quest>>): Channel<List<Quest>>

    suspend fun listenForAllUnscheduled(channel: Channel<List<Quest>>): Channel<List<Quest>>

    fun findRandomUnscheduled(count: Int): List<Quest>

    fun findScheduledAt(date: LocalDate): List<Quest>

    fun findScheduledForRepeatingQuestBetween(
        repeatingQuestId: String,
        start: LocalDate,
        end: LocalDate
    ): List<Quest>

    fun findServerScheduledForRepeatingQuestBetween(
        repeatingQuestId: String,
        start: LocalDate,
        end: LocalDate
    ): List<Quest>

    fun findNextReminderTime(afterTime: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())): LocalDateTime?

    fun findQuestsToRemind(remindTime: LocalDateTime): List<Quest>
    fun findCompletedForDate(date: LocalDate): List<Quest>
    fun findStartedQuests(): List<Quest>
    fun findLastScheduledDate(currentDate: LocalDate, maxQuests: Int): LocalDate?
    fun findFirstScheduledDate(currentDate: LocalDate, maxQuests: Int): LocalDate?
    fun findNextScheduledNotCompletedForRepeatingQuest(
        repeatingQuestId: String,
        currentDate: LocalDate
    ): Quest?

    fun findAllForRepeatingQuest(
        repeatingQuestId: String,
        includeRemoved: Boolean = true
    ): List<Quest>

    fun findOriginalScheduledForRepeatingQuestAtDate(
        repeatingQuestId: String,
        currentDate: LocalDate
    ): Quest?

    fun findCompletedCountForRepeatingQuestInPeriod(
        repeatingQuestId: String,
        start: LocalDate,
        end: LocalDate?
    ): Int

    fun findCompletedForRepeatingQuestInPeriod(
        repeatingQuestId: String,
        start: LocalDate,
        end: LocalDate? = null
    ): List<Quest>

    fun purgeAllNotCompletedForRepeating(
        repeatingQuestId: String,
        startDate: LocalDate = LocalDate.now()
    )

    fun findNotCompletedNotForChallengeNotRepeating(
        challengeId: String,
        start: LocalDate = LocalDate.now()
    ): List<Quest>

    fun findAllForChallengeNotRepeating(challengeId: String): List<Quest>

    fun findAllForChallenge(challengeId: String): List<Quest>
    fun findAllForRepeatingQuestAfterDate(
        repeatingQuestId: String,
        includeRemoved: Boolean,
        currentDate: LocalDate = LocalDate.now()
    ): List<Quest>

    fun findCountForTag(tagId: String): Int

    fun findByTag(tagId: String): List<Quest>
    fun findByTagWithRemoved(tagId: String): List<Quest>

    fun findQuestsForDailyChallenge(dailyChallenge: DailyChallenge): List<Quest>

    fun findOriginallyScheduledOrCompletedInPeriod(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Quest>

    fun findCompletedInPeriod(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Quest>

    fun purge(questIds: List<String>)
}

data class DbQuest(override val map: MutableMap<String, Any?> = mutableMapOf()) :
    FirestoreModel {
    override var id: String by map
    var name: String by map
    var color: String by map
    var icon: String? by map
    var tags: Map<String, MutableMap<String, Any?>> by map
    var duration: Long by map
    var priority: String by map
    var preferredStartTime: String by map
    var reminders: List<MutableMap<String, Any?>> by map
    var startMinute: Long? by map
    var experience: Long? by map
    var coins: Long? by map
    var bounty: MutableMap<String, Any?>? by map
    var startDate: Long? by map
    var dueDate: Long? by map
    var scheduledDate: Long? by map
    var originalScheduledDate: Long? by map
    var completedAtDate: Long? by map
    var completedAtMinute: Long? by map
    var subQuests: List<MutableMap<String, Any?>> by map
    var timeRanges: List<MutableMap<String, Any?>> by map
    var timeRangeCount: Long by map
    var repeatingQuestId: String? by map
    var challengeId: String? by map
    var note: String by map
    override var createdAt: Long by map
    override var updatedAt: Long by map
    override var removedAt: Long? by map
}

data class DbSubQuest(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var name: String by map
    var completedAtDate: Long? by map
    var completedAtMinute: Long? by map
}

data class DbReminder(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var type: String by map
    var message: String by map
    var minute: Long? by map
    var date: Long? by map
    var minutesFromStart: Long? by map

    enum class Type {
        RELATIVE, FIXED
    }
}

data class DbBounty(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var type: String by map
    var name: String? by map

    enum class Type {
        NONE, FOOD
    }
}

data class DbTimeRange(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var type: String by map
    var duration: Long by map
    var start: Long? by map
    var end: Long? by map
}

class FirestoreQuestRepository(
    database: FirebaseFirestore,
    coroutineContext: CoroutineContext,
    sharedPreferences: SharedPreferences,
    executor: ExecutorService
) : BaseCollectionFirestoreRepository<Quest, DbQuest>(
    database,
    coroutineContext,
    sharedPreferences,
    executor
), QuestRepository {

    override fun findAllForRepeatingQuestAfterDate(
        repeatingQuestId: String,
        includeRemoved: Boolean,
        currentDate: LocalDate
    ): List<Quest> {
        val query = collectionReference
            .whereEqualTo("repeatingQuestId", repeatingQuestId)
            .whereGreaterThanOrEqualTo("scheduledDate", currentDate.startOfDayUTC())
        return if (includeRemoved)
            toEntityObjects(query.documents)
        else
            query.notRemovedEntities
    }

    override fun findOriginallyScheduledOrCompletedInPeriod(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Quest> {
        val scheduled = collectionReference
            .whereGreaterThanOrEqualTo("originalScheduledDate", startDate.startOfDayUTC())
            .whereLessThanOrEqualTo("originalScheduledDate", endDate.startOfDayUTC())
            .notRemovedEntities

        val completed = collectionReference
            .whereGreaterThanOrEqualTo("completedAtDate", startDate.startOfDayUTC())
            .whereLessThanOrEqualTo("completedAtDate", endDate.startOfDayUTC())
            .notRemovedEntities

        return (scheduled + completed).toSet().toList()
    }

    override fun findCompletedInPeriod(startDate: LocalDate, endDate: LocalDate): List<Quest> =
        collectionReference
            .whereGreaterThanOrEqualTo("completedAtDate", startDate.startOfDayUTC())
            .whereLessThanOrEqualTo("completedAtDate", endDate.startOfDayUTC())
            .notRemovedEntities

    /**
     * Includes removed Quests
     */
    override fun findAllForChallenge(challengeId: String) =
        toEntityObjects(
            collectionReference
                .whereEqualTo("challengeId", challengeId)
                .documents
        )

    override fun findNotCompletedNotForChallengeNotRepeating(
        challengeId: String,
        start: LocalDate
    ): List<Quest> {
        val quests = collectionReference
            .whereEqualTo("repeatingQuestId", null)
            .whereEqualTo("completedAtDate", null)
            .whereGreaterThanOrEqualTo("scheduledDate", start.startOfDayUTC()).notRemovedEntities

        return quests.filter { it.challengeId != challengeId }
    }

    override fun findAllForRepeatingQuest(
        repeatingQuestId: String,
        includeRemoved: Boolean
    ): List<Quest> {

        val query = collectionReference.whereEqualTo("repeatingQuestId", repeatingQuestId)
        return if (includeRemoved)
            toEntityObjects(query.documents)
        else
            query.notRemovedEntities
    }

    override fun findQuestsForDailyChallenge(dailyChallenge: DailyChallenge): List<Quest> {
        if (dailyChallenge.questIds.isEmpty()) {
            return emptyList()
        }
        return dailyChallenge.questIds.map {
            findById(it)!!
        }.filter { !it.isRemoved }
    }


    override fun purgeAllNotCompletedForRepeating(
        repeatingQuestId: String,
        startDate: LocalDate
    ) =
        collectionReference
            .whereEqualTo("repeatingQuestId", repeatingQuestId)
            .whereEqualTo("completedAtDate", null)
            .whereGreaterThanOrEqualTo("scheduledDate", startDate.startOfDayUTC())
            .documents
            .map { it.id }
            .let { purge(it) }

    override fun findAllForChallengeNotRepeating(challengeId: String) =
        collectionReference
            .whereEqualTo("challengeId", challengeId)
            .whereEqualTo("repeatingQuestId", null)
            .notRemovedEntities


    override fun findCompletedForRepeatingQuestInPeriod(
        repeatingQuestId: String,
        start: LocalDate,
        end: LocalDate?
    ) = createCompletedForRepeatingInPeriodQuery(repeatingQuestId, start, end).notRemovedEntities

    override fun findCompletedCountForRepeatingQuestInPeriod(
        repeatingQuestId: String,
        start: LocalDate,
        end: LocalDate?
    ) = createCompletedForRepeatingInPeriodQuery(repeatingQuestId, start, end).documents.size

    private fun createCompletedForRepeatingInPeriodQuery(
        repeatingQuestId: String,
        start: LocalDate,
        end: LocalDate?
    ): Query {
        var ref = collectionReference
            .whereEqualTo("repeatingQuestId", repeatingQuestId)
            .whereGreaterThanOrEqualTo("completedAtDate", start.startOfDayUTC())
        if (end != null) {
            ref = ref.whereLessThanOrEqualTo("completedAtDate", end.startOfDayUTC())
        }
        return ref
    }

    override fun findNextScheduledNotCompletedForRepeatingQuest(
        repeatingQuestId: String,
        currentDate: LocalDate
    ) =
        collectionReference
            .whereEqualTo("repeatingQuestId", repeatingQuestId)
            .whereGreaterThanOrEqualTo("scheduledDate", currentDate.startOfDayUTC())
            .whereEqualTo("completedAtDate", null)
            .orderBy("scheduledDate", Query.Direction.ASCENDING)
            .limit(1)
            .notRemovedEntities.firstOrNull()

    override fun findOriginalScheduledForRepeatingQuestAtDate(
        repeatingQuestId: String,
        currentDate: LocalDate
    ): Quest? {
        val doc = collectionReference
            .whereEqualTo("repeatingQuestId", repeatingQuestId)
            .whereEqualTo("originalScheduledDate", currentDate.startOfDayUTC())
            .limit(1)
            .execute().documents
        if (doc.isEmpty()) {
            return null
        }
        return toEntityObject(doc.first().data!!)
    }

    override suspend fun listenByTag(
        tagId: String,
        channel: Channel<List<Quest>>
    ) =
        collectionReference
            .whereEqualTo("tags.$tagId.id", tagId)
            .listenForChanges(channel)

    override suspend fun listenForAllUnscheduled(channel: Channel<List<Quest>>) =
        collectionReference
            .whereEqualTo("scheduledDate", null)
            .orderBy("dueDate", Query.Direction.ASCENDING)
            .listenForChanges(channel)

    override fun findByTag(tagId: String) =
        collectionReference
            .whereEqualTo("tags.$tagId.id", tagId)
            .notRemovedEntities

    override fun findByTagWithRemoved(tagId: String) =
        collectionReference
            .whereEqualTo("tags.$tagId.id", tagId)
            .entities

    override fun findCountForTag(tagId: String): Int =
        collectionReference
            .whereEqualTo("tags.$tagId.id", tagId)
            .whereEqualTo("completedAtDate", null)
            .documents.size

    override suspend fun listenForScheduledBetween(
        startDate: LocalDate,
        endDate: LocalDate,
        channel: Channel<List<Quest>>
    ) =
        collectionReference
            .whereGreaterThanOrEqualTo("scheduledDate", startDate.startOfDayUTC())
            .whereLessThanOrEqualTo("scheduledDate", endDate.startOfDayUTC())
            .orderBy("scheduledDate")
            .orderBy("startMinute")
            .listenForChanges(channel)

    override fun findRandomUnscheduled(count: Int) =
        collectionReference
            .whereEqualTo("scheduledDate", null)
            .whereEqualTo("completedAtDate", null)
            .notRemovedEntities
            .shuffled()
            .take(count)

    override fun findScheduledAt(date: LocalDate) =
        collectionReference
            .whereGreaterThan("scheduledDate", date.startOfDayUTC() - 1)
            .whereLessThanOrEqualTo("scheduledDate", date.startOfDayUTC())
            .orderBy("scheduledDate")
            .orderBy("startMinute")
            .notRemovedEntities

    override fun findScheduledForRepeatingQuestBetween(
        repeatingQuestId: String,
        start: LocalDate,
        end: LocalDate
    ) =
        collectionReference
            .whereEqualTo("repeatingQuestId", repeatingQuestId)
            .whereGreaterThan("scheduledDate", start.startOfDayUTC() - 1)
            .whereLessThanOrEqualTo("scheduledDate", end.startOfDayUTC())
            .entities

    override fun findServerScheduledForRepeatingQuestBetween(
        repeatingQuestId: String,
        start: LocalDate,
        end: LocalDate
    ) =
        toEntityObjects(collectionReference
            .whereEqualTo("repeatingQuestId", repeatingQuestId)
            .whereGreaterThan("scheduledDate", start.startOfDayUTC() - 1)
            .whereLessThanOrEqualTo("scheduledDate", end.startOfDayUTC())
            .serverDocuments)

    override suspend fun listenForScheduledAt(
        date: LocalDate,
        channel: Channel<List<Quest>>
    ) =
        collectionReference
            .whereEqualTo("scheduledDate", date.startOfDayUTC())
            .orderBy("startMinute")
            .listenForChanges(channel)

    override fun findNextReminderTime(afterTime: ZonedDateTime): LocalDateTime? {

        val currentDateMillis = afterTime.toLocalDate().startOfDayUTC()

        val millisOfDay = afterTime.toLocalTime().toSecondOfDay().seconds.millisValue

        val query =
            remindersReference
                .orderBy("date")
                .orderBy("millisOfDay")
                .startAt(
                    currentDateMillis,
                    millisOfDay + 1
                )
                .limit(1)

        val documents = query.serverDocuments
        if (documents.isEmpty()) {
            return null
        }

        val reminder = documents[0]

        val remindDate = (reminder.get("date") as Long).startOfDayUTC
        val remindMillis = reminder.get("millisOfDay") as Long
        return LocalDateTime.of(
            remindDate,
            LocalTime.ofSecondOfDay(remindMillis.milliseconds.asSeconds.longValue)
        )
    }

    override fun findQuestsToRemind(remindTime: LocalDateTime): List<Quest> {
        val query = remindersReference
            .whereEqualTo("date", remindTime.toLocalDate().startOfDayUTC())
            .whereEqualTo(
                "millisOfDay", remindTime.toLocalTime().toSecondOfDay().seconds.millisValue
            )
        val documents = query.serverDocuments
        if (documents.isEmpty()) {
            return listOf()
        }
        val questIds = documents.map {
            it["questId"]
        }

        var questRef: Query = collectionReference
        questIds.forEach {
            questRef = questRef.whereEqualTo("id", it)
        }
        return questRef.notRemovedEntities
    }

    override fun findStartedQuests(): List<Quest> {
        val query = collectionReference
            .whereEqualTo("completedAtDate", null)
            .whereGreaterThan("timeRangeCount", 0)
        return query.notRemovedEntities
    }

    override fun findCompletedForDate(date: LocalDate): List<Quest> {
        val query = collectionReference
            // Due to Firestore bug (kinda) we can't query using the same value as data
            // see https://stackoverflow.com/a/47379643/6336582
            .whereGreaterThan("completedAtDate", date.startOfDayUTC() - 1)
            .whereLessThanOrEqualTo("completedAtDate", date.startOfDayUTC())
        return query.notRemovedEntities
    }

    override fun findLastScheduledDate(currentDate: LocalDate, maxQuests: Int): LocalDate? {
        val endDateQuery = collectionReference
            .whereGreaterThan("scheduledDate", currentDate.startOfDayUTC())
            .limit(maxQuests.toLong())
            .orderBy("scheduledDate", Query.Direction.ASCENDING)
        val endDateQuests = endDateQuery.notRemovedEntities

        if (endDateQuests.isEmpty()) {
            return null
        }

        return endDateQuests.last().scheduledDate
    }

    override fun findFirstScheduledDate(currentDate: LocalDate, maxQuests: Int): LocalDate? {
        val startDateQuery = collectionReference
            .whereLessThan("scheduledDate", currentDate.startOfDayUTC())
            .limit(maxQuests.toLong())
            .orderBy("scheduledDate", Query.Direction.DESCENDING)
        val startDateQuests = startDateQuery.notRemovedEntities

        if (startDateQuests.isEmpty()) {
            return null
        }

        return startDateQuests.last().scheduledDate
    }

    override val collectionReference: CollectionReference
        get() {
            return database.collection("players").document(playerId).collection("quests")
        }

    private val remindersReference
        get() = database.collection("players").document(playerId).collection("questReminders")

    override fun save(entity: Quest): Quest {
        val quest = super.save(entity)
        saveReminders(quest, quest.reminders)
        return quest
    }

    override fun save(entities: List<Quest>): List<Quest> {
        val quests = super.save(entities)

        val batch = database.batch()

        val questToReminder = quests.map { q -> q.reminders.map { Pair(q, it) } }.flatten()

        val questIds = quests.map { it.id }
        batch.commit()

        bulkPurgeReminders(questIds, questToReminder)
        return quests
    }

    private fun bulkPurgeReminders(
        questIds: List<String>,
        questToReminder: List<Pair<Quest, Reminder>>
    ) {

        purgeReminders(questIds)

        val batch = database.batch()
        questToReminder.forEach {
            if (!it.first.isCompleted) {
                createReminderData(it.second, it.first)?.let {
                    val ref = remindersReference.document()
                    batch.set(ref, it)
                }
            }
        }
        batch.commit()

    }

    private fun purgeReminders(
        questIds: List<String>
    ) {
        val batch = database.batch()
        var allRemindersQuery: Query = remindersReference

        questIds.forEach {
            allRemindersQuery = allRemindersQuery.whereEqualTo("questId", it)
        }

        allRemindersQuery.serverDocuments.forEach {
            val ref = remindersReference.document(it.id)
            batch.delete(ref)
        }
        batch.commit()
    }

    private fun saveReminders(quest: Quest, reminders: List<Reminder>) {
        purgeQuestReminders(quest.id)
        if (!quest.isCompleted) {
            addReminders(reminders, quest)
        }
    }

    private fun addReminders(
        reminders: List<Reminder>,
        quest: Quest
    ) {
        reminders.forEach {
            createReminderData(it, quest)?.let {
                remindersReference.add(it)
            }
        }
    }

    private fun createReminderData(reminder: Reminder, quest: Quest) =
        when (reminder) {
            is Reminder.Fixed ->
                createReminderData(quest.id, reminder.date, reminder.time)
            is Reminder.Relative ->
                if (quest.isScheduled) {

                    val questDateTime =
                        LocalDateTime.of(
                            quest.scheduledDate!!,
                            LocalTime.of(quest.startTime!!.hours, quest.startTime.getMinutes())
                        )
                    val reminderDateTime =
                        questDateTime.minusMinutes(reminder.minutesFromStart)
                    val toLocalTime = reminderDateTime.toLocalTime()

                    createReminderData(
                        quest.id,
                        reminderDateTime.toLocalDate(),
                        Time.at(toLocalTime.hour, toLocalTime.minute)
                    )
                } else null

        }

    private fun createReminderData(
        questId: String,
        date: LocalDate,
        time: Time
    ) =
        mapOf(
            "questId" to questId,
            "date" to date.startOfDayUTC(),
            "millisOfDay" to time.toMillisOfDay()
        )

    private fun purgeQuestReminders(questId: String) {
        val batch = database.batch()

        val query = remindersReference.whereEqualTo("questId", questId)
        query.serverDocuments.forEach {
            val ref = remindersReference.document(it.id)
            batch.delete(ref)
        }
        batch.commit()
    }

    override fun remove(id: String) {
        super.remove(id)
        purgeQuestReminders(id)
    }

    override fun undoRemove(id: String) {
        super.undoRemove(id)
        val quest = findById(id)!!
        if (quest.reminders.isNotEmpty()) {
            addReminders(quest.reminders, quest)
        }
    }

    override fun purge(questIds: List<String>) {
        val batch = database.batch()
        questIds.forEach {
            val ref = collectionReference.document(it)
            batch.delete(ref)
        }

        batch.commit()

        purgeReminders(questIds)
    }

    override fun toEntityObject(dataMap: MutableMap<String, Any?>): Quest {
        val cq = DbQuest(dataMap.withDefault {
            null
        })

        return Quest(
            id = cq.id,
            name = cq.name,
            color = Color.valueOf(cq.color),
            icon = cq.icon?.let {
                Icon.valueOf(it)
            },
            tags = cq.tags.values.map {
                createTag(it)
            },
            startDate = cq.startDate?.startOfDayUTC,
            dueDate = cq.dueDate?.startOfDayUTC,
            scheduledDate = cq.scheduledDate?.startOfDayUTC,
            originalScheduledDate = cq.originalScheduledDate?.startOfDayUTC,
            startTime = cq.startMinute?.let { Time.of(it.toInt()) },
            duration = cq.duration.toInt(),
            priority = Priority.valueOf(cq.priority),
            preferredStartTime = TimePreference.valueOf(cq.preferredStartTime),
            experience = cq.experience?.toInt(),
            coins = cq.coins?.toInt(),
            bounty = cq.bounty?.let {
                val cr = DbBounty(it)
                when {
                    cr.type == DbBounty.Type.NONE.name -> Quest.Bounty.None
                    cr.type == DbBounty.Type.FOOD.name -> Quest.Bounty.Food(Food.valueOf(cr.name!!))
                    else -> null
                }
            },
            completedAtDate = cq.completedAtDate?.startOfDayUTC,
            completedAtTime = cq.completedAtMinute?.let {
                Time.of(it.toInt())
            },
            reminders = cq.reminders.map {
                val cr = DbReminder(it)
                val type = DbReminder.Type.valueOf(cr.type)
                when (type) {
                    DbReminder.Type.RELATIVE ->
                        Reminder.Relative(cr.message, cr.minutesFromStart!!.toLong())

                    DbReminder.Type.FIXED ->
                        Reminder.Fixed(
                            cr.message,
                            cr.date!!.startOfDayUTC,
                            Time.of(cr.minute!!.toInt())
                        )
                }

            },
            subQuests = cq.subQuests.map {
                val dsq = DbSubQuest(it)
                SubQuest(
                    name = dsq.name,
                    completedAtDate = dsq.completedAtDate?.startOfDayUTC,
                    completedAtTime = dsq.completedAtMinute?.let { Time.of(it.toInt()) }
                )
            },
            timeRanges = cq.timeRanges.map {
                val ctr = DbTimeRange(it)
                TimeRange(
                    TimeRange.Type.valueOf(ctr.type),
                    ctr.duration.toInt(),
                    ctr.start?.instant,
                    ctr.end?.instant
                )
            },
            repeatingQuestId = cq.repeatingQuestId,
            challengeId = cq.challengeId,
            note = cq.note,
            createdAt = Instant.ofEpochMilli(cq.createdAt),
            updatedAt = Instant.ofEpochMilli(cq.updatedAt)
        )
    }

    override fun toDatabaseObject(entity: Quest): DbQuest {
        val q = DbQuest()
        q.id = entity.id
        q.name = entity.name
        q.tags = entity.tags.map { it.id to createDbTag(it).map }.toMap()
        q.color = entity.color.name
        q.icon = entity.icon?.name
        q.duration = entity.duration.toLong()
        q.priority = entity.priority.name
        q.preferredStartTime = entity.preferredStartTime.name
        q.startDate = entity.startDate?.startOfDayUTC()
        q.dueDate = entity.dueDate?.startOfDayUTC()
        q.scheduledDate = entity.scheduledDate?.startOfDayUTC()
        q.originalScheduledDate = entity.originalScheduledDate?.startOfDayUTC()
        q.reminders = entity.reminders.map {
            createDbReminder(it).map
        }
        q.subQuests = entity.subQuests.map {
            DbSubQuest().apply {
                name = it.name
                completedAtDate = it.completedAtDate?.startOfDayUTC()
                completedAtMinute = it.completedAtTime?.toMinuteOfDay()?.toLong()
            }.map
        }
        q.experience = entity.experience?.toLong()
        q.coins = entity.coins?.toLong()
        q.bounty = entity.bounty?.let {
            val cr = DbBounty()

            cr.type = when (it) {
                Quest.Bounty.None -> DbBounty.Type.NONE.name
                is Quest.Bounty.Food -> DbBounty.Type.FOOD.name
            }

            if (it is Quest.Bounty.Food) {
                cr.name = it.food.name
            }
            cr.map
        }
        q.startMinute = entity.startTime?.toMinuteOfDay()?.toLong()
        q.completedAtDate = entity.completedAtDate?.startOfDayUTC()
        q.completedAtMinute = entity.completedAtTime?.toMinuteOfDay()?.toLong()
        q.timeRanges = entity.timeRanges.map {
            createDbTimeRange(it).map
        }
        q.timeRangeCount = q.timeRanges.size.toLong()
        q.repeatingQuestId = entity.repeatingQuestId
        q.challengeId = entity.challengeId
        q.note = entity.note
        q.createdAt = entity.createdAt.toEpochMilli()
        q.updatedAt = entity.updatedAt.toEpochMilli()
        return q
    }

    private fun createDbTimeRange(timeRange: TimeRange): DbTimeRange {
        val cTimeRange = DbTimeRange()
        cTimeRange.type = timeRange.type.name
        cTimeRange.duration = timeRange.duration.toLong()
        cTimeRange.start = timeRange.start?.toEpochMilli()
        cTimeRange.end = timeRange.end?.toEpochMilli()
        return cTimeRange
    }

    private fun createDbReminder(reminder: Reminder): DbReminder {
        val cr = DbReminder()
        cr.message = reminder.message
        when (reminder) {

            is Reminder.Fixed -> {
                cr.type = DbReminder.Type.FIXED.name
                cr.date = reminder.date.startOfDayUTC()
                cr.minute = reminder.time.toMinuteOfDay().toLong()
            }

            is Reminder.Relative -> {
                cr.type = DbReminder.Type.RELATIVE.name
                cr.minutesFromStart = reminder.minutesFromStart
            }
        }
        return cr
    }

    private fun createDbTag(tag: Tag) =
        DbEmbedTag().apply {
            id = tag.id
            name = tag.name
            isFavorite = tag.isFavorite
            color = tag.color.name
            icon = tag.icon?.name
        }

    private fun createTag(dataMap: MutableMap<String, Any?>) =
        with(
            DbEmbedTag(dataMap.withDefault {
                null
            })
        ) {
            Tag(
                id = id,
                name = name,
                color = Color.valueOf(color),
                icon = icon?.let {
                    Icon.valueOf(it)
                },
                isFavorite = isFavorite
            )
        }
}

class DbEmbedTag(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var id: String by map
    var name: String by map
    var color: String by map
    var icon: String? by map
    var isFavorite: Boolean by map
}