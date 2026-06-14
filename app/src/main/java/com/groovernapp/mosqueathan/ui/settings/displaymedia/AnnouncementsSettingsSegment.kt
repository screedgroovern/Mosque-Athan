package com.groovernapp.mosqueathan.ui.settings.displaymedia

import com.groovernapp.mosqueathan.ui.settings.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.data.AnnouncementEntity
import com.groovernapp.mosqueathan.data.MosqueConfigEntity

@Composable
fun AnnouncementsSettingsSegment(
    config: MosqueConfigEntity,
    announcements: List<AnnouncementEntity>,
    onAddAnnouncement: (String) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onEditAnnouncement: (String, String) -> Unit,
    onUpdateFooters: (String, String) -> Unit,
    onPreviewAdzan: () -> Unit,
    onPreviewIqomah: () -> Unit
) {
    // Parse adzan & iqomah footer texts
    val adzanFooters = remember(config.adzanFooterText) {
        parseFooterTexts(config.adzanFooterText)
    }
    val iqomahFooters = remember(config.iqomahFooterText) {
        parseFooterTexts(config.iqomahFooterText)
    }

    var newAnnouncementText by remember { mutableStateOf("") }
    var newAdzanText by remember { mutableStateOf("") }
    var newIqomahText by remember { mutableStateOf("") }

    // Dialog state
    var editingAnnouncement by remember { mutableStateOf<AnnouncementEntity?>(null) }
    var editingAdzanIndex by remember { mutableStateOf<Int?>(null) }
    var editingIqomahIndex by remember { mutableStateOf<Int?>(null) }
    var editTextValue by remember { mutableStateOf("") }

    // Dialog rendering
    if (editingAnnouncement != null) {
        AlertDialog(
            onDismissRequest = { editingAnnouncement = null },
            title = { Text(com.groovernapp.mosqueathan.util.Lang.get("Edit Announcement", "Edit Pengumuman"), color = Color.White) },
            text = {
                OutlinedTextField(
                    value = editTextValue,
                    onValueChange = { editTextValue = it },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TvFocusablePrimaryButton(
                    onClick = {
                        editingAnnouncement?.let {
                            if (editTextValue.isNotBlank()) {
                                onEditAnnouncement(it.id, editTextValue.trim())
                            }
                        }
                        editingAnnouncement = null
                    }
                ) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Save", "Simpan"), color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAnnouncement = null }) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Cancel", "Batal"), color = Color.White)
                }
            }
        )
    }

    if (editingAdzanIndex != null) {
        AlertDialog(
            onDismissRequest = { editingAdzanIndex = null },
            title = { Text(com.groovernapp.mosqueathan.util.Lang.get("Edit Adzan Footer", "Edit Footer Adzan"), color = Color.White) },
            text = {
                OutlinedTextField(
                    value = editTextValue,
                    onValueChange = { editTextValue = it },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TvFocusablePrimaryButton(
                    onClick = {
                        editingAdzanIndex?.let { index ->
                            if (editTextValue.isNotBlank()) {
                                val updated = adzanFooters.toMutableList().apply { set(index, editTextValue.trim()) }
                                onUpdateFooters(formatFooterTexts(updated), config.iqomahFooterText)
                            }
                        }
                        editingAdzanIndex = null
                    }
                ) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Save", "Simpan"), color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAdzanIndex = null }) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Cancel", "Batal"), color = Color.White)
                }
            }
        )
    }

    if (editingIqomahIndex != null) {
        AlertDialog(
            onDismissRequest = { editingIqomahIndex = null },
            title = { Text(com.groovernapp.mosqueathan.util.Lang.get("Edit Iqomah Footer", "Edit Footer Iqomah"), color = Color.White) },
            text = {
                OutlinedTextField(
                    value = editTextValue,
                    onValueChange = { editTextValue = it },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TvFocusablePrimaryButton(
                    onClick = {
                        editingIqomahIndex?.let { index ->
                            if (editTextValue.isNotBlank()) {
                                val updated = iqomahFooters.toMutableList().apply { set(index, editTextValue.trim()) }
                                onUpdateFooters(config.adzanFooterText, formatFooterTexts(updated))
                            }
                        }
                        editingIqomahIndex = null
                    }
                ) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Save", "Simpan"), color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingIqomahIndex = null }) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Cancel", "Batal"), color = Color.White)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Section: Adzan & Iqomah Footer Texts
        item {
            Text(
                text = com.groovernapp.mosqueathan.util.Lang.get("Adzan & Iqomah Footer Texts (Running Text)", "Teks Footer Adzan & Iqomah (Teks Berjalan)"),
                color = Color(0xFFFFD54F),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Sub-section: Adzan Footer Input & List
        item {
            Column {
                Text(
                    text = com.groovernapp.mosqueathan.util.Lang.get("Adzan Screen Footer", "Footer Layar Adzan"),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newAdzanText,
                        onValueChange = { newAdzanText = it },
                        placeholder = { Text(com.groovernapp.mosqueathan.util.Lang.get("Type adzan footer...", "Ketik footer adzan...")) },
                        colors = textFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    TvFocusablePrimaryButton(
                        onClick = {
                            if (newAdzanText.isNotBlank()) {
                                val updated = adzanFooters + newAdzanText.trim()
                                onUpdateFooters(formatFooterTexts(updated), config.iqomahFooterText)
                                newAdzanText = ""
                            }
                        },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(com.groovernapp.mosqueathan.util.Lang.get("Add", "Tambah"), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        itemsIndexed(adzanFooters) { index, footer ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = footer,
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = {
                        editTextValue = footer
                        editingAdzanIndex = index
                    }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color.Blue)
                    }
                    IconButton(onClick = {
                        val updated = adzanFooters.toMutableList().apply { removeAt(index) }
                        onUpdateFooters(formatFooterTexts(updated), config.iqomahFooterText)
                    }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                    }
                }
            }
        }

        // Sub-section: Iqomah Footer Input & List
        item {
            Column {
                Text(
                    text = com.groovernapp.mosqueathan.util.Lang.get("Iqomah Screen Footer", "Footer Layar Iqomah"),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newIqomahText,
                        onValueChange = { newIqomahText = it },
                        placeholder = { Text(com.groovernapp.mosqueathan.util.Lang.get("Type iqomah footer...", "Ketik footer iqomah...")) },
                        colors = textFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    TvFocusablePrimaryButton(
                        onClick = {
                            if (newIqomahText.isNotBlank()) {
                                val updated = iqomahFooters + newIqomahText.trim()
                                onUpdateFooters(config.adzanFooterText, formatFooterTexts(updated))
                                newIqomahText = ""
                            }
                        },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(com.groovernapp.mosqueathan.util.Lang.get("Add", "Tambah"), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        itemsIndexed(iqomahFooters) { index, footer ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = footer,
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = {
                        editTextValue = footer
                        editingIqomahIndex = index
                    }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color.Blue)
                    }
                    IconButton(onClick = {
                        val updated = iqomahFooters.toMutableList().apply { removeAt(index) }
                        onUpdateFooters(config.adzanFooterText, formatFooterTexts(updated))
                    }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                    }
                }
            }
        }

        // Preview Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TvFocusableButton(
                    onClick = onPreviewAdzan,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(40.dp).weight(1f)
                ) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Preview Adzan Screen", "Preview Layar Adzan"), color = Color.White)
                }

                TvFocusableButton(
                    onClick = onPreviewIqomah,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(40.dp).weight(1f)
                ) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Preview Iqomah Screen", "Preview Layar Iqomah"), color = Color.White)
                }
            }
        }

        // Section Divider
        item {
            Divider(color = Color.White.copy(alpha = 0.15f))
        }

        // Section: Dashboard Running Text
        item {
            Text(
                text = com.groovernapp.mosqueathan.util.Lang.get("Dashboard Running Text", "Teks Berjalan Dashboard"),
                color = Color(0xFFFFD54F),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newAnnouncementText,
                    onValueChange = { newAnnouncementText = it },
                    placeholder = { Text(com.groovernapp.mosqueathan.util.Lang.get("Type new announcement here...", "Ketik pengumuman baru di sini...")) },
                    colors = textFieldColors(),
                    modifier = Modifier.weight(1f)
                )
                TvFocusablePrimaryButton(
                    onClick = {
                        if (newAnnouncementText.isNotBlank()) {
                            onAddAnnouncement(newAnnouncementText.trim())
                            newAnnouncementText = ""
                        }
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Add", "Tambah"), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        itemsIndexed(announcements) { index, announcement ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = announcement.content,
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = {
                        editTextValue = announcement.content
                        editingAnnouncement = announcement
                    }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color.Blue)
                    }
                    IconButton(onClick = { onDeleteAnnouncement(announcement.id) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                    }
                }
            }
        }
    }
}

private fun parseFooterTexts(raw: String): List<String> {
    if (raw.trim().startsWith("[")) {
        try {
            val arr = org.json.JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            return list
        } catch (e: Exception) {
            // fallback
        }
    }
    return if (raw.isBlank()) emptyList() else listOf(raw)
}

private fun formatFooterTexts(list: List<String>): String {
    val arr = org.json.JSONArray()
    list.forEach { arr.put(it) }
    return arr.toString()
}
