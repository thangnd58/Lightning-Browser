/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.settings.activity

import acr.browser.lightning.R
import acr.browser.lightning.ThemableActivity
import acr.browser.lightning.browser.di.injector
import acr.browser.lightning.device.BuildInfo
import acr.browser.lightning.device.BuildType
import acr.browser.lightning.settings.SettingsBottomSheetChooserState
import acr.browser.lightning.settings.SettingsBottomSheetInputState
import acr.browser.lightning.settings.SettingsClickableState
import acr.browser.lightning.settings.SettingsDialogConfirmationState
import acr.browser.lightning.settings.SettingsOption
import acr.browser.lightning.settings.SettingsToggleState
import acr.browser.lightning.settings.SettingsUiState
import acr.browser.lightning.settings.framework.SettingsFrameworkPresenter
import acr.browser.lightning.settings.framework.SettingsFrameworkState
import acr.browser.lightning.settings.framework.SettingsFrameworkUiEvent
import acr.browser.lightning.settings.screens.AboutSettingsScreen
import acr.browser.lightning.settings.screens.AdBlockSettingsScreen
import acr.browser.lightning.settings.screens.AdvancedSettingsScreen
import acr.browser.lightning.settings.screens.BookmarkSettingsScreen
import acr.browser.lightning.settings.screens.DebugSettingsScreen
import acr.browser.lightning.settings.screens.DisplaySettingsScreen
import acr.browser.lightning.settings.screens.GeneralSettingsScreen
import acr.browser.lightning.settings.screens.PrivacySettingsScreen
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A classic View based settings screen. It renders the declarative [SettingsFrameworkState]
 * produced by the per-screen classes, replacing the previous Jetpack Compose implementation
 * (which required minSdk 21 and could not run on Android 4.4.2 / API 19).
 */
class SettingsActivity : ThemableActivity() {

    @Inject internal lateinit var buildInfo: BuildInfo

    @Inject internal lateinit var aboutSettingsScreen: AboutSettingsScreen
    @Inject internal lateinit var adBlockSettingsScreen: AdBlockSettingsScreen
    @Inject internal lateinit var advancedSettingsScreen: AdvancedSettingsScreen
    @Inject internal lateinit var bookmarkSettingsScreen: BookmarkSettingsScreen
    @Inject internal lateinit var debugSettingsScreen: DebugSettingsScreen
    @Inject internal lateinit var displaySettingsScreen: DisplaySettingsScreen
    @Inject internal lateinit var generalSettingsScreen: GeneralSettingsScreen
    @Inject internal lateinit var privacySettingsScreen: PrivacySettingsScreen

    private lateinit var contentContainer: LinearLayout
    private lateinit var progressBar: ProgressBar

    private var currentScreenKey: String? = null
    private var activePresenter: SettingsFrameworkPresenter? = null
    private var stateJob: Job? = null
    private var previousState: SettingsUiState? = null
    private var currentDialog: AlertDialog? = null
    private var handledWebLink: String? = null

    private lateinit var getContentLauncher: ActivityResultLauncher<String>
    private lateinit var createDocumentLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(this)
        super.onCreate(savedInstanceState)

        getContentLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                activePresenter?.onEvent(SettingsFrameworkUiEvent.FileChosen(uri))
            }
        createDocumentLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
                activePresenter?.onEvent(SettingsFrameworkUiEvent.FileChosen(uri))
            }

        contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val scrollView = ScrollView(this).apply {
            addView(
                contentContainer,
                ViewGroup.LayoutParams(MATCH, WRAP)
            )
        }
        progressBar = ProgressBar(this).apply { visibility = View.GONE }

        val root = FrameLayout(this).apply {
            setBackgroundColor(resolveColor(android.R.attr.colorBackground))
            addView(scrollView, FrameLayout.LayoutParams(MATCH, MATCH))
            addView(
                progressBar,
                FrameLayout.LayoutParams(WRAP, WRAP).apply { gravity = Gravity.CENTER }
            )
        }
        setContentView(root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        showRoot()
    }

    override fun onSupportNavigateUp(): Boolean {
        handleBack()
        return true
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (currentScreenKey != null) {
            showRoot()
        } else {
            super.onBackPressed()
        }
    }

    private fun handleBack() {
        if (currentScreenKey != null) {
            showRoot()
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        stateJob?.cancel()
        dismissDialog()
        super.onDestroy()
    }

    private fun showRoot() {
        currentScreenKey = null
        activePresenter = null
        stateJob?.cancel()
        stateJob = null
        previousState = null
        handledWebLink = null
        dismissDialog()
        progressBar.visibility = View.GONE
        supportActionBar?.title = getString(R.string.settings)
        contentContainer.removeAllViews()

        addClickableRow(getString(R.string.settings_adblock), null) {
            openScreen(adBlockSettingsScreen.key) { adBlockSettingsScreen.createSettingsFrameworkState() }
        }
        addClickableRow(getString(R.string.settings_general), null) {
            openScreen(generalSettingsScreen.key) { generalSettingsScreen.createSettingsFrameworkState() }
        }
        addClickableRow(getString(R.string.bookmark_settings), null) {
            openScreen(bookmarkSettingsScreen.key) { bookmarkSettingsScreen.createSettingsFrameworkState() }
        }
        addClickableRow(getString(R.string.settings_display), null) {
            openScreen(displaySettingsScreen.key) { displaySettingsScreen.createSettingsFrameworkState() }
        }
        addClickableRow(getString(R.string.settings_privacy), null) {
            openScreen(privacySettingsScreen.key) { privacySettingsScreen.createSettingsFrameworkState() }
        }
        addClickableRow(getString(R.string.settings_advanced), null) {
            openScreen(advancedSettingsScreen.key) { advancedSettingsScreen.createSettingsFrameworkState() }
        }
        addClickableRow(
            getString(R.string.settings_about),
            getString(R.string.settings_about_explain)
        ) {
            openScreen(aboutSettingsScreen.key) { aboutSettingsScreen.createSettingsFrameworkState() }
        }
        addClickableRow(getString(R.string.faq), getString(R.string.faq_description)) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "http://acrdevelopment.org/lightning/faq".toUri()
                )
            )
        }
        if (buildInfo.buildType == BuildType.DEBUG) {
            addClickableRow(getString(R.string.debug_title), null) {
                openScreen(debugSettingsScreen.key) { debugSettingsScreen.createSettingsFrameworkState() }
            }
        }
    }

    private fun openScreen(key: String, producer: () -> SettingsFrameworkState) {
        currentScreenKey = key
        previousState = null
        handledWebLink = null
        dismissDialog()
        val presenter = ViewModelProvider(
            this,
            SettingsFrameworkPresenter.Factory(settingsFrameworkState = { producer() })
        ).get(key, SettingsFrameworkPresenter::class.java)
        activePresenter = presenter
        stateJob?.cancel()
        stateJob = lifecycleScope.launch {
            presenter.state.collect { render(it) }
        }
    }

    private fun render(state: SettingsUiState) {
        val prev = previousState
        supportActionBar?.title = state.title

        when (val content = state.content) {
            SettingsUiState.Content.Loading -> {
                progressBar.visibility = View.VISIBLE
                contentContainer.removeAllViews()
            }

            is SettingsUiState.Content.Actual -> {
                progressBar.visibility = View.GONE
                renderEntries(content.entries)
                handleDialogs(prev, content)
            }
        }

        handleOneShots(prev, state)

        previousState = state
    }

    private fun handleOneShots(prev: SettingsUiState?, state: SettingsUiState) {
        state.ephemeral?.let {
            if (prev?.ephemeral == null) {
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                activePresenter?.onEvent(SettingsFrameworkUiEvent.SnackbarDismissed)
            }
        }
        state.chooseFile?.let {
            if (prev?.chooseFile == null) {
                getContentLauncher.launch(it)
            }
        }
        state.createFile?.let {
            if (prev?.createFile == null) {
                createDocumentLauncher.launch(it)
            }
        }
        state.webLink?.let { link ->
            if (link != handledWebLink) {
                handledWebLink = link
                startActivity(Intent(Intent.ACTION_VIEW, link.toUri()))
                // Clear the consumed link so re-entering the screen does not reopen it.
                activePresenter?.let { it.state.value = it.state.value.copy(webLink = null) }
            }
        }
    }

    private fun handleDialogs(prev: SettingsUiState?, content: SettingsUiState.Content.Actual) {
        val prevContent = prev?.content as? SettingsUiState.Content.Actual
        val chooser = content.bottomSheetChooser
        val input = content.bottomSheetInput
        val confirmation = content.dialogConfirmation

        if (chooser == null && input == null && confirmation == null) {
            dismissDialog()
            return
        }

        when {
            chooser != null && chooser !== prevContent?.bottomSheetChooser -> showChooser(chooser)
            input != null && input !== prevContent?.bottomSheetInput -> showInput(input)
            confirmation != null && confirmation !== prevContent?.dialogConfirmation ->
                showConfirmation(confirmation)
        }
    }

    private fun showChooser(chooser: SettingsBottomSheetChooserState) {
        val presenter = activePresenter ?: return
        dismissDialog()
        val items = Array<CharSequence>(chooser.values.size) { chooser.values[it] }
        currentDialog = AlertDialog.Builder(this)
            .setTitle(chooser.title)
            .setSingleChoiceItems(
                items,
                chooser.selected
            ) { dialog, which ->
                dialog.dismiss()
                presenter.onEvent(SettingsFrameworkUiEvent.BottomSheetChoiceResult(which))
            }
            .setOnCancelListener {
                presenter.onEvent(SettingsFrameworkUiEvent.BottomSheetChoiceResult(null))
            }
            .create()
        currentDialog?.show()
    }

    private fun showInput(input: SettingsBottomSheetInputState) {
        val presenter = activePresenter ?: return
        dismissDialog()
        val editText = EditText(this).apply {
            setText(input.currentValue)
            hint = input.hint
        }
        val pad = dp(16)
        val container = FrameLayout(this).apply {
            setPadding(pad, dp(8), pad, 0)
            addView(editText)
        }
        currentDialog = AlertDialog.Builder(this)
            .setTitle(input.title)
            .setView(container)
            .setPositiveButton(R.string.action_ok) { _, _ ->
                presenter.onEvent(
                    SettingsFrameworkUiEvent.BottomSheetInputResult(editText.text.toString())
                )
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                presenter.onEvent(SettingsFrameworkUiEvent.BottomSheetInputResult(null))
            }
            .setOnCancelListener {
                presenter.onEvent(SettingsFrameworkUiEvent.BottomSheetInputResult(null))
            }
            .create()
        currentDialog?.show()
    }

    private fun showConfirmation(confirmation: SettingsDialogConfirmationState) {
        val presenter = activePresenter ?: return
        dismissDialog()
        currentDialog = AlertDialog.Builder(this)
            .setTitle(confirmation.title)
            .setMessage(confirmation.message)
            .setPositiveButton(confirmation.positiveAction) { _, _ ->
                presenter.onEvent(SettingsFrameworkUiEvent.DialogConfirmation(true))
            }
            .setNegativeButton(confirmation.negativeAction) { _, _ ->
                presenter.onEvent(SettingsFrameworkUiEvent.DialogConfirmation(false))
            }
            .setOnCancelListener {
                presenter.onEvent(SettingsFrameworkUiEvent.DialogConfirmation(false))
            }
            .create()
        currentDialog?.show()
    }

    private fun dismissDialog() {
        currentDialog?.let { dialog ->
            // Detach the cancel listener so a programmatic dismiss does not emit a cancel event.
            dialog.setOnCancelListener(null)
            dialog.dismiss()
        }
        currentDialog = null
    }

    private fun renderEntries(entries: List<SettingsOption>) {
        contentContainer.removeAllViews()
        entries.forEachIndexed { index, option ->
            when (option) {
                is SettingsClickableState -> addClickableRow(
                    title = option.title,
                    summary = option.summary,
                    enabled = option.enabled
                ) {
                    activePresenter?.onEvent(SettingsFrameworkUiEvent.Click(index))
                }

                is SettingsToggleState -> addToggleRow(option, index)
            }
        }
    }

    private fun addClickableRow(
        title: String,
        summary: String?,
        enabled: Boolean = true,
        onClick: () -> Unit
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(16)
            setPadding(pad, pad, pad, pad)
            isEnabled = enabled
            if (enabled) {
                setBackgroundResource(selectableItemBackground())
                setOnClickListener { onClick() }
            }
        }
        row.addView(makeTitle(title, enabled))
        if (summary != null) {
            row.addView(makeSummary(summary, enabled))
        }
        contentContainer.addView(row, LinearLayout.LayoutParams(MATCH, WRAP))
    }

    private fun addToggleRow(option: SettingsToggleState, index: Int) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val pad = dp(16)
            setPadding(pad, pad, pad, pad)
            if (option.enabled) {
                setBackgroundResource(selectableItemBackground())
            }
        }
        val textColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textColumn.addView(makeTitle(option.title, option.enabled))
        option.summary?.let { textColumn.addView(makeSummary(it, option.enabled)) }
        row.addView(textColumn, LinearLayout.LayoutParams(0, WRAP, 1f))

        val switch = Switch(this).apply {
            isChecked = option.isChecked
            isEnabled = option.enabled
        }
        row.addView(switch, LinearLayout.LayoutParams(WRAP, WRAP))

        switch.setOnClickListener {
            activePresenter?.onEvent(SettingsFrameworkUiEvent.Toggle(switch.isChecked, index))
        }
        if (option.enabled) {
            row.setOnClickListener {
                switch.isChecked = !switch.isChecked
                activePresenter?.onEvent(SettingsFrameworkUiEvent.Toggle(switch.isChecked, index))
            }
        }
        contentContainer.addView(row, LinearLayout.LayoutParams(MATCH, WRAP))
    }

    private fun makeTitle(text: String, enabled: Boolean): TextView = TextView(this).apply {
        this.text = text
        textSize = 16f
        setTextColor(resolveColor(android.R.attr.textColorPrimary))
        if (!enabled) alpha = 0.5f
    }

    private fun makeSummary(text: String, enabled: Boolean): TextView = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(resolveColor(android.R.attr.textColorSecondary))
        if (!enabled) alpha = 0.5f
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun selectableItemBackground(): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        return typedValue.resourceId
    }

    private fun resolveColor(attr: Int): Int {
        val typedValue = TypedValue()
        return if (theme.resolveAttribute(attr, typedValue, true)) {
            if (typedValue.resourceId != 0) {
                ContextCompat.getColor(this, typedValue.resourceId)
            } else {
                typedValue.data
            }
        } else {
            0xFF000000.toInt()
        }
    }

    private companion object {
        private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
