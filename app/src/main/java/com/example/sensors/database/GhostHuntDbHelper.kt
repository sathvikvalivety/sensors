package com.example.sensors.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.sensors.model.GhostHuntRecord

class GhostHuntDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "ghost_detector_3000.db"
        const val DATABASE_VERSION = 1

        const val TABLE_HUNTS = "ghost_hunts"
        const val COL_ID = "id"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_DURATION = "duration"
        const val COL_PEAK_SCORE = "peak_score"
        const val COL_LEVEL = "paranormal_level"
        const val COL_GHOST_NAME = "ghost_name"
        const val COL_GHOST_MOOD = "ghost_mood"
        const val COL_LOCATION = "location_tag"
        const val COL_EVIDENCE = "evidence_summary"
        const val COL_SENSOR_BREAKDOWN = "sensor_breakdown"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = """
            CREATE TABLE $TABLE_HUNTS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_DURATION TEXT NOT NULL,
                $COL_PEAK_SCORE INTEGER NOT NULL,
                $COL_LEVEL TEXT NOT NULL,
                $COL_GHOST_NAME TEXT NOT NULL,
                $COL_GHOST_MOOD TEXT NOT NULL,
                $COL_LOCATION TEXT NOT NULL,
                $COL_EVIDENCE TEXT NOT NULL,
                $COL_SENSOR_BREAKDOWN TEXT NOT NULL
            )
        """.trimIndent()
        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_HUNTS")
        onCreate(db)
    }

    fun insertHunt(record: GhostHuntRecord): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TIMESTAMP, record.timestamp)
            put(COL_DURATION, record.durationFormatted)
            put(COL_PEAK_SCORE, record.peakScore)
            put(COL_LEVEL, record.paranormalLevel)
            put(COL_GHOST_NAME, record.ghostName)
            put(COL_GHOST_MOOD, record.ghostMood)
            put(COL_LOCATION, record.locationTag)
            put(COL_EVIDENCE, record.evidenceSummary)
            put(COL_SENSOR_BREAKDOWN, record.sensorBreakdown)
        }
        return db.insert(TABLE_HUNTS, null, values)
    }

    fun getAllHunts(): List<GhostHuntRecord> {
        val list = mutableListOf<GhostHuntRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HUNTS,
            null,
            null,
            null,
            null,
            null,
            "$COL_TIMESTAMP DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val record = GhostHuntRecord(
                    id = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                    timestamp = it.getLong(it.getColumnIndexOrThrow(COL_TIMESTAMP)),
                    durationFormatted = it.getString(it.getColumnIndexOrThrow(COL_DURATION)),
                    peakScore = it.getInt(it.getColumnIndexOrThrow(COL_PEAK_SCORE)),
                    paranormalLevel = it.getString(it.getColumnIndexOrThrow(COL_LEVEL)),
                    ghostName = it.getString(it.getColumnIndexOrThrow(COL_GHOST_NAME)),
                    ghostMood = it.getString(it.getColumnIndexOrThrow(COL_GHOST_MOOD)),
                    locationTag = it.getString(it.getColumnIndexOrThrow(COL_LOCATION)),
                    evidenceSummary = it.getString(it.getColumnIndexOrThrow(COL_EVIDENCE)),
                    sensorBreakdown = it.getString(it.getColumnIndexOrThrow(COL_SENSOR_BREAKDOWN))
                )
                list.add(record)
            }
        }
        return list
    }

    fun getHuntCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_HUNTS", null)
        var count = 0
        cursor.use {
            if (it.moveToFirst()) {
                count = it.getInt(0)
            }
        }
        return count
    }

    fun getHighestScore(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT MAX($COL_PEAK_SCORE) FROM $TABLE_HUNTS", null)
        var maxScore = 0
        cursor.use {
            if (it.moveToFirst()) {
                maxScore = it.getInt(0)
            }
        }
        return maxScore
    }

    fun deleteHunt(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_HUNTS, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun clearAllHunts(): Int {
        val db = writableDatabase
        return db.delete(TABLE_HUNTS, null, null)
    }
}
