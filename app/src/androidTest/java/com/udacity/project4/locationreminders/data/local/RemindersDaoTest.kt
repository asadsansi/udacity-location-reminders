package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var database: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()


    @Before
    fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDatabase() = database.close()



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

    @Test
    fun save_retrieveById_equal() = runBlocking {

        val reminder = dataReminder()
        database.reminderDao().saveReminder(reminder)
        val actualReminder: ReminderDTO? = database.reminderDao().getReminderById(reminder.id)
        assertThat(actualReminder, `is`(reminder))
    }

    @Test
    fun save_deleteAll_exist() = runBlocking {
        val reminder = dataReminder("School", "Children school", "Canada", 54.0, 20.2)
        val id = reminder.id
        database.reminderDao().saveReminder(reminder)
        database.reminderDao().deleteAllReminders()

        assertThat(database.reminderDao().getReminderById(id), `is`(CoreMatchers.nullValue()))
    }


}
