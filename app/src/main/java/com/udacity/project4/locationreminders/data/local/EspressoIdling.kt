package com.udacity.project4.locationreminders.data.local
import androidx.test.espresso.idling.CountingIdlingResource


// EspressoIdlingResource
object EspressoIdling {

    private const val RESOURCE_NAME = "EspressoRes"

    // count Idling Resource
    @JvmField
    val countIdling = CountingIdlingResource(RESOURCE_NAME)

    fun increment() {
        countIdling.increment()
    }

    fun decrement() {
        if (!countIdling.isIdleNow) {
            countIdling.decrement()
        }
    }
}

inline fun <T> wrapEspressoIdling(function: () -> T): T {

    EspressoIdling.increment()
    return try {
        function()
    } finally {
        EspressoIdling.decrement() // Set app as idle.
    }
}


