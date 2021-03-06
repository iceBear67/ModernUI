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

package icyllis.modernui.impl.setting;

import icyllis.modernui.graphics.math.TextAlign;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.test.widget.UniformScrollEntry;

import javax.annotation.Nonnull;

/**
 * Single option line in settings interface
 */
public abstract class SettingEntry extends UniformScrollEntry {

    public String title;

    protected float titleBrightness = 0.85f;

    protected float centerX;

    public SettingEntry(SettingScrollWindow window, String title) {
        super(window, 320, SettingCategoryGroup.ENTRY_HEIGHT);
        this.title = title;
        //TODO tooltip description lines
        /*if (desc != null)
            this.desc = FontRendererTools.splitStringToWidth(desc, 150);*/
    }

    @Override
    public void locate(float px, float py) {
        super.locate(px, py);
        centerX = px;
    }

    @Override
    public final void onDraw(@Nonnull Canvas canvas, float time) {
        /*Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();*/
        //canvas.setColor(titleBrightness, titleBrightness, titleBrightness, 1);
        canvas.setTextAlign(TextAlign.LEFT);
        canvas.drawText(title, x1, y1 + 6);
        /*if (desc.length > 0) {

        }*/
        drawExtra(canvas, time);
        //canvas.setColor(0.55f, 0.55f, 0.55f, 0.863f);
        canvas.drawLine(x1, y2, x2, y2);
        /*RenderSystem.disableTexture();
        bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        GL11.glLineWidth(1.0f);
        bufferBuilder.pos(x1, y2, 0.0D).color(140, 140, 140, 220).endVertex();
        bufferBuilder.pos(x2, y2, 0.0D).color(140, 140, 140, 220).endVertex();
        tessellator.draw();
        RenderSystem.enableTexture();*/
    }

    protected abstract void drawExtra(Canvas canvas, float time);

    @Override
    public void onMouseHoverEnter(double mouseX, double mouseY) {
        super.onMouseHoverEnter(mouseX, mouseY);
        titleBrightness = 1.0f;
    }

    @Override
    public void onMouseHoverExit() {
        super.onMouseHoverExit();
        titleBrightness = 0.85f;
    }

}
