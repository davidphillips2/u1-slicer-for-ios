package com.u1.slicer

import android.content.Context
import com.u1.slicer.data.AppDatabase
import com.u1.slicer.data.SettingsRepository
import com.u1.slicer.network.MoonrakerClient
import com.u1.slicer.printer.PrinterRepository

class AppContainer(context: Context) {
    val settingsRepository = SettingsRepository(context)
    val moonrakerClient = MoonrakerClient()
    val printerRepository = PrinterRepository(moonrakerClient, settingsRepository)

    val database = AppDatabase.getInstance(context)
    val filamentDao = database.filamentDao()
    val sliceJobDao = database.sliceJobDao()
}
