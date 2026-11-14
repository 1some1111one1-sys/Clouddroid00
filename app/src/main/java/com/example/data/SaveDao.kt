package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SaveDao {
    @Query("SELECT * FROM player_save WHERE id = 1")
    fun observePlayerSave(): Flow<PlayerSaveEntity?>

    @Query("SELECT * FROM player_save WHERE id = 1")
    suspend fun getPlayerSaveOnce(): PlayerSaveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayerSave(save: PlayerSaveEntity)

    @Query("SELECT * FROM control_config WHERE id = 1")
    fun observeControlConfig(): Flow<ControlConfigEntity?>

    @Query("SELECT * FROM control_config WHERE id = 1")
    suspend fun getControlConfigOnce(): ControlConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertControlConfig(config: ControlConfigEntity)
}
