@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.groovernapp.mosqueathan.ui.settings.fridaysettings

import com.groovernapp.mosqueathan.ui.design.LocalWindowClass
import com.groovernapp.mosqueathan.ui.settings.displaymedia.TransitionIntervalRow
import com.groovernapp.mosqueathan.ui.settings.displaymedia.TransitionAnimationRow
import com.groovernapp.mosqueathan.ui.settings.StopPreviewButton
import com.groovernapp.mosqueathan.ui.design.isRunning
import com.groovernapp.mosqueathan.domain.PreviewKind
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.groovernapp.mosqueathan.ui.component.SlideshowComponent
import com.groovernapp.mosqueathan.ui.design.MosqueRadius
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.component.MosqueIconButton
import com.groovernapp.mosqueathan.ui.design.component.MosquePrimaryButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueSectionCard
import com.groovernapp.mosqueathan.ui.design.component.MosqueSecondaryButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueTertiaryButton
import com.groovernapp.mosqueathan.ui.design.component.mosqueTextFieldColors
import com.groovernapp.mosqueathan.ui.design.focusHighlight
import com.groovernapp.mosqueathan.ui.design.media.MediaKind
import com.groovernapp.mosqueathan.ui.design.media.rememberAudioFilePicker
import com.groovernapp.mosqueathan.ui.design.media.rememberMediaPicker
import com.groovernapp.mosqueathan.ui.design.media.rememberMultipleMediaPicker
import com.groovernapp.mosqueathan.ui.settings.AdjustmentRow
import com.groovernapp.mosqueathan.util.Lang
import kotlinx.coroutines.launch

@Composable
fun FridaySettingsPane(
    state: FridayUiState,
    onEvent: (FridayEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val slidePicker = rememberMultipleMediaPicker(MediaKind.ImageAndVideo)
    val slideReplacePicker = rememberMediaPicker(MediaKind.ImageAndVideo)

    /**
     * Opens a picker for the request events and passes everything else on.
     *
     * The pickers have to be remembered here — they are composables — so the pane is
     * the only place that can act on a "pick a file" request. Children stay on
     * `state` + `onEvent` and never see a picker.
     */
    val handleEvent: (FridayEvent) -> Unit = { event ->
        when (event) {
            FridayEvent.MediaPickRequested ->
                slidePicker.launch { files -> onEvent(FridayEvent.MediaAdded(files)) }

            is FridayEvent.MediaFilePickRequested ->
                slideReplacePicker.launch { file ->
                    onEvent(FridayEvent.MediaFileReplaced(event.id, file))
                }

            else -> onEvent(event)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xxxl)
    ) {
        // Card 1: Friday Core Settings — the enable switch needs no heading above it
        MosqueSectionCard(title = "", icon = null) {
            Column(verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)) {
                SettingSwitchRow(
                    title = Lang.get("Enable Friday Mode", "Aktifkan Mode Jum'at"),
                    description = Lang.get(
                        "Enable Friday features and layout adjustments",
                        "Aktifkan fitur khusus dan penyesuaian tata letak untuk hari Jum'at"
                    ),
                    checked = state.enabled,
                    onCheckedChange = { handleEvent(FridayEvent.EnabledChanged(it)) }
                )

                if (state.enabled) {
                    Spacer(modifier = Modifier.height(MosqueTheme.spacing.xl))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
                    ) {
                        OutlinedTextField(
                            value = state.startTime,
                            onValueChange = { handleEvent(FridayEvent.StartTimeChanged(it)) },
                            label = { Text(Lang.get("Start Time (HH:mm)", "Waktu Mulai (HH:mm)")) },
                            colors = mosqueTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.endTime,
                            onValueChange = { handleEvent(FridayEvent.EndTimeChanged(it)) },
                            label = { Text(Lang.get("End Time (HH:mm)", "Waktu Selesai (HH:mm)")) },
                            colors = mosqueTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    AdjustmentRow(
                        title = Lang.get("Friday Adzan Offset", "Koreksi Waktu Adzan Jum'at"),
                        value = state.dzuhurOffsetMinutes,
                        minValue = -60,
                        maxValue = 60,
                        showSign = true,
                        onValueChange = { handleEvent(FridayEvent.DzuhurOffsetChanged(it)) }
                    )
                }
            }
        }

        if (state.enabled) {
            // Card 2: Silence & Warning Text
            MosqueSectionCard(
                title = Lang.get("Friday Silence Mode", "Mode Ibadah & Khutbah"),
                icon = Icons.Default.Settings
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)) {
                    SettingSwitchRow(
                        title = Lang.get("Standby Mode Only", "Mode Standby Saja"),
                        description = Lang.get(
                            "Turn screen completely black after Friday Adzan until Friday service ends",
                            "Layar TV akan padam total setelah adzan Jum'at sampai ibadah Jum'at selesai"
                        ),
                        checked = state.standbyOnly,
                        onCheckedChange = { handleEvent(FridayEvent.StandbyOnlyChanged(it)) }
                    )

                    if (state.standbyOnly) {
                        Text(
                            text = Lang.get(
                                "Note: Other Friday visual features are disabled because Standby Mode is active.",
                                "Catatan: Menu visual Jum'at lainnya dinonaktifkan karena Mode Standby Saja aktif."
                            ),
                            color = MosqueTheme.colors.accent,
                            fontSize = MosqueTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = MosqueTheme.spacing.xs)
                        )
                    }
                }
            }

            // Card 3: Murottal Pre-Adzan Settings
            MosqueSectionCard(
                title = Lang.get("Pre-Adzan Murottal Quran", "Murottal Sebelum Adzan"),
                icon = Icons.Default.Settings,
                dimmed = !state.visualsEnabled
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)) {
                    SettingSwitchRow(
                        title = Lang.get("Auto Play Murottal", "Putar Murottal Otomatis"),
                        description = Lang.get(
                            "Automatically play Quran audio files before adzan",
                            "Putar berkas audio Al-Qur'an secara otomatis menjelang adzan"
                        ),
                        checked = state.murottal.enabled,
                        onCheckedChange = { handleEvent(FridayEvent.MurottalEnabledChanged(it)) },
                        enabled = state.visualsEnabled
                    )

                    if (state.murottal.enabled) {
                        AdjustmentRow(
                            title = Lang.get(
                                "Start Delay (Minutes)",
                                "Jeda Mulai (Menit setelah Waktu Mulai)"
                            ),
                            value = state.murottal.startDelayMinutes,
                            minValue = 0,
                            maxValue = 120,
                            showSign = false,
                            onValueChange = {
                                if (state.visualsEnabled) {
                                    handleEvent(FridayEvent.MurottalStartDelayChanged(it))
                                }
                            }
                        )

                        MurottalAudioPathsManager(
                            state = state.murottal,
                            onEvent = handleEvent,
                            enabled = state.visualsEnabled
                        )
                    }
                }
            }

            // Card 4: Friday Slideshow
            MosqueSectionCard(
                title = Lang.get("Friday Specific Slideshow", "Slideshow Khusus Jum'at"),
                icon = Icons.Default.Settings,
                dimmed = !state.visualsEnabled
            ) {
                FridaySlideshowSection(
                    state = state.slideshow,
                    visualsEnabled = state.visualsEnabled,
                    onEvent = handleEvent
                )
            }
        }
    }
}

