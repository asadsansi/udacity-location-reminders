package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

// Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

    private var shouldReturnsError = false

    fun setReturnsError(value: Boolean) {
        shouldReturnsError = value
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }
    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return if (shouldReturnsError) Result.Error(Exception("ERROR").toString())
        else {
            reminders?.let {
                for (reminder in it) {
                    if (reminder.id == id) return Result.Success(reminder)
                }
            }
            Result.Error(Exception(
                "Reminder not found"
                ).toString()
            )
        }
    }
    override suspend fun getReminders(): Result<List<ReminderDTO>> {

        return if (shouldReturnsError) {
            Result.Error(Exception("ERROR").toString())
        } else {
            reminders?.let { return Result.Success(ArrayList(it)) }
            Result.Error(
                Exception(
                    "Reminders not found"
                ).toString()
            )
        }
    }

}
