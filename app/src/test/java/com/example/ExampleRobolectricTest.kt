package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.ControlConfigEntity
import com.example.data.ControlPreset
import com.example.data.GameRepository
import com.example.data.PlayerSaveEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Vice Metro", appName)
    }

    @Test
    fun `offline room save and control config round trip`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = GameRepository(db.saveDao())

        val customSave = PlayerSaveEntity(cash = 4200, respectXp = 900, completedMissionsCsv = "m1_harbor_run")
        repo.savePlayerProgress(customSave)
        val loadedSave = repo.loadPlayerSave()
        assertEquals(4200, loadedSave.cash)
        assertEquals("m1_harbor_run", loadedSave.completedMissionsCsv)

        val customControls = ControlConfigEntity.forPreset(ControlPreset.LEFT_HANDED)
            .copy(buttonScale = 1.25f, lowPower30Fps = true)
        repo.saveControlConfig(customControls)
        val loadedControls = repo.loadControlConfig()
        assertEquals(ControlPreset.LEFT_HANDED.name, loadedControls.preset)
        assertEquals(1.25f, loadedControls.buttonScale, 0.01f)
        assertEquals(true, loadedControls.lowPower30Fps)
        db.close()
    }
}
