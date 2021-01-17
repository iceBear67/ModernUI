/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import icyllis.modernui.mcimpl.mixin.AccessGameRenderer;
import icyllis.modernui.mcimpl.mixin.AccessPostChain;
import icyllis.modernui.view.IMuiScreen;
import icyllis.modernui.view.UIManager;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public enum BlurHandler {
    INSTANCE;

    /**
     * Config values
     */
    public static boolean sBlurEffect;
    public static float sAnimationDuration;
    public static float sBlurRadius;
    public static float sBackgroundAlpha;

    // minecraft namespace
    private final ResourceLocation bilinearBlur = new ResourceLocation("shaders/post/blur_fast.json");

    private final Minecraft minecraft = Minecraft.getInstance();

    private final Set<Class<?>> blacklist = new ObjectArraySet<>();

    /**
     * If is playing animation
     */
    private boolean fadingIn;

    /**
     * If blur post-processing shader is activated
     */
    private boolean blurring;

    /**
     * If a screen excluded, the other screens that opened after this screen won't be blurred, unless current screen closed
     */
    private boolean screenOpened;

    /**
     * Background alpha
     */
    private float backgroundAlpha;

    BlurHandler() {

    }

    /**
     * Use blur shader in game renderer post-processing.
     */
    public void count(@Nullable Screen nextScreen) {
        if (minecraft.level == null) {
            return;
        }
        final boolean excluded;
        if (nextScreen == null || nextScreen instanceof IMuiScreen) {
            excluded = false;
        } else {
            Class<?> t = nextScreen.getClass();
            excluded = blacklist.stream().anyMatch(c -> c.isAssignableFrom(t));
        }
        boolean blurDisabled = excluded || !sBlurEffect;
        if (blurDisabled && excluded && blurring) {
            minecraft.gameRenderer.shutdownEffect();
            fadingIn = false;
            blurring = false;
        }

        boolean hasGui = nextScreen != null;
        GameRenderer gr = minecraft.gameRenderer;
        if (hasGui && !blurring && !screenOpened) {
            if (!blurDisabled && gr.currentEffect() == null) {
                ((AccessGameRenderer) gr).callLoadEffect(bilinearBlur);
                blurring = true;
                if (sAnimationDuration <= 0) {
                    updateRadius(sBlurRadius);
                }
            }
            if (sAnimationDuration > 0) {
                fadingIn = true;
                backgroundAlpha = 0;
            } else {
                fadingIn = false;
                backgroundAlpha = sBackgroundAlpha;
            }
        } else if (!hasGui && blurring) {
            gr.shutdownEffect();
            fadingIn = false;
            blurring = false;
        }
        screenOpened = hasGui;
    }

    /**
     * Internal method, to re-blur after resources (including shaders) reloaded in in-game menu
     */
    public void forceBlur() {
        // no need to check if is excluded, this method is only called by opened ModernUI Screen
        if (!sBlurEffect) {
            return;
        }
        if (minecraft.level != null) {
            GameRenderer gr = minecraft.gameRenderer;
            if (gr.currentEffect() == null) {
                ((AccessGameRenderer) gr).callLoadEffect(bilinearBlur);
                fadingIn = true;
                blurring = true;
            }
        }
    }

    public void loadBlacklist(@Nonnull List<? extends String> names) {
        blacklist.clear();
        for (String s : names) {
            try {
                Class<?> clazz = Class.forName(s);
                blacklist.add(clazz);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Render tick, should called before rendering things
     */
    public void update() {
        if (fadingIn) {
            float p = Math.min(UIManager.getInstance().getDrawingTime() / sAnimationDuration, 1.0f);
            if (blurring) {
                updateRadius(p * sBlurRadius);
            }
            if (backgroundAlpha < sBackgroundAlpha) {
                backgroundAlpha = p * sBackgroundAlpha;
            }
            if (p == 1.0f) {
                fadingIn = false;
            }
        }
    }

    private void updateRadius(float radius) {
        PostChain effect = minecraft.gameRenderer.currentEffect();
        if (effect == null)
            return;
        List<PostPass> passes = ((AccessPostChain) effect).getPasses();
        for (PostPass s : passes) {
            s.getEffect().safeGetUniform("Progress").set(radius);
        }
    }

    public void drawScreenBackground(@Nonnull Screen screen, @Nonnull PoseStack stack, int x1, int y1, int x2, int y2) {
        int a = (int) (backgroundAlpha * 0xff);
        if (a == 0)
            return;
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.defaultBlendFunc();

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = stack.last().pose();
        int z = screen.getBlitOffset();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(matrix, x2, y1, z).color(0, 0, 0, a).endVertex();
        builder.vertex(matrix, x1, y1, z).color(0, 0, 0, a).endVertex();
        builder.vertex(matrix, x1, y2, z).color(0, 0, 0, a).endVertex();
        builder.vertex(matrix, x2, y2, z).color(0, 0, 0, a).endVertex();
        builder.end();
        BufferUploader.end(builder);

        RenderSystem.disableBlend();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableTexture();
    }
}
