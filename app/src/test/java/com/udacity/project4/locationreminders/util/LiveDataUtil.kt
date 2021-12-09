package com.udacity.project4.locationreminders.util

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@VisibleForTesting(otherwise = VisibleForTesting.NONE)
fun <T> LiveData<T>.getOrWaitValue(time: Long = 2,
                                   unit: TimeUnit = TimeUnit.SECONDS, afterObserve: () -> Unit = {}
): T {
    var data: T? = null
    val countDownLatch = CountDownLatch(1)
    val iObserver = object : Observer<T> {
        override fun onChanged(o: T?) {
            data = o
            countDownLatch.countDown()
            this@getOrWaitValue.removeObserver(this)
        }
    }
    this.observeForever(iObserver)

    try {
        afterObserve.invoke()
        if (!countDownLatch.await(time, unit)) {
            throw TimeoutException("Value never set.")
        }
    } finally {
        this.removeObserver(iObserver)
    }

    @Suppress("UNCHECKED_CAST")
    return data as T
}
