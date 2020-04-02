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

package icyllis.modernui.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.font.TextAlign;
import icyllis.modernui.font.TextTools;
import icyllis.modernui.font.IFontRenderer;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.element.IElement;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.util.Color3I;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.function.Consumer;

public class DropDownMenu implements IWidget {

    private static int ENTRY_HEIGHT = 13;

    private IFontRenderer fontRenderer = TextTools.FONT_RENDERER;

    private final List<String> list;

    private final float vHeight;

    private final int selectedIndex;

    private final float reservedSpace;

    private final Consumer<Integer> receiver;

    private float x2, y;

    private float width, height;

    private boolean upward = false;

    private int hoveredIndex = -1;

    private float textAlpha = 0;

    private boolean initResize = false;

    public DropDownMenu(List<String> contents, int selectedIndex, float reservedSpace, Consumer<Integer> receiver) {
        list = contents;
        this.selectedIndex = selectedIndex;
        width = list.stream().distinct().mapToInt(s -> (int) fontRenderer.getStringWidth(s)).max().orElse(0) + 7;
        vHeight = list.size() * ENTRY_HEIGHT;
        this.reservedSpace = reservedSpace;
        this.receiver = receiver;
        GlobalModuleManager.INSTANCE.addAnimation(new Animation(4, true)
                .applyTo(new Applier(0, vHeight, value -> height = value)));
        GlobalModuleManager.INSTANCE.addAnimation(new Animation(3)
                .applyTo(new Applier(1, value -> textAlpha = value))
                .withDelay(3));
    }

    public void draw() {
        this.draw(0);
    }

    @Override
    public void draw(float currentTime) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableAlphaTest();
        RenderSystem.disableTexture();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        float left = x2 - width;
        float bottom = upward ? y + vHeight : y + height;

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        if (upward) {
            bufferBuilder.pos(left, bottom, 0.0D).color(8, 8, 8, 160).endVertex();
            bufferBuilder.pos(x2, bottom, 0.0D).color(8, 8, 8, 160).endVertex();
            bufferBuilder.pos(x2, bottom - height, 0.0D).color(8, 8, 8, 160).endVertex();
            bufferBuilder.pos(left, bottom - height, 0.0D).color(8, 8, 8, 160).endVertex();
        } else {
            bufferBuilder.pos(left, bottom, 0.0D).color(8, 8, 8, 160).endVertex();
            bufferBuilder.pos(x2, bottom, 0.0D).color(8, 8, 8, 160).endVertex();
            bufferBuilder.pos(x2, y, 0.0D).color(8, 8, 8, 160).endVertex();
            bufferBuilder.pos(left, y, 0.0D).color(8, 8, 8, 160).endVertex();
        }
        tessellator.draw();

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        bufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        GL11.glLineWidth(1.0F);
        if (upward) {
            bufferBuilder.pos(left, bottom, 0.0D).color(255, 255, 255, 80).endVertex();
            bufferBuilder.pos(x2, bottom, 0.0D).color(255, 255, 255, 80).endVertex();
            bufferBuilder.pos(x2, bottom - height, 0.0D).color(255, 255, 255, 80).endVertex();
            bufferBuilder.pos(left, bottom - height, 0.0D).color(255, 255, 255, 80).endVertex();
        } else {
            bufferBuilder.pos(left, bottom, 0.0D).color(255, 255, 255, 80).endVertex();
            bufferBuilder.pos(x2, bottom, 0.0D).color(255, 255, 255, 80).endVertex();
            bufferBuilder.pos(x2, y, 0.0D).color(255, 255, 255, 80).endVertex();
            bufferBuilder.pos(left, y, 0.0D).color(255, 255, 255, 80).endVertex();
        }
        tessellator.draw();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        RenderSystem.enableTexture();
        for (int i = 0; i < list.size(); i++) {
            String text = list.get(i);
            float cy = y + ENTRY_HEIGHT * i;
            if (i == hoveredIndex) {
                RenderSystem.disableTexture();
                bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                bufferBuilder.pos(left, cy + ENTRY_HEIGHT, 0.0D).color(128, 128, 128, 80).endVertex();
                bufferBuilder.pos(x2, cy + ENTRY_HEIGHT, 0.0D).color(128, 128, 128, 80).endVertex();
                bufferBuilder.pos(x2, cy, 0.0D).color(128, 128, 128, 80).endVertex();
                bufferBuilder.pos(left, cy, 0.0D).color(128, 128, 128, 80).endVertex();
                tessellator.draw();
                RenderSystem.enableTexture();
            }
            if (selectedIndex == i) {
                fontRenderer.drawString(text, x2 - 3, cy + 2, Color3I.BLUE_C, textAlpha, TextAlign.RIGHT);
            } else {
                fontRenderer.drawString(text, x2 - 3, cy + 2, Color3I.WHILE, textAlpha, TextAlign.RIGHT);
            }
        }
    }

    public void setPos(float x2, float y, float height) {
        this.x2 = x2;
        this.y = y;
        float vH = vHeight + reservedSpace;
        upward = y + vH >= height;
        if (upward) {
            this.y -= vH;
        }
    }

    @Override
    public void resize(int width, int height) {
        if (!initResize) {
            initResize = true;
            return;
        }
        GlobalModuleManager.INSTANCE.closePopup();
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (isMouseOver(mouseX, mouseY)) {
            int pIndex = (int) ((mouseY - y) / ENTRY_HEIGHT);
            if (pIndex >= 0 && pIndex < list.size()) {
                hoveredIndex = pIndex;
            } else {
                hoveredIndex = -1;
            }
        } else {
            hoveredIndex = -1;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0 && hoveredIndex != -1) {
            receiver.accept(hoveredIndex);
        }
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x2 - width && mouseX <= x2 && mouseY >= y && mouseY <= y + height;
    }
}