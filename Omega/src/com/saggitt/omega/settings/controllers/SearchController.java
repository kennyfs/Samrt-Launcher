/*
 *  This file is part of Omega Launcher
 *  Copyright (c) 2021   Saul Henriquez
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.saggitt.omega.settings.controllers;

import android.content.Context;

import com.saggitt.omega.preferences.PreferenceController;
import com.saggitt.omega.search.SearchProvider;
import com.saggitt.omega.search.SearchProviderController;
import com.saggitt.omega.search.webproviders.WebSearchProvider;

import org.jetbrains.annotations.NotNull;

public class SearchController extends PreferenceController {
    private SearchProvider provider;

    //TODO: implement a listener
    public SearchController(@NotNull Context context) {
        super(context);
        provider = SearchProviderController.Companion.getInstance(context).getSearchProvider();
    }

    @Override
    public boolean isEnabled() {
        return !(provider instanceof WebSearchProvider);
    }
}