/**
 * The Friday slideshow card's contents: what to show, in what order, and for how
 * long.
 */
@Composable
private fun FridaySlideshowSection(
    state: FridaySlideshowSettings,
    visualsEnabled: Boolean,
    onEvent: (FridayEvent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)) {
        SettingSwitchRow(
            title = Lang.get("Enable Friday Slideshow", "Aktifkan Slideshow Jum'at"),
            description = Lang.get(
                "Display different backgrounds/slides on Friday",
                "Tampilkan gambar/slideshow khusus di hari Jum'at"
            ),
            checked = state.enabled,
            onCheckedChange = { onEvent(FridayEvent.SlideshowEnabledChanged(it)) },
            enabled = visualsEnabled
        )

        SettingSwitchRow(
            title = Lang.get("Include Global Slideshow", "Sertakan Slideshow Global"),
            description = Lang.get(
                "Mix global slideshow images in rotation",
                "Sertakan gambar dari slideshow reguler dalam putaran"
            ),
            checked = state.includeGlobalMedia,
            onCheckedChange = { onEvent(FridayEvent.IncludeGlobalMediaChanged(it)) },
            enabled = visualsEnabled && state.enabled
        )

        if (state.enabled) {
            if (state.includeGlobalMedia) {
                val pagerState = rememberPagerState(initialPage = 0) { 2 }
                val coroutineScope = rememberCoroutineScope()

                SecondaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MosqueTheme.colors.transparent,
                    contentColor = MosqueTheme.colors.accent,
                    divider = {}
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                        text = {
                            Text(
                                text = Lang.get("Slideshow", "Slideshow"),
                                fontSize = MosqueTheme.typography.bodyCompact,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        selectedContentColor = MosqueTheme.colors.accent,
                        unselectedContentColor = MosqueTheme.colors.textSecondary
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        text = {
                            Text(
                                text = Lang.get("Global Slideshow", "Slideshow Global"),
                                fontSize = MosqueTheme.typography.bodyCompact,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        selectedContentColor = MosqueTheme.colors.accent,
                        unselectedContentColor = MosqueTheme.colors.textSecondary
                    )
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    if (page == 0) {
                        FridayMediaListSection(state = state, onEvent = onEvent)
                    } else {
                        GlobalSlideshowSelectionSection(state = state, onEvent = onEvent)
                    }
                }
            } else {
                FridayMediaListSection(state = state, onEvent = onEvent)
            }

            Spacer(modifier = Modifier.height(MosqueTheme.spacing.xl))

            // The same two controls the Dashboard and Slideshow tabs use, so the wording and the
            // layout cannot drift between them.
            TransitionAnimationRow(
                title = Lang.get("Transition Animation", "Animasi Transisi"),
                selectedType = state.transitionType,
                isCompact = LocalWindowClass.current.isCompact,
                onTypeSelected = { onEvent(FridayEvent.TransitionTypeChanged(it)) }
            )

            TransitionIntervalRow(
                title = Lang.get("Transition interval", "Interval transisi"),
                seconds = state.intervalSeconds,
                onSecondsChange = { onEvent(FridayEvent.IntervalSecondsChanged(it)) }
            )

            SettingSwitchRow(
                title = Lang.get("Wait Video End", "Tunggu Video Selesai"),
                description = Lang.get(
                    "Wait for video to finish before next slide",
                    "Tunggu video selesai sebelum lanjut slide berikutnya"
                ),
                checked = state.videoWaitUntilEnd,
                onCheckedChange = { onEvent(FridayEvent.VideoWaitUntilEndChanged(it)) }
            )

            Spacer(modifier = Modifier.height(MosqueTheme.spacing.xl))
            if (PreviewKind.FridaySlideshow.isRunning) {
                StopPreviewButton(
                    onClick = { onEvent(FridayEvent.SlideshowPreviewRequested) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else MosquePrimaryButton(
                onClick = { onEvent(FridayEvent.SlideshowPreviewRequested) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = Lang.get("Preview Slideshow", "Pratinjau Slideshow"),
                    fontWeight = FontWeight.Bold,
                    color = MosqueTheme.colors.onAccent
                )
            }
        }
    }
}

/**
 * A titled switch with a line of explanation — the shape this pane repeats seven
 * times, previously written out in full each time.
 */
@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) MosqueTheme.colors.textPrimary else MosqueTheme.colors.textDisabled,
                fontWeight = FontWeight.Bold,
                fontSize = MosqueTheme.typography.body
            )
            Text(
                text = description,
                color = if (enabled) MosqueTheme.colors.textSecondary else MosqueTheme.colors.border,
                fontSize = MosqueTheme.typography.labelSmall
            )
        }
        Switch(
            modifier = Modifier.focusHighlight(
                shape = RoundedCornerShape(MosqueRadius.pill),
                scaleUp = false
            ),
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MosqueTheme.colors.accent,
                checkedTrackColor = MosqueTheme.colors.accent.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun FridayMediaListSection(
    state: FridaySlideshowSettings,
    onEvent: (FridayEvent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
    ) {
        SlideshowComponent(
            items = state.media,
            keySelector = { it.id },
            filePathSelector = { it.filePath },
            isVideoSelector = { it.isVideo },
            isUsedSelector = null, // No checkbox for Friday specific media
            showAddButton = true,
            addText = Lang.get("Add Image / Video", "Tambah Gambar / Video"),
            onAddClicked = { onEvent(FridayEvent.MediaPickRequested) },
            onReorderItems = { ids -> onEvent(FridayEvent.MediaReordered(ids)) },
            onEditItem = { onEvent(FridayEvent.MediaFilePickRequested(it.id)) },
            onDeleteItem = { onEvent(FridayEvent.MediaDeleted(it)) },
            emptyText = Lang.get(
                "No media added. Slideshow is empty.",
                "Belum ada media. Slideshow kosong."
            ),
            showTransitionAnimation = false,
            showTransitionInterval = false
        )
    }
}

@Composable
private fun GlobalSlideshowSelectionSection(
    state: FridaySlideshowSettings,
    onEvent: (FridayEvent) -> Unit
) {
    val selectedIdsSet = remember(state.selectedGlobalMediaIds) {
        state.selectedGlobalMediaIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
    ) {
        SlideshowComponent(
            items = state.globalMedia,
            keySelector = { it.id },
            filePathSelector = { it.filePath },
            isVideoSelector = { it.isVideo },
            isUsedSelector = { selectedIdsSet.contains(it.id) },
            onUpdateUseStatus = { item, isUsed ->
                val newSet = if (isUsed) selectedIdsSet + item.id else selectedIdsSet - item.id
                onEvent(FridayEvent.SelectedGlobalMediaChanged(newSet.joinToString(",")))
            },
            showAddButton = false, // Not adding/editing from this view, just selecting
            onEditItem = { /* No edit from here */ },
            onDeleteItem = { /* No delete from here */ },
            emptyText = Lang.get("No global media found.", "Slideshow global kosong."),
            showTransitionAnimation = false,
            showTransitionInterval = false
        )
    }
}

@Composable
private fun MurottalAudioPathsManager(
    state: MurottalSettings,
    onEvent: (FridayEvent) -> Unit,
    enabled: Boolean
) {
    val paths = remember(state.audioPaths) {
        state.audioPaths.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    // The chosen references are the setting itself — nothing is copied and nothing is
    // resolved by a ViewModel — so this picker stays local to the manager that owns
    // the list.
    val audioPicker = rememberAudioFilePicker()


    Column(verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)) {
        Text(
            text = Lang.get("Murottal Audio Files", "File Audio Murottal"),
            color = if (enabled) MosqueTheme.colors.textPrimary else MosqueTheme.colors.textMuted,
            fontSize = MosqueTheme.typography.label
        )

        if (paths.isNotEmpty()) {
            paths.take(MAX_LISTED_AUDIO_PATHS).forEach { path ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MosqueTheme.colors.fillSubtle,
                            RoundedCornerShape(MosqueTheme.radius.md)
                        )
                        .padding(MosqueTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = path.substringAfterLast("/").takeIf { it.isNotEmpty() } ?: path,
                        color = MosqueTheme.colors.textPrimary,
                        fontSize = MosqueTheme.typography.caption,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    MosqueIconButton(
                        onClick = {
                            val newPaths = paths.filter { it != path }
                            onEvent(FridayEvent.MurottalAudioPathsChanged(newPaths.joinToString(",")))
                        },
                        modifier = Modifier.size(MosqueTheme.size.iconLg)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = MosqueTheme.colors.danger.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        } else {
            Text(
                text = Lang.get("No audio files selected", "Belum ada file audio"),
                color = MosqueTheme.colors.textDisabled,
                fontSize = MosqueTheme.typography.caption
            )
        }

        // A field you press, not one you type into.
        //
        // The value is never typed: a path has to name a file the *display* can open, and no one
        // can be expected to know that path. So the field is a target for the platform's own audio
        // chooser — the file is copied on the display and uploaded from a controller, and the path
        // comes back from whichever of those happened.
        //
        // The inner field is disabled rather than read-only: a read-only field still takes focus and
        // shows a caret, which invites typing that will never be accepted. Disabled ignores pointer
        // input entirely, so the press lands on the Box.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MosqueTheme.radius.md))
                .focusHighlight(RoundedCornerShape(MosqueTheme.radius.md), scaleUp = false)
                .clickable(enabled = enabled) {
                    audioPicker.launch { picked ->
                        onEvent(FridayEvent.MurottalAudioPicked(picked))
                    }
                }
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                enabled = false,
                singleLine = true,
                placeholder = {
                    Text(
                        Lang.get("Choose audio files…", "Pilih file audio…"),
                        fontSize = MosqueTheme.typography.caption
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = if (enabled) {
                            MosqueTheme.colors.accent
                        } else {
                            MosqueTheme.colors.textDisabled
                        }
                    )
                },
                colors = mosqueTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)) {
            if (paths.size > MAX_LISTED_AUDIO_PATHS) {
                MosqueSecondaryButton(
                    onClick = { /* Open full list dialog or expand */ },
                    enabled = enabled
                ) {
                    Text(
                        Lang.get(
                            "List Audio (${paths.size})",
                            "Daftar Audio (${paths.size})"
                        )
                    )
                }
            }
        }
    }
}

/** Beyond this the list is summarised behind a button rather than scrolled. */
private const val MAX_LISTED_AUDIO_PATHS = 5

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 1280, heightDp = 720)
@Composable
fun FridaySettingsPanePreview() {
    // Forty-one arguments became two, so the preview is now readable: it says what
    // it is showing rather than restating the whole signature.
    MosqueTheme {
        FridaySettingsPane(
            state = FridayUiState(
                enabled = true,
                startTime = "11:00",
                endTime = "13:00",
                murottal = MurottalSettings(enabled = true, startDelayMinutes = 15),
                slideshow = FridaySlideshowSettings(enabled = true, transitionType = "FADE", intervalSeconds = 10)
            ),
            onEvent = {}
        )
    }
}
