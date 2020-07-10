/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.test;

import icyllis.modernui.ui.test.IHost;
import icyllis.modernui.ui.test.IWidget;
import icyllis.modernui.ui.widget.Scroller;

@Deprecated
public interface IScrollHost extends IHost, IWidget {

    /**
     * Get scroll offset without top and bottom margin
     *
     * @return scroll amount (gt 0)
     */
    float getVisibleOffset();

    float getMargin();

    void layoutList();

    float getMaxScrollAmount();

    void callbackScrollAmount(float scrollAmount);

    Scroller getScrollController();
}