package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.rule.CoroutineRule
import com.udacity.project4.locationreminders.util.getOrWaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(maxSdk = Build.VERSION_CODES.Q)
class RemindersListViewModelTest {

    @get:Rule
    var coroutineRule = CoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()


    private lateinit var data: FakeDataSource

    private lateinit var viewModel: RemindersListViewModel

    @Before
    fun initViewModel() {
        stopKoin()
        FirebaseApp.initializeApp(getApplicationContext())
        data = FakeDataSource()
        viewModel = RemindersListViewModel(getApplicationContext(), data)
    }

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
    fun mustReturnError() {
        coroutineRule.runBlockingTest {
            data.setReturnsError(true)
            viewModel.loadReminders()
            assertThat(
                viewModel.showSnackBar.getOrWaitValue(),
                Matchers.`is`(Matchers.notNullValue())
            )
        }
    }


    @Test
    fun testLoading() {

        coroutineRule.runBlockingTest {

            val reminder = dataReminder()
            data.saveReminder(reminder)

            coroutineRule.pauseDispatcher()

            viewModel.loadReminders()
            assertThat(viewModel.showLoading.getOrWaitValue(), Matchers.`is`(true))

            coroutineRule.resumeDispatcher()
            assertThat(
                viewModel.showLoading.getOrWaitValue(), Matchers.`is`(false)
            )
        }
    }

}