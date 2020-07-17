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

package icyllis.modernui.ui.example;

import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.ui.animation.Animation;
import icyllis.modernui.ui.animation.Applier;
import icyllis.modernui.ui.animation.ITimeInterpolator;
import icyllis.modernui.ui.layout.Gravity;
import icyllis.modernui.ui.layout.LinearLayout;
import icyllis.modernui.ui.layout.Orientation;
import icyllis.modernui.ui.master.UIManager;
import icyllis.modernui.ui.master.View;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;

public class TestLinearLayout extends LinearLayout {

    public TestLinearLayout() {
        setOrientation(Orientation.VERTICAL);
        setGravity(Gravity.CENTER);
        setDivider(new Drawable() {
            @Override
            public void draw(@Nonnull Canvas canvas) {
                canvas.moveTo(this);
                canvas.setColor(192, 192, 192, 128);
                canvas.drawLine(0, 0, getWidth(), 0);
            }

            @Override
            public int getIntrinsicHeight() {
                return 1;
            }
        });
        setShowDividers(SHOW_DIVIDER_MIDDLE | SHOW_DIVIDER_END);
        setDividerPadding(8);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(100, 18);
        for (int i = 0; i < 8; i++) {
            addView(new CView(), p);
        }
        addView(new DView(ITimeInterpolator.DECELERATE, 0), new LinearLayout.LayoutParams(60, 20));
        //addView(new DView(ITimeInterpolator.VISCOUS_FLUID, 30), new LinearLayout.LayoutParams(60, 20));
    }

    @Override
    protected void onDraw(@Nonnull Canvas canvas) {
        super.onDraw(canvas);
        canvas.moveTo(this);
        canvas.resetColor();
        float a = canvas.drawText("LinearLayout", 0, 0);
        canvas.drawText(String.valueOf(canvas.getDrawingTime()), a + 2, 0);
    }

    private static class CView extends View {

        @Override
        protected void onDraw(@Nonnull Canvas canvas) {
            canvas.moveTo(this);
            canvas.resetColor();
            String str = TextFormatting.UNDERLINE + "Modern" + TextFormatting.AQUA + " UI " + TextFormatting.RESET + "\u0629\u064a\u0628\u0631\u0639\u0644\u0627" + TextFormatting.STRIKETHROUGH + "\u2642";
            canvas.drawText(str, 0, 4);
        }
    }

    private static class DView extends View {

        private final Animation animation;

        private float offsetY;

        private final int offset;

        public DView(ITimeInterpolator interpolator, int offset) {
            this.offset = offset;
            animation = new Animation(200)
                    .applyTo(new Applier(0, 60, () -> offsetY, v -> offsetY = v).setInterpolator(interpolator));
            animation.invertFull();
        }

        @Override
        protected void onDraw(@Nonnull Canvas canvas) {
            canvas.moveTo(this);
            canvas.drawText(TextFormatting.UNDERLINE + "G" + TextFormatting.RESET + " " + TextFormatting.STRIKETHROUGH + "Uko", offset, offsetY + 4);
        }

        @Override
        protected void tick(int ticks) {
            super.tick(ticks);
            /*if (ticks % 40 == 0) {
                animation.invert();
            } else if (ticks % 20 == 0) {
                animation.start();
            }*/
        }
    }
}
