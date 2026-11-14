package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(private val saveDao: SaveDao) {

    val playerSaveFlow: Flow<PlayerSaveEntity> = saveDao.observePlayerSave()
        .map { it ?: PlayerSaveEntity() }

    val controlConfigFlow: Flow<ControlConfigEntity> = saveDao.observeControlConfig()
        .map { it ?: ControlConfigEntity() }

    suspend fun ensureInitialized() {
        if (saveDao.getPlayerSaveOnce() == null) {
            saveDao.upsertPlayerSave(PlayerSaveEntity())
        }
        if (saveDao.getControlConfigOnce() == null) {
            saveDao.upsertControlConfig(ControlConfigEntity())
        }
    }

    suspend fun loadPlayerSave(): PlayerSaveEntity {
        ensureInitialized()
        return saveDao.getPlayerSaveOnce() ?: PlayerSaveEntity()
    }

    suspend fun savePlayerProgress(save: PlayerSaveEntity) {
        saveDao.upsertPlayerSave(save.copy(lastSavedEpochMs = System.currentTimeMillis()))
    }

    suspend fun resetCareer(): PlayerSaveEntity {
        val fresh = PlayerSaveEntity()
        saveDao.upsertPlayerSave(fresh)
        return fresh
    }

    suspend fun loadControlConfig(): ControlConfigEntity {
        ensureInitialized()
        return saveDao.getControlConfigOnce() ?: ControlConfigEntity()
    }

    suspend fun saveControlConfig(config: ControlConfigEntity) {
        saveDao.upsertControlConfig(config)
    }
}
