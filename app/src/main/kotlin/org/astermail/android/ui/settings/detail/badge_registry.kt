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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class BadgeVisual(val icon: ImageVector, val color: Color)

private val BADGE_VISUALS: Map<String, BadgeVisual> = mapOf(
    "big_bang" to BadgeVisual(Icons.Filled.AutoAwesome, Color(0xFFFBBF24)),
    "event_horizon" to BadgeVisual(Icons.Filled.DarkMode, Color(0xFF8B5CF6)),
    "black_hole" to BadgeVisual(Icons.Filled.Public, Color(0xFF6366F1)),
    "singularity" to BadgeVisual(Icons.Filled.Lightbulb, Color(0xFF94A3B8)),
    "supernova" to BadgeVisual(Icons.Filled.WbSunny, Color(0xFFF97316)),
    "andromeda" to BadgeVisual(Icons.Filled.Autorenew, Color(0xFFA855F7)),
    "nebula" to BadgeVisual(Icons.Filled.Cloud, Color(0xFFEC4899)),
    "comet" to BadgeVisual(Icons.Filled.Rocket, Color(0xFF0EA5E9)),
    "pulsar" to BadgeVisual(Icons.Filled.Bolt, Color(0xFF3B82F6)),
    "stargazer" to BadgeVisual(Icons.Filled.Star, Color(0xFF8B5CF6)),
    "founding_member" to BadgeVisual(Icons.Filled.EmojiEvents, Color(0xFFFACC15)),
    "early_supporter" to BadgeVisual(Icons.Filled.Favorite, Color(0xFFF43F5E)),
)

private val DEFAULT_BADGE_VISUAL = BadgeVisual(Icons.Filled.Star, Color(0xFF64748B))

fun badge_visual_for(slug: String): BadgeVisual = BADGE_VISUALS[slug] ?: DEFAULT_BADGE_VISUAL
