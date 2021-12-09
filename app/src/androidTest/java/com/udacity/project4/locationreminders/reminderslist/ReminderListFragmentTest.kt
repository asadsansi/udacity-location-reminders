package com.udacity.project4.locationreminders.reminderslist


import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {

    private lateinit var repository: ReminderDataSource

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()


    @Before
    fun initRepository() {

     stopKoin()

    val mModule = module {

        viewModel {
            RemindersListViewModel(
                    getApplicationContext(),
                    get() as ReminderDataSource
            )
        }

        single { RemindersLocalRepository(get()) as ReminderDataSource }
        single { LocalDB.createRemindersDao(getApplicationContext()) }
    }

    startKoin {
        androidContext(getApplicationContext())
        modules(listOf(mModule))
    }

     repository = GlobalContext.get().koin.get()


     runBlocking {
         repository.deleteAllReminders()
     }
    }


    @After
    fun stopKoinAfterTesting() = stopKoin()




    @Test
    fun remindersSave_ShowsUI() {

        val reminder1 = ReminderDTO("Europe", "Beautiful", "Germany", 41.0, 98.0)
        val reminder2 = ReminderDTO("Asia", "Nice Place", "Turkey", 71.0, 38.0)

        runBlocking{
            repository.saveReminder(reminder1)
            repository.saveReminder(reminder2)
        }

        launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)

        onView(withId(R.id.noDataTextView)).check(matches(not(isDisplayed())))
        onView(withText(reminder1.title)).check(matches(isDisplayed()))
        onView(withText(reminder2.title)).check(matches(isDisplayed()))
    }

    @Test
    fun deleteAll_checkNoData() {

        runBlocking {
            repository.deleteAllReminders()
        }

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun clickFab_navigateTo_saveReminderFragment() {


        val navigationController = mock(NavController::class.java)

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme).onFragment {
            Navigation.setViewNavController(it.view!!, navigationController)
        }

        onView(withId(R.id.addReminderFAB)).perform(click())
        verify(navigationController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

}