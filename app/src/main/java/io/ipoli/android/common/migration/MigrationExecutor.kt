package io.ipoli.android.common.migration

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import io.ipoli.android.Constants
import io.ipoli.android.player.data.Player

interface Migration {
    val fromVersion: Int
    val toVersion: Int

    suspend fun execute(database: FirebaseFirestore, playerId: String)
}

abstract class FirestoreMigration : Migration {

    protected fun playerRef(database: FirebaseFirestore, playerId: String) =
        database
            .collection("players")
            .document(playerId)

    protected fun playerCollectionRef(
        collection: String,
        database: FirebaseFirestore,
        playerId: String
    ) =
        database
            .collection("players")
            .document(playerId)
            .collection(collection)
}

class MigrationFrom100To101 : FirestoreMigration() {
    override val fromVersion = 100
    override val toVersion = 101

    override suspend fun execute(database: FirebaseFirestore, playerId: String) {
        val updateData = mutableMapOf(
            "schemaVersion" to Constants.SCHEMA_VERSION,
            "preferences.planDayStartMinute" to Constants.DEFAULT_PLAN_DAY_REMINDER_START_MINUTE,
            "preferences.planDays" to Constants.DEFAULT_PLAN_DAYS.map { it.name },
            "preferences.temperatureUnit" to Player.Preferences.TemperatureUnit.FAHRENHEIT.name
        )
        Tasks.await(
            playerRef(database, playerId)
                .update(updateData)
        )
    }
}


class MigrationFrom101To102 : FirestoreMigration() {

    override val fromVersion = 101
    override val toVersion = 102

    override suspend fun execute(database: FirebaseFirestore, playerId: String) {

        val tagDocs = Tasks.await(
            playerCollectionRef("tags", database, playerId)
                .get(Source.SERVER)
        ).documents

        val qDocs = Tasks.await(
            playerCollectionRef("quests", database, playerId)
                .get(Source.SERVER)
        ).documents

        val rqDocs = Tasks.await(
            playerCollectionRef("repeatingQuests", database, playerId)
                .get(Source.SERVER)
        ).documents

        val csDocs = Tasks.await(
            playerCollectionRef("challenges", database, playerId)
                .get(Source.SERVER)
        ).documents

        var batch = database.batch()
        var batchCount = 0

        for (qDoc in qDocs) {
            if (!qDoc.contains("tagIds")) {
                continue
            }

            val qRef = playerCollectionRef("quests", database, playerId)
                .document(qDoc.id)

            @Suppress("UNCHECKED_CAST")
            val tagIdsData = qDoc.get("tagIds") as Map<String, Any?>

            batch.update(qRef, createUpdateTagsData(tagIdsData, tagDocs))

            batchCount++

            if (batchCount == 500) {
                Tasks.await(batch.commit())
                batchCount = 0
                batch = database.batch()
            }
        }

        for (rqDoc in rqDocs) {
            if (!rqDoc.contains("tagIds")) {
                continue
            }

            val rqRef = playerCollectionRef("repeatingQuests", database, playerId)
                .document(rqDoc.id)

            @Suppress("UNCHECKED_CAST")
            val tagIdsData = rqDoc.get("tagIds") as Map<String, Any?>

            batch.update(rqRef, createUpdateTagsData(tagIdsData, tagDocs))

            batchCount++

            if (batchCount == 500) {
                Tasks.await(batch.commit())
                batchCount = 0
                batch = database.batch()
            }
        }

        for (cDoc in csDocs) {
            if (!cDoc.contains("tagIds")) {
                continue
            }

            val cRef = playerCollectionRef("challenges", database, playerId)
                .document(cDoc.id)

            @Suppress("UNCHECKED_CAST")
            val tagIdsData = cDoc.get("tagIds") as Map<String, Any?>

            batch.update(cRef, createUpdateTagsData(tagIdsData, tagDocs))

            batchCount++

            if (batchCount == 500) {
                Tasks.await(batch.commit())
                batchCount = 0
                batch = database.batch()
            }
        }

        batch.update(
            playerRef(database, playerId),
            mapOf("schemaVersion" to Constants.SCHEMA_VERSION)
        )

        Tasks.await(batch.commit())
    }

    private fun createUpdateTagsData(
        tIdData: Map<String, Any?>,
        tagDocs: List<DocumentSnapshot>
    ) =
        mutableMapOf(
            "tags" to tIdData.map {
                val tId = it.key
                val tDoc = tagDocs.first { it.id == tId }
                tId to mutableMapOf(
                    "id" to tId,
                    "name" to tDoc.getString("name"),
                    "color" to tDoc.getString("color"),
                    "icon" to tDoc.getString("icon"),
                    "isFavorite" to tDoc.getBoolean("isFavorite")
                )
            }.toMap(),
            "tagIds" to FieldValue.delete()
        )

}

class MigrationFrom102To103 : FirestoreMigration() {

    override val fromVersion = 102
    override val toVersion = 103

