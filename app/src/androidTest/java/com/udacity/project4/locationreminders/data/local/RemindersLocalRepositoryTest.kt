package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {


    private lateinit var database: RemindersDatabase
    private lateinit var repository: RemindersLocalRepository

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private fun dataReminder(
        title: String = "title",
        description: String = "desc",
        location: String = "location",
        latitude: Double = 47.5456551,
        longitude: Double = 122.0101731

    ): ReminderDTO {
        return ReminderDTO(
            title = title,
            description = description,
            location = location,
            latitude = latitude,
            longitude = longitude
        )
    }


    @Before
    fun setupRepository() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun closeDatabase() {
        database.close()
    }


    @Test
    fun save_retrieve_equal() = runBlocking {

        val reminder = dataReminder()
        repository.saveReminder(reminder)
        val actualReminder: Result.Success<ReminderDTO> = repository.getReminder(reminder.id) as Result.Success
        assertThat(actualReminder.data, `is`(reminder))
    }

    @Test
    fun save_deleteAll_retrieve_exist() = runBlocking {
        val reminder = dataReminder("Gym", "Football gym", "America", 64.0, 20.2)

        repository.saveReminder(reminder)
        repository.deleteAllReminders()

        val actual = repository.getReminder(reminder.id) as Result.Error

        assertThat(actual.message, `is`("Reminder not found!"))

    }
}