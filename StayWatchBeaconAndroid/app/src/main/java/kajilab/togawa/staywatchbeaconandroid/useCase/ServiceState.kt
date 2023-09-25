package kajilab.togawa.staywatchbeaconandroid.useCase

import android.app.ActivityManager
import android.content.Context

class ServiceState {
    fun isServiceRunning(context: Context, serviceClass:Class<*>): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val services = activityManager.getRunningServices(Int.MAX_VALUE)
        for (service in services) {
            if(serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}