package com.smartledger.app

import android.app.Application
import com.smartledger.app.data.database.AppDatabase

class SmartLedgerApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: SmartLedgerApp
            private set
    }
}
