package com.udacity.project4.locationreminders.savereminder


import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
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


@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(maxSdk = Build.VERSION_CODES.Q)
class SaveReminderViewModelTest {

    @get:Rule
    var coroutineRule = CoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()


    private lateinit var data: FakeDataSource
    private lateinit var viewModel: SaveReminderViewModel

    @Before
    fun initViewModel() {
        stopKoin()
        FirebaseApp.initializeApp(getApplicationContext())
        data = FakeDataSource()
        viewModel = SaveReminderViewModel(getApplicationContext(), data)
    }

    private fun getReminderDataItem(
        title: String?,
        description: String = "desc",
        location: String = "location",
        latitude: Double = 47.5456551,
        longitude: Double = 122.0101731
    ): ReminderDataItem {
        return ReminderDataItem(
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
            val reminderNoTitle = getReminderDataItem(null)
            viewModel.validateAndSaveReminder(reminderNoTitle)
            assertThat(
                viewModel.showSnackBarInt.getOrWaitValue(),
                Matchers.`is`(Matchers.notNullValue())
            )
        }
    }

    @Test
    fun testLoading() {
        coroutineRule.runBlockingTest {
            val reminder = getReminderDataItem("SpaceX", "Company", "Elon", 23.2, 52.3)

            coroutineRule.pauseDispatcher()

            viewModel.validateAndSaveReminder(reminder)

            assertThat(viewModel.showLoading.getOrWaitValue(), Matchers.`is`(true))

            coroutineRule.resumeDispatcher()
            assertThat(viewModel.showLoading.getOrWaitValue(), Matchers.`is`(false))
        }
    }

}