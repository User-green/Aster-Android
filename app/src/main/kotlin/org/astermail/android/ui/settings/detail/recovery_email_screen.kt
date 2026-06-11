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

import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.astermail.android.R
import org.astermail.android.design.AsterMaterial
import org.astermail.android.design.AsterSpacing
import org.astermail.android.design.components.AsterButton
import org.astermail.android.design.components.AsterCard
import org.astermail.android.design.components.AsterGhostButton
import org.astermail.android.design.components.AsterTextField
import org.astermail.android.settings.SaveStatus
import org.astermail.android.settings.SettingsViewModel

@Composable
fun RecoveryEmailScreen(on_back: () -> Unit) {
    val vm: SettingsViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = AsterMaterial.colors
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.load_recovery_email() }

    var email by remember(state.recovery_email_address) {
        mutableStateOf(state.recovery_email_address ?: "")
    }

    LaunchedEffect(state.save_status) {
        if (state.save_status == SaveStatus.SAVED) {
            Toast.makeText(
                context,
                context.getString(R.string.recovery_email_saved),
                Toast.LENGTH_SHORT,
            ).show()
            vm.reset_save_status()
        }
    }

    LaunchedEffect(state.action_result) {
        state.action_result?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.clear_action_result()
        }
    }

    detail_scaffold(title = stringResource(R.string.recovery_email), on_back = on_back) {
        Text(
            text = stringResource(R.string.backup_email_description),
            color = colors.text_tertiary,
            fontSize = 14.sp,
        )
        v_gap(AsterSpacing.lg)

        if (state.recovery_email_set) {
            AsterCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AsterSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.recovery_email_address
                                ?: stringResource(R.string.recovery_email),
                            color = colors.text_primary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = if (state.recovery_email_verified) {
                                stringResource(R.string.recovery_email_status_verified)
                            } else {
                                stringResource(R.string.recovery_email_status_unverified)
                            },
                            color = colors.text_tertiary,
                            fontSize = 13.sp,
                        )
                    }
                    if (state.recovery_email_verified) {
                        verified_badge(stringResource(R.string.verified))
                    }
                }
            }
            v_gap(AsterSpacing.lg)
        }

        state.error?.let {
            error_banner(it)
            v_gap(AsterSpacing.lg)
        }

        AsterTextField(
            value = email,
            onValueChange = { email = it },
            label = stringResource(R.string.recovery_email),
            placeholder = stringResource(R.string.recovery_email_placeholder),
            keyboard_options = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
            leading_icon = {
                Icon(Icons.Filled.Email, null, tint = colors.text_muted)
            },
        )
        v_gap(AsterSpacing.lg)

        AsterButton(
            label = if (state.recovery_email_set) {
                stringResource(R.string.update_recovery_email)
            } else {
                stringResource(R.string.save_recovery_email)
            },
            onClick = { vm.save_recovery_email(email) },
            enabled = email.trim().contains("@") && state.save_status != SaveStatus.SAVING,
            is_loading = state.save_status == SaveStatus.SAVING,
        )

        if (state.recovery_email_set && !state.recovery_email_verified) {
            v_gap(AsterSpacing.sm)
            AsterGhostButton(
                label = stringResource(R.string.recovery_email_resend),
                onClick = { vm.resend_recovery_verification() },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (state.recovery_email_set) {
            v_gap(AsterSpacing.sm)
            AsterGhostButton(
                label = stringResource(R.string.recovery_email_remove),
                onClick = { vm.remove_recovery_email() },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.save_status != SaveStatus.SAVING,
            )
        }

        v_gap(AsterSpacing.xxl)
    }
}
