//
// Aster Communications Inc.
//
// Copyright (c) 2026 Aster Communications Inc.
//
// This file is part of this project.
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//

package org.astermail.android.ui.settings.detail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import org.astermail.android.R
import org.astermail.android.api.user.Badge
import org.astermail.android.ui.common.current_user_avatar
import org.astermail.android.design.AsterMaterial
import org.astermail.android.design.AsterSpacing
import org.astermail.android.design.components.AsterButton
import org.astermail.android.design.components.AsterCard
import org.astermail.android.design.components.AsterTextField
import org.astermail.android.settings.SaveStatus
import org.astermail.android.settings.SettingsViewModel
import org.astermail.android.ui.common.resolve_primary_sender_email
import org.astermail.android.ui.mail.SenderAvatar

@Composable
fun ProfileScreen(
    on_back: () -> Unit,
    on_open: (id: String) -> Unit = {},
) {
    val colors = AsterMaterial.colors
    val vm: SettingsViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.load_profile()
        vm.load_aliases()
        vm.load_ghost_aliases()
        vm.load_badges()
    }

    val live_account by vm.account_store.current_account.collectAsStateWithLifecycle(
        initialValue = vm.account_store.get_current()
    )
    val user = state.user
    val email = user?.email ?: live_account?.email ?: ""
    val primary_email = resolve_primary_sender_email(
        state.default_sender_id,
        email,
        state.aliases,
        state.ghost_aliases,
    )
    var display_name by remember { mutableStateOf(live_account?.display_name ?: user?.display_name ?: "") }
    LaunchedEffect(live_account?.display_name, user?.display_name) {
        val incoming = user?.display_name ?: live_account?.display_name ?: return@LaunchedEffect
        if (incoming != display_name) display_name = incoming
    }

    var photo_uploading by remember { mutableStateOf(false) }
    var photo_failed by remember { mutableStateOf(false) }

    val image_picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            photo_uploading = true
            photo_failed = false
            val data_uri = withContext(Dispatchers.IO) { read_image_as_data_uri(context, uri) }
            val success = if (data_uri != null) vm.update_profile_picture(data_uri) else false
            photo_uploading = false
            photo_failed = !success
        }
    }

    LaunchedEffect(state.save_status) {
        if (state.save_status == SaveStatus.SAVED) {
            kotlinx.coroutines.delay(1500)
            vm.reset_save_status()
        }
    }

    val name_save_label = when (state.save_status) {
        SaveStatus.SAVING -> stringResource(R.string.saving)
        SaveStatus.SAVED -> stringResource(R.string.saved)
        SaveStatus.ERROR -> stringResource(R.string.error_try_again)
        else -> stringResource(R.string.save)
    }
    val is_name_saving = state.save_status == SaveStatus.SAVING

    detail_scaffold(title = stringResource(R.string.profile), on_back = on_back) {
        AsterCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AsterSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                current_user_avatar(
                    account_store = vm.account_store,
                    size = 60.dp,
                )
                Spacer(Modifier.width(AsterSpacing.lg))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = listOfNotNull(
                            display_name.takeIf { it.isNotBlank() },
                            live_account?.display_name?.takeIf { it.isNotBlank() },
                            user?.display_name?.takeIf { it.isNotBlank() },
                            user?.username?.takeIf { it.isNotBlank() },
                            live_account?.email?.substringBefore("@")?.takeIf { it.isNotBlank() },
                        ).firstOrNull() ?: stringResource(R.string.your_name),
                        color = colors.text_primary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = primary_email,
                        color = colors.text_tertiary,
                        fontSize = 13.sp,
                    )
                    if (primary_email != email && email.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.also_receives_at, email),
                            color = colors.text_muted,
                            fontSize = 12.sp,
                        )
                    }
}
            }
        }
        v_gap(AsterSpacing.sm)
        AsterButton(
            label = when {
                photo_uploading -> stringResource(R.string.saving)
                photo_failed -> stringResource(R.string.error_try_again)
                else -> stringResource(R.string.change_photo)
            },
            onClick = {
                image_picker.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                    ),
                )
                photo_failed = false
            },
            enabled = !photo_uploading && !is_name_saving,
            is_loading = photo_uploading,
        )
        v_gap(AsterSpacing.lg)
        section_label(stringResource(R.string.display_name))
        AsterTextField(
            value = display_name,
            onValueChange = {
                display_name = it
                vm.reset_save_status()
            },
            placeholder = stringResource(R.string.your_name),
            enabled = !is_name_saving,
        )
        v_gap(AsterSpacing.sm)
        AsterButton(
            label = name_save_label,
            onClick = { vm.update_display_name(display_name) },
            enabled = display_name.isNotBlank() && !is_name_saving && state.save_status != SaveStatus.SAVED,
            is_loading = is_name_saving,
        )
        v_gap(AsterSpacing.lg)
        section_label(stringResource(R.string.email))
        AsterCard(modifier = Modifier.fillMaxWidth()) {
            detail_row(title = email, subtitle = stringResource(R.string.primary_address))
        }
        if (state.badges.isNotEmpty()) {
            v_gap(AsterSpacing.lg)
            section_label(stringResource(R.string.badges))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AsterSpacing.sm),
            ) {
                items(state.badges) { badge ->
                    badge_chip(badge)
                }
            }
        }
        v_gap(AsterSpacing.xxl)
    }
}

@Composable
private fun badge_chip(badge: Badge) {
    val bg_color = remember(badge.color) {
        try {
            Color(android.graphics.Color.parseColor(badge.color))
        } catch (_: Throwable) {
            Color(0xFF6366F1)
        }
    }
    val is_dark = remember(bg_color) {
        (bg_color.red * 0.299f + bg_color.green * 0.587f + bg_color.blue * 0.114f) < 0.5f
    }
    val text_color = if (is_dark) Color.White else Color(0xFF1A1A1A)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg_color)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = badge.icon,
                fontSize = 14.sp,
            )
            Text(
                text = badge.display_name,
                color = text_color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private val MAX_RAW_BYTES = 6 * 1024 * 1024

private fun read_image_as_data_uri(context: Context, uri: Uri): String? {
    return try {
        val stream = context.contentResolver.openInputStream(uri) ?: return null
        val bytes = stream.use { it.readBytes() }
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val allowed = setOf("image/jpeg", "image/png", "image/webp")

        val (final_bytes, final_mime) = if (bytes.size <= MAX_RAW_BYTES) {
            Pair(bytes, if (mime in allowed) mime else "image/jpeg")
        } else {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            var compressed: ByteArray? = null
            for (quality in listOf(80, 60, 40, 20)) {
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                val candidate = out.toByteArray()
                if (candidate.size <= MAX_RAW_BYTES) {
                    compressed = candidate
                    break
                }
            }
            bitmap.recycle()
            Pair(compressed ?: return null, "image/jpeg")
        }

        val b64 = android.util.Base64.encodeToString(final_bytes, android.util.Base64.NO_WRAP)
        "data:$final_mime;base64,$b64"
    } catch (_: Throwable) {
        null
    }
}
