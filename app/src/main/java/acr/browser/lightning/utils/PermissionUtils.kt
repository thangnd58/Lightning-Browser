package acr.browser.lightning.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Minimal runtime-permission helper used in place of a third party permission library.
 *
 * Runtime permissions only exist on Android 6.0 (API 23) and above. On Android 4.4.2 (API 19),
 * which this build targets, the permissions declared in the manifest are granted at install time,
 * so [isGranted] returns true and [request] reports success immediately. On API 23+ the system
 * prompt is shown and the current grant status is reported back.
 */
object PermissionUtils {

    private const val REQUEST_CODE = 9001

    fun isGranted(context: Context, permission: String): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun request(activity: Activity, permissions: List<String>, onResult: (Boolean) -> Unit) {
        val missing = permissions.filter { !isGranted(activity, it) }
        if (missing.isEmpty()) {
            onResult(true)
            return
        }
        ActivityCompat.requestPermissions(activity, missing.toTypedArray(), REQUEST_CODE)
        onResult(missing.all { isGranted(activity, it) })
    }
}