    override suspend fun execute(database: FirebaseFirestore, playerId: String) {

        val qDocs = Tasks.await(
            playerCollectionRef("quests", database, playerId)
                .get(Source.SERVER)
        ).documents

        val rqDocs = Tasks.await(
            playerCollectionRef("repeatingQuests", database, playerId)
                .get(Source.SERVER)
        ).documents

        val csDocs = Tasks.await(
            playerCollectionRef("challenges", database, playerId)
                .get(Source.SERVER)
        ).documents

        var batch = database.batch()
        var batchCount = 0

        for (qDoc in qDocs) {
            if (!qDoc.contains("tags")) {

                val qRef = playerCollectionRef("quests", database, playerId)
                    .document(qDoc.id)

                batch.update(qRef, mapOf("tags" to mapOf<String, Any?>()))

                batchCount++

                if (batchCount == 500) {
                    Tasks.await(batch.commit())
                    batchCount = 0
                    batch = database.batch()
                }
            }
        }

        for (rqDoc in rqDocs) {

            if (!rqDoc.contains("tags")) {

                val rqRef = playerCollectionRef("repeatingQuests", database, playerId)
                    .document(rqDoc.id)

                batch.update(rqRef, mapOf("tags" to mapOf<String, Any?>()))

                batchCount++

                if (batchCount == 500) {
                    Tasks.await(batch.commit())
                    batchCount = 0
                    batch = database.batch()
                }
            }
        }

        for (cDoc in csDocs) {
            if (!cDoc.contains("tags")) {

                val cRef = playerCollectionRef("challenges", database, playerId)
                    .document(cDoc.id)

                batch.update(cRef, mapOf("tags" to mapOf<String, Any?>()))

                batchCount++

                if (batchCount == 500) {
                    Tasks.await(batch.commit())
                    batchCount = 0
                    batch = database.batch()
                }
            }
        }

        batch.update(
            playerRef(database, playerId),
            mapOf("schemaVersion" to Constants.SCHEMA_VERSION)
        )

        Tasks.await(batch.commit())
    }

}

class MigrationFrom103To104 : FirestoreMigration() {

    override val fromVersion = 103

    override val toVersion = 104

    override suspend fun execute(database: FirebaseFirestore, playerId: String) {

        val qDocs = Tasks.await(
            playerCollectionRef("quests", database, playerId)
                .get(Source.SERVER)
        ).documents

        var batch = database.batch()
        var batchCount = 0

        for (qDoc in qDocs) {
            if (!qDoc.contains("createdAt")) {

                val qRef = playerCollectionRef("quests", database, playerId)
                    .document(qDoc.id)

                batch.update(qRef, mapOf("createdAt" to qDoc["updatedAt"]))

                batchCount++

                if (batchCount == 500) {
                    Tasks.await(batch.commit())
                    batchCount = 0
                    batch = database.batch()
                }
            }
        }

        batch.update(
            playerRef(database, playerId),
            mapOf("schemaVersion" to Constants.SCHEMA_VERSION)
        )
        Tasks.await(batch.commit())
    }
}

class MigrationFrom104To105 : FirestoreMigration() {
    override val fromVersion = 104

    override val toVersion = 105

    override suspend fun execute(database: FirebaseFirestore, playerId: String) {

        val batch = database.batch()
        batch.update(
            playerRef(database, playerId),
            mapOf(
                "bio" to null,
                "schemaVersion" to Constants.SCHEMA_VERSION,
                "preferences.isQuickDoNotificationEnabled" to Constants.DEFAULT_QUICK_DO_NOTIFICATION_ENABLED
            )
        )
        Tasks.await(batch.commit())
    }

}

class MigrationFrom105To106 : FirestoreMigration() {
    override val fromVersion = 105

    override val toVersion = 106

    override suspend fun execute(database: FirebaseFirestore, playerId: String) {

        val batch = database.batch()
        batch.update(
            playerRef(database, playerId),
            mapOf(
                "statistics" to mapOf<String, Any>(),
                "schemaVersion" to Constants.SCHEMA_VERSION,
                "achievements" to emptyList<Map<String, Any>>()
            )
        )
        Tasks.await(batch.commit())
    }

}

class MigrationException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

class MigrationExecutor(
    private val database: FirebaseFirestore,
    private val migrations: List<Migration>
) {

    fun shouldMigrate(playerSchemaVersion: Int) =
        playerSchemaVersion != Constants.SCHEMA_VERSION

    suspend fun migrate(playerSchemaVersion: Int, playerId: String) {
        if (!shouldMigrate(playerSchemaVersion)) {
            return
        }

        var currentSchemaVersion = playerSchemaVersion

        val sortedMigrations = migrations.sortedBy { it.fromVersion }

        for (m in sortedMigrations) {
            if (m.fromVersion == currentSchemaVersion) {
                try {
                    m.execute(database, playerId)
                } catch (e: Throwable) {
                    throw MigrationException(
                        "Unable to migrate Player $playerId from ${m.fromVersion} to ${m.toVersion}",
                        e
                    )
                }
                currentSchemaVersion = m.toVersion
            }
        }
    }
}