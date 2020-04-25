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

import com.google.gson.annotations.Expose;
import icyllis.modernui.font.FontTools;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.*;
import icyllis.modernui.gui.math.Align9D;
import icyllis.modernui.gui.math.Locator;
import icyllis.modernui.impl.module.SettingAudio;
import icyllis.modernui.system.ConstantsLibrary;
import net.minecraft.client.resources.I18n;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiFunction;

public class MenuButton extends IconButton {

    private final AnimationControl sideTextAC = new SideTextControl(this);

    private final String text;
    private final int id;

    private float frameAlpha = 0;
    private float textAlpha = 0;
    private float frameSizeW = 5;

    public MenuButton(Module module, Builder builder) {
        super(module, builder);
        this.text = I18n.format(builder.text);
        this.id = builder.id;
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        super.draw(canvas, time);
        sideTextAC.update();
        if (sideTextAC.isAnimationOpen()) {
            canvas.setRGBA(0.0f, 0.0f, 0.0f, 0.5f * frameAlpha);
            canvas.drawRoundedRect(x1 + 27, y1 + 1, x1 + 32 + frameSizeW, y1 + 15, 6);
            canvas.setRGBA(0.5f, 0.5f, 0.5f, frameAlpha);
            canvas.drawRoundedRectFrame(x1 + 27, y1 + 1, x1 + 32 + frameSizeW, y1 + 15, 6);
            canvas.setRGBA(1.0f, 1.0f, 1.0f, textAlpha);
            canvas.drawText(text, x1 + 32, y1 + 4);
        }
    }

    /*@Override
    public void draw(float time) {
        super.draw(time);
        sideText.draw(time);

        //RenderSystem.pushMatrix();

        //RenderSystem.color3f(brightness, brightness, brightness);
        DrawTools.INSTANCE.setRGBA(brightness, brightness, brightness, 1.0f);
        //RenderSystem.scalef(0.5f, 0.5f, 1);
        //textureManager.bindTexture(ConstantsLibrary.ICONS);
        *//*GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);*//*
        //DrawTools.blitFinal(x1, x1 + 16, y1, y1 + 16, u / 512, (u + 64) / 512, 0, 64 / 512f);
        DrawTools.INSTANCE.drawIcon(icon, x1, y1, x1 + 16, y1 + 16);

        //RenderSystem.popMatrix();

        // draw text box on right side
        // ingame menu would be opened in the game initialization phase, so font renderer shouldn't be called because there's no proper GL state !!
        if (sideText.isAnimationOpen()) {
            DrawTools.INSTANCE.setRGBA(0.0f, 0.0f, 0.0f, 0.5f * frameAlpha);
            DrawTools.INSTANCE.drawRoundedRect(x1 + 27, y1 + 1, x1 + 32 + frameSizeW, y1 + 15, 6);
            DrawTools.INSTANCE.setRGBA(0.5f, 0.5f, 0.5f, frameAlpha);
            DrawTools.INSTANCE.drawRoundedRectFrame(x1 + 27, y1 + 1, x1 + 32 + frameSizeW, y1 + 15, 6);
            //DrawTools.fillRectWithFrame(x1 + 27, y1 + 1, x1 + 31 + frameSizeW, y1 + 15, 0.51f, 0x000000, 0.4f * frameAlpha, 0x404040, 0.8f * frameAlpha);
            DrawTools.INSTANCE.setRGBA(1.0f, 1.0f, 1.0f, textAlpha);
            DrawTools.INSTANCE.drawText(text, x1 + 32, y1 + 4); // called font renderer
        }
    }*/

    @Override
    protected void onMouseHoverEnter() {
        super.onMouseHoverEnter();
        sideTextAC.startOpenAnimation();
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        sideTextAC.startCloseAnimation();
    }

    public float getTextLength() {
        return FontTools.getStringWidth(text);
    }

    public void setFrameAlpha(float frameAlpha) {
        this.frameAlpha = frameAlpha;
    }

    public void setTextAlpha(float textAlpha) {
        this.textAlpha = textAlpha;
    }

    public void setFrameSizeW(float frameSizeW) {
        this.frameSizeW = frameSizeW;
    }

    public void onModuleChanged(int id) {
        iconAC.setLockState(this.id == id);
        if (iconAC.canChangeState()) {
            if (!isMouseHovered()) {
                iconAC.startCloseAnimation();
            }
        }
    }

    @Nonnull
    @Override
    public Class<? extends Widget.Builder> getBuilder() {
        return Builder.class;
    }

    public static class Builder extends IconButton.Builder {

        @Expose
        public final String text;

        @Expose
        public final int id;

        public Builder(String text, int uIndex, int id) {
            super(new Icon(ConstantsLibrary.ICONS, uIndex * 64 / 512f, 0, (uIndex + 1) * 64 / 512f, 64 / 512f, true));
            this.text = text;
            this.id = id;
        }

        @Override
        public Builder setWidth(float width) {
            super.setWidth(width);
            return this;
        }

        @Override
        public Builder setHeight(float height) {
            super.setHeight(height);
            return this;
        }

        @Override
        public Builder setLocator(@Nonnull Locator locator) {
            super.setLocator(locator);
            return this;
        }

        @Override
        public Builder setAlign(@Nonnull Align9D align) {
            super.setAlign(align);
            return this;
        }

        @Override
        public MenuButton build(Module module) {
            return new MenuButton(module, this);
        }
    }

    private static class SideTextControl extends AnimationControl {

        private final MenuButton instance;

        public SideTextControl(MenuButton instance) {
            this.instance = instance;
        }

        @Override
        protected void createOpenAnimations(@Nonnull List<Animation> list) {
            list.add(new Animation(3, true)
                    .applyTo(new Applier(0.0f, instance.getTextLength() + 5.0f, instance::setFrameSizeW)));
            list.add(new Animation(3)
                    .applyTo(new Applier(1.0f, instance::setFrameAlpha)));
            list.add(new Animation(3)
                    .applyTo(new Applier(1.0f, instance::setTextAlpha))
                    .withDelay(2));
        }

        @Override
        protected void createCloseAnimations(@Nonnull List<Animation> list) {
            list.add(new Animation(5)
                    .applyTo(new Applier(1.0f, 0.0f, v -> {
                        instance.setTextAlpha(v);
                        instance.setFrameAlpha(v);
                    })));
        }
    }
}
