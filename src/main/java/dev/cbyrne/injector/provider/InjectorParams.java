/*
 *     Injector is a runtime class modification library for Kotlin
 *     Copyright (C) 2021  Conor Byrne
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.cbyrne.injector.provider;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InjectorParams {
    private final List<Object> params;
    private final Map<String, Object> fields;
    private final ReturnInfo retInfo;

    public InjectorParams(
            @Nullable List<Object> params,
            @Nullable Map<String, Object> fields,
            ReturnInfo retInfo
    ) {
        this.params = params == null ? new ArrayList<>() : params;
        this.fields = fields == null ? new HashMap<>() : fields;
        this.retInfo = retInfo;
    }

    public List<Object> component1() {
        return this.params;
    }

    public Map<String, Object> component2() {
        return this.fields;
    }

    public ReturnInfo component3() {
        return this.retInfo;
    }
}
