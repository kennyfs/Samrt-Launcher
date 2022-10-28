/*
 * This file is part of Neo Launcher
 * Copyright (c) 2022   Neo Launcher Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.saggitt.omega.compose.screens.preferences

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.saggitt.omega.compose.components.ListItemWithIcon
import com.saggitt.omega.compose.components.ViewWithActionBar
import com.saggitt.omega.compose.components.move
import com.saggitt.omega.dashProviderOptions
import com.saggitt.omega.iconIds
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@Composable
fun EditDashPage() {
    val context = LocalContext.current
    val prefs = Utilities.getOmegaPrefs(context)
    val iconList = iconIds

    val allItems = dashProviderOptions

    val enabled = allItems.filter {
        it.key in prefs.dashProvidersItems.onGetValue()
    }.map { DashItem(it.key, it.value) }.associateBy { it.key }

    val enabledSorted = prefs.dashProvidersItems.onGetValue().mapNotNull { enabled[it] }

    val disabled = allItems.filter {
        it.key !in prefs.dashProvidersItems.onGetValue()
    }.map { DashItem(it.key, it.value) }

    val enabledItems = remember { mutableStateListOf(*enabledSorted.toTypedArray()) }
    val disabledItems = remember { mutableStateOf(disabled) }

    val state = rememberReorderableLazyListState(onMove = { from, to ->
        enabledItems.move(
            (from.index - 2).coerceIn(0, enabledItems.size - 1),
            (to.index - 2).coerceIn(0, enabledItems.size - 1)
        )
    })

    ViewWithActionBar(
        title = stringResource(id = R.string.edit_dash),
        onBackAction = {
            val enabledKeys = enabledItems.map { it.key }
            prefs.dashProvidersItems.onSetValue(enabledKeys)
        }
    ) { paddingValues ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(
                    start = 8.dp,
                    end = 8.dp,
                    bottom = paddingValues.calculateBottomPadding() + 8.dp
                ),
            state = state.listState
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.enabled_events),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            itemsIndexed(enabledItems, { i, it -> it.key }) { index, item ->
                ReorderableItem(state, key = item.key, index = index + 2) { isDragging ->
                    val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp)
                    ListItemWithIcon(
                        title = stringResource(id = item.titleResId),
                        modifier = Modifier
                            .composed { jit(index) }
                            .clickable {
                                enabledItems.remove(item)
                                val tempList = disabledItems.value.toMutableList()
                                tempList.add(0, item)
                                disabledItems.value = tempList
                            },
                        startIcon = {
                            Icon(
                                painter = painterResource(
                                    id = iconList[item.key] ?: R.drawable.ic_edit_dash
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(30.dp)
                            )
                        },
                        endCheckbox = {
                            IconButton(
                                modifier = Modifier.size(36.dp),
                                onClick = {
                                    enabledItems.remove(item)
                                    val tempList = disabledItems.value.toMutableList()
                                    tempList.add(0, item)
                                    disabledItems.value = tempList
                                }
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_drag_handle),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        verticalPadding = 6.dp
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(
                    Modifier
                        .height(1.dp)
                        .padding(horizontal = 32.dp)
                )
            }
            item {
                Text(
                    text = stringResource(id = R.string.drag_to_enable_packs),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }
            items(disabledItems.value) { item ->
                ListItemWithIcon(
                    title = stringResource(id = item.titleResId),
                    modifier = Modifier.clickable {
                        disabledItems.value = disabledItems.value - item
                        enabledItems.add(item)
                    },
                    startIcon = {
                        Image(
                            painter = painterResource(
                                id = iconList[item.key] ?: R.drawable.ic_edit_dash
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp)
                        )
                    },
                    endCheckbox = {
                        IconButton(
                            modifier = Modifier.size(36.dp),
                            onClick = {
                                disabledItems.value = disabledItems.value - item
                                val tempList = enabledItems.toMutableList()
                                tempList.add(0, item)
                                enabledItems.clear()
                                enabledItems.addAll(tempList)
                            }
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_drag_handle),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    verticalPadding = 6.dp
                )
            }
        }
    }

    DisposableEffect(key1 = null) {
        onDispose {
            val enabledKeys = enabledItems.map { it.key }
            prefs.dashProvidersItems.onSetValue(enabledKeys)
        }
    }
}

data class DashItem(val key: String, val titleResId: Int)