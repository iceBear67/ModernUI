/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.api.animation;

public interface IAnimation {

    /**
     * Update animations before drawing
     * @param currentTime floating point ticks, 20.0 ticks = 1 second
     */
    void update(float currentTime);

    default void resize(int width, int height) {}

    /**
     * If return true, this instance will be removed from render loop
     * @return whether to remove
     */
    default boolean shouldRemove() {
        return false;
    }
}