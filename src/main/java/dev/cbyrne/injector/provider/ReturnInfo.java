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

public class ReturnInfo {
    private boolean cancelled;
    @Nullable
    private Object returnValue;

    public ReturnInfo() {
        this(false, null);
    }

    public ReturnInfo(@Nullable Object returnValue) {
        this(false, returnValue);
    }

    public ReturnInfo(boolean cancelled, @Nullable Object returnValue) {
        this.cancelled = cancelled;
        this.returnValue = returnValue;
    }

    public void cancel(@Nullable Object value) {
        this.cancelled = true;
        this.returnValue = value;
    }

    public boolean component1() {
        return this.cancelled;
    }

    @Nullable
    public Object component2() {
        return this.returnValue;
    }

    public boolean getCancelled() {
        return this.cancelled;
    }

    @Nullable
    public Object getReturnValue() {
        return this.returnValue;
    }
}