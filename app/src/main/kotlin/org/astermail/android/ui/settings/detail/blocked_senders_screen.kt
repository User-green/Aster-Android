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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.astermail.android.R
import org.astermail.android.design.AsterMaterial
import org.astermail.android.design.AsterSpacing
import org.astermail.android.design.components.AsterButton
import org.astermail.android.design.components.AsterCard
import org.astermail.android.design.components.AsterDivider
import org.astermail.android.design.components.AsterGhostButton
import org.astermail.android.settings.SettingsViewModel

@Composable
fun BlockedSendersScreen(
    on_back: () -> Unit,
    on_open: (id: String) -> Unit = {},
) {
    val vm: SettingsViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = AsterMaterial.colors
    var show_add_dialog by remember { mutableStateOf(false) }
    var add_email by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.load_blocked_senders() }

    detail_scaffold(title = stringResource(R.string.blocked_senders), on_back = on_back) {
        AsterButton(
            label = "Block a sender",
            onClick = { show_add_dialog = true },
            modifier = Modifier.fillMaxWidth(),
        )
        v_gap(AsterSpacing.md)

        if (state.is_loading && state.blocked_senders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(AsterSpacing.xxl),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = colors.accent_blue, modifier = Modifier.size(24.dp))
            }
        } else if (state.blocked_senders.isEmpty()) {
            AsterCard(modifier = Modifier.fillMaxWidth()) {
                detail_row(
                    title = stringResource(R.string.no_blocked_senders),
                    subtitle = state.error ?: stringResource(R.string.no_blocked_senders_subtitle),
                )
            }
        } else {
            AsterCard(modifier = Modifier.fillMaxWidth()) {
                state.blocked_senders.filter { it.address.isNotBlank() }.forEachIndexed { idx, sender ->
                    detail_row(
                        title = sender.address,
                        subtitle = if (sender.blocked_count > 0) stringResource(R.string.blocked_count, sender.blocked_count) else null,
                        trailing = {
                            AsterGhostButton(
                                label = stringResource(R.string.unblock),
                                onClick = { vm.unblock_sender(sender.address) },
                            )
                        },
                    )
                    if (idx < state.blocked_senders.filter { it.address.isNotBlank() }.lastIndex) AsterDivider(modifier = Modifier)
                }
            }
        }
        v_gap(AsterSpacing.xxl)
    }

    if (show_add_dialog) {
        AlertDialog(
            onDismissRequest = { show_add_dialog = false; add_email = "" },
            containerColor = colors.bg_card,
            title = {
                Text("Block sender", color = colors.text_primary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Column {
                    Text(
                        "Enter the email address to block. You won't receive messages from this sender.",
                        color = colors.text_secondary,
                        fontSize = 13.sp,
                    )
                    v_gap(AsterSpacing.md)
                    OutlinedTextField(
                        value = add_email,
                        onValueChange = { add_email = it.trim().lowercase() },
                        label = { Text("Email address") },
                        placeholder = { Text("e.g. spam@example.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (add_email.isNotBlank()) {
                            vm.block_sender(add_email)
                            show_add_dialog = false
                            add_email = ""
                        }
                    },
                    enabled = add_email.contains("@"),
                ) {
                    Text("Block", color = colors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { show_add_dialog = false; add_email = "" }) {
                    Text("Cancel")
                }
            },
        )
    }
}
