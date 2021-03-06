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

package icyllis.modernui.test.discard;

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.ScrollController;
import icyllis.modernui.test.widget.UniformScrollEntry;
import icyllis.modernui.test.widget.UniformScrollGroup;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * Light-weighted scroll window with fixed size, and can only use single uniform scroll group
 */
@Deprecated
public class ScrollPanel<E extends UniformScrollEntry, G extends UniformScrollGroup<E>> extends Widget implements IScrollHost {

    @Nonnull
    protected final G group;

    protected float scrollAmount = 0f;

    protected View.ScrollBar scrollbar;

    protected ScrollController controller;

    /**
     * This cannot used in inner class
     */
    public ScrollPanel(IHost host, @Nonnull Builder builder, @Nonnull Function<ScrollPanel<E, G>, G> group) {
        super(host, builder);
        this.group = group.apply(this);
        /*this.scrollbar = new ScrollBar(this);
        this.controller = new ScrollController(this);*/
    }

    @Override
    protected void onDraw(@Nonnull Canvas canvas, float time) {
        //controller.update(time);

        canvas.clipStart(x1, y1, width, height);

        canvas.save();
        canvas.translate(0, -getVisibleOffset());
        group.draw(canvas, time);
        canvas.restore();

        //scrollbar.draw(canvas, time);

        canvas.clipEnd();
    }

    /*@Override
    public void resize(int width, int height) {
        super.resize(width, height);
        this.scrollbar.setPos(this.x2 - scrollbar.barThickness - 1, y1 + 1);
        this.scrollbar.setHeight(this.height - 2);
        this.layoutList();
    }

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (super.updateMouseHover(mouseX, mouseY)) {
            if (scrollbar.updateMouseHover(mouseX, mouseY)) {
                group.setMouseHoverExit();
            } else {
                group.updateMouseHover(mouseX, mouseY + getVisibleOffset());
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean dispatchMouseClick(double mouseX, double mouseY, int mouseButton) {
        if (scrollbar.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        return group.mouseClicked(mouseX, mouseY + getVisibleOffset(), mouseButton);
    }

    @Override
    public boolean dispatchMouseRelease(double mouseX, double mouseY, int mouseButton) {
        if (scrollbar.mouseReleased(mouseX, mouseY, mouseButton)) {
            return true;
        }
        return group.mouseReleased(mouseX, mouseY + getVisibleOffset(), mouseButton);
    }

    @Override
    protected boolean onMouseScrolled(double amount) {
        if (scrollbar.mouseScrolled(, amount)) {
            return true;
        }
        if (group.mouseScrolled(, amount)) {
            return true;
        }
        controller.scrollSmoothBy(Math.round(amount * -20f));
        onScrollPanel(amount);
        return true;
    }

    protected void onScrollPanel(double amount) {

    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        scrollbar.setMouseHoverExit();
        group.setMouseHoverExit();
    }*/

    @Override
    public void callbackScrollAmount(float scrollAmount) {
        this.scrollAmount = scrollAmount;
        updateScrollBarOffset();
        group.updateVisible(y1 + getVisibleOffset(), y2 + getVisibleOffset());
        refocusMouseCursor();
    }

    public float getScrollPercentage() {
        float max = getMaxScrollAmount();
        if (max == 0) {
            return 0;
        }
        return scrollAmount / max;
    }

    public void updateScrollBarOffset() {
        //scrollbar.setBarOffset(getScrollPercentage());
    }

    @Override
    public float getMaxScrollAmount() {
        return Math.max(0, group.getHeight() - getHeight());
    }

    @Override
    public ScrollController getScrollController() {
        return controller;
    }

    @Override
    public float getVisibleOffset() {
        return scrollAmount;
    }

    @Override
    public float getMargin() {
        return 0;
    }

    @Override
    public void layoutList() {
        group.locate((x1 + x2) / 2f, y1);
        updateScrollBarLength();
        // update all scroll data
        //controller.scrollDirectBy(0);
    }

    public void updateScrollBarLength() {
        float v = getHeight();
        float t = group.getHeight();
        boolean renderBar = t > v;
        //scrollbar.setVisibility(renderBar ? View.VISIBLE : View.INVISIBLE);
        if (renderBar) {
            float p = v / t;
            //scrollbar.setBarLength(p);
        }
    }

    @Override
    public double getRelativeMouseY() {
        return getParent().getRelativeMouseY() + getVisibleOffset();
    }

    @Override
    public float toAbsoluteY(float ry) {
        return getParent().toAbsoluteY(ry) - getVisibleOffset();
    }
}
