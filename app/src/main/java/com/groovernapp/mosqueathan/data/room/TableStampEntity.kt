package com.groovernapp.mosqueathan.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * When each table last changed, including when a row was *removed* from it.
 *
 * `MAX(lastModified)` over the rows cannot see a deletion — delete the newest running text and the
 * maximum goes down, or stays where the second-newest row left it, and a sync comparing the two
 * sides concludes nothing happened. It would then take the other side's copy and put the deleted
 * row back. That is the failure this table exists to prevent, and it is invisible in every test
 * that only ever adds and edits.
 *
 * One row per table, written by the repository on every mutation. Not migrated from Realm: at the
 * upgrade every table is stamped with the migration's own clock anyway.
 */
@Entity(tableName = "table_stamps")
data class TableStampEntity(
    @PrimaryKey
    val tableName: String = "",
    val lastModified: Long = 0L
)
