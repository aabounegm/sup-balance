package com.abounegm.sup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

fun ensureBackgroundUpdatesRegistered(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    val updateRequest =
        PeriodicWorkRequestBuilder<UpdateBalanceWorker>(5, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
    WorkManager
        .getInstance(context)
        .enqueueUniquePeriodicWork(
            "balanceBackgroundUpdate",
            ExistingPeriodicWorkPolicy.UPDATE,
            updateRequest
        )
}

class UpdateBalanceWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    // this method will run in background thread and WorkManger will take care of it
    override suspend fun doWork(): Result {
        val store = BalanceStore(this.applicationContext)
        try {
            store.updateValues()
        } catch (e: Exception) {
            return Result.failure()
        }

        return Result.success()
    }
}