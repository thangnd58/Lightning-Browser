package acr.browser.lightning

import acr.browser.lightning.compose.StateProvider
import androidx.appcompat.app.AppCompatActivity
import javax.inject.Inject
import javax.inject.Named

abstract class ThemableActivity : AppCompatActivity() {
    @Named("theme")
    @Inject lateinit var appThemePreferenceStoreStateProvider: StateProvider<AppTheme>
}
