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

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import icyllis.modernui.ModernUI;
import icyllis.modernui.graphics.font.pipeline.TextRenderNode;
import icyllis.modernui.graphics.text.TextLayoutProcessor;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.math.Color3i;
import icyllis.modernui.graphics.math.Icon;
import icyllis.modernui.graphics.math.TextAlign;
import icyllis.modernui.graphics.shader.program.*;
import icyllis.modernui.view.UIManager;
import icyllis.modernui.view.View;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

/**
 * The main renderer of Modern UI, draw things for View:
 * likes rect, rounded rect, circle, ring, text, line, point, image etc.
 * <p>
 * The canvas actually uses shaders (hardware-accelerated)
 * to render in real-time, so there's no need to control redrawing.
 * Also avoided RenderType being used in GUI, for better performance
 * (reduces GL callings, because render states changed little)
 * <p>
 * The font renderer uses another system, which has two parts, one for Modern UI, and
 * the global one is using RenderType, make Modern UI font renderer work everywhere,
 * because it's not always called in GUI, likes screens of other mods, TileEntityRenderer
 * or in world rendering, that also need matrix transformation to be compatible with vanilla
 *
 * @author BloCamLimb
 */
@SuppressWarnings("unused")
//TODO New render system (LOWEST PRIORITY)
public class Canvas {

    private static Canvas instance;

    /**
     * Instances
     */
    private final Window mainWindow;
    private final ItemRenderer itemRenderer;

    private final TextLayoutProcessor fontEngine = TextLayoutProcessor.getInstance();

    private final BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();


    /**
     * Shaders instance
     */
    private final RingShader ring = RingShader.INSTANCE;
    private final RoundedRectShader roundedRect = RoundedRectShader.INSTANCE;
    private final RoundedFrameShader roundedFrame = RoundedFrameShader.INSTANCE;
    private final CircleShader circle = CircleShader.INSTANCE;
    private final FeatheredRectShader featheredRect = FeatheredRectShader.INSTANCE;


    /**
     * Paint colors
     */
    private int r = 255;
    private int g = 255;
    private int b = 255;
    private int a = 255;


    /**
     * Depth
     */
    private double z = 0.0D;

    /*
     * Drawing location offset, view or drawable
     */
    /*private int drawingX = 0;
    private int drawingY = 0;*/

    /**
     * Elapsed time from a gui open
     */
    private long drawingTime = 0;


    /**
     * Text align
     */
    private float alignFactor = TextAlign.LEFT.offsetFactor;


    /**
     * GL states
     */
    private static boolean lineAA = false;


    private Canvas(@Nonnull Minecraft minecraft) {
        RenderCore.startRenderEngine();
        mainWindow = minecraft.getWindow();
        itemRenderer = minecraft.getItemRenderer();
        fontEngine.initRenderer();
    }

    /**
     * This will start the render engine of Modern UI. Always do not call this
     * at the wrong time.
     *
     * @return the instance
     * @see UIManager#initialize()
     */
    public static Canvas getInstance() {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        if (instance == null) {
            instance = new Canvas(Minecraft.getInstance());
            ModernUI.LOGGER.debug(RenderCore.MARKER, "Render engine started");
        }
        return instance;
    }

    /**
     * Set current paint color with alpha
     *
     * @param r red [0,255]
     * @param g green [0,255]
     * @param b blue [0,255]
     * @param a alpha [0,255]
     */
    public void setColor(int r, int g, int b, int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    /**
     * Set current paint color, keep previous alpha
     *
     * @param r red [0,1]
     * @param g green [0,1]
     * @param b blue [0,1]
     */
    public void setColor(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /**
     * Set current paint color with alpha
     *
     * @param argb like 0x80404040 (=R64,G64,B64,A128)
     */
    public void setARGB(int argb) {
        a = argb >> 24 & 0xff;
        r = argb >> 16 & 0xff;
        g = argb >> 8 & 0xff;
        b = argb & 0xff;
    }

    /**
     * Set current paint color, keep previous alpha
     *
     * @param rgb like 0x404040 (=R64,G64,B64)
     */
    public void setRGB(int rgb) {
        r = rgb >> 16 & 0xff;
        g = rgb >> 8 & 0xff;
        b = rgb & 0xff;
    }

    /**
     * Set current paint alpha in float form
     *
     * @param a alpha [0,1]
     */
    public void setAlpha(float a) {
        this.a = (int) (a * 255.0f);
    }

    /**
     * Set current paint alpha in integer form
     *
     * @param a alpha [0,255]
     */
    public void setAlpha(int a) {
        this.a = a;
    }

    @Deprecated
    public void setColor(@Nonnull Color3i color) {
        r = color.getRed();
        g = color.getGreen();
        b = color.getBlue();
    }

    @Deprecated
    public void setColor(@Nonnull Color3i color, int a) {
        r = color.getRed();
        g = color.getGreen();
        b = color.getBlue();
        this.a = a;
    }

    /**
     * Reset color to white color and completely opaque.
     */
    public void resetColor() {
        r = 255;
        g = 255;
        b = 255;
        a = 255;
    }

    /**
     * Get elapsed time in UI window, update every frame
     *
     * @return drawing time in milliseconds
     */
    public long getDrawingTime() {
        return drawingTime;
    }

    // inner use
    public void setDrawingTime(long drawingTime) {
        this.drawingTime = drawingTime;
    }

    /**
     * Enable or disable anti aliasing for lines
     *
     * @param aa anti-aliasing
     */
    public void setLineAntiAliasing(boolean aa) {
        if (aa) {
            if (!lineAA) {
                GL11.glEnable(GL11.GL_LINE_SMOOTH);
                GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
                lineAA = true;
            }
        } else if (lineAA) {
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            lineAA = false;
        }
    }

    /**
     * Set line width for lines drawing
     *
     * @param width width, default is 1.0f (not affected by gui scale)
     */
    public void setLineWidth(float width) {
        RenderSystem.lineWidth(width);
    }

    /**
     * Set z pos / level, determines the depth, higher value will draw at the top
     * Minimum value and default value are 0
     *
     * @param z target z
     */
    @Deprecated
    public void setZ(double z) {
        this.z = z;
    }

    /**
     * Set current text alignment for next drawing
     *
     * @param align the align to set
     * @see #drawText(String, float, float)
     */
    public void setTextAlign(@Nonnull TextAlign align) {
        alignFactor = align.offsetFactor;
    }

    /**
     * Layout and draw a single line of text on screen, {@link ChatFormatting}
     * and bidirectional algorithm are supported, returns the text width.
     * <p>
     * It's recommended to use this when you draw a fast changing number,
     * all digits are laid-out with the same width as '0', because we don't know
     * whether it is static layout or dynamic layout, so we don't want to
     * re-layout when the numbers are changing too fast as it's performance hungry.
     * <p>
     * This method is convenient to use at any time but inflexible, such as,
     * you can't do cross line text layout, auto translatable text, formatting with
     * multiple arguments, hyperlinks, hover tooltips, or add various styles (such as
     * custom color, font size, etc) to different parts of the text. Because of
     * localization, the length of each part of the text is uncertain.
     * <p>
     * To achieve these things, use a {@link icyllis.modernui.widget.TextView}, we
     * also support markdown syntax using commonmark specification.
     *
     * @param text the text to draw
     * @param x    the x-coordinate of origin for where to draw the text
     * @param y    the y-coordinate of origin for where to draw the text
     * @return the total advance of the text (text line width)
     * @see #setTextAlign(TextAlign)
     */
    public float drawText(String text, float x, float y) {
        if (text == null || text.isEmpty())
            return 0;
        final TextRenderNode node = fontEngine.lookupVanillaNode(text, Style.EMPTY);
        if (alignFactor > 0)
            x -= node.advance * alignFactor;
        return node.drawText(bufferBuilder, text, x, y, r, g, b, a);
    }

    /**
     * Draw a rectangle on screen with given rect area
     *
     * @param left   rect left
     * @param top    rect top
     * @param right  rect right
     * @param bottom rect bottom
     */
    public void drawRect(float left, float top, float right, float bottom) {
        RenderSystem.disableTexture();

        /*left += drawingX;
        top += drawingY;
        right += drawingX;
        bottom += drawingY;*/

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(left, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(right, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(right, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(left, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.end();
        BufferUploader.end(bufferBuilder);
    }

    /**
     * Draw four rectangles outside the given rect with thickness
     *
     * @param left      rect left
     * @param top       rect top
     * @param right     rect right
     * @param bottom    rect bottom
     * @param thickness thickness, must be integral multiple of 1.0
     */
    public void drawRectOutline(float left, float top, float right, float bottom, float thickness) {
        RenderSystem.disableTexture();

        /*left += drawingX;
        top += drawingY;
        right += drawingX;
        bottom += drawingY;*/

        /*ShaderTools.useShader(featheredRect);
        featheredRect.setThickness(0.25f);

        featheredRect.setInnerRect(left - thickness + 0.25f, top - thickness + 0.25f, right - 0.25f, top - 0.25f);*/

        final int r = this.r;
        final int g = this.g;
        final int b = this.b;
        final int a = this.a;
        final double z = this.z;

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(left - thickness, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(right, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(right, top - thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(left - thickness, top - thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.end();
        BufferUploader.end(bufferBuilder);

        //featheredRect.setInnerRect(right + 0.25f, top - thickness + 0.25f, right + thickness - 0.25f, bottom - 0.25f);

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(right, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(right + thickness, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(right + thickness, top - thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(right, top - thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.end();
        BufferUploader.end(bufferBuilder);

        //featheredRect.setInnerRect(left + 0.25f, bottom + 0.25f, right + thickness - 0.25f, bottom + thickness - 0.25f);

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(left, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(right + thickness, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(right + thickness, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(left, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.end();
        BufferUploader.end(bufferBuilder);

        //featheredRect.setInnerRect(left - thickness + 0.25f, top + 0.25f, left - 0.25f, bottom + thickness - 0.25f);

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(left - thickness, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(left, bottom + thickness, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(left, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(left - thickness, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.end();
        BufferUploader.end(bufferBuilder);

        //ShaderTools.releaseShader();
    }

    /**
     * Draw a rect frame with bevel angle
     *
     * @param left   rect left
     * @param top    rect top
     * @param right  rect right
     * @param bottom rect bottom
     * @param bevel  bevel length
     */
    public void drawOctagonRectFrame(float left, float top, float right, float bottom, float bevel) {
        RenderSystem.disableTexture();

        /*left += drawingX;
        top += drawingY;
        right += drawingX;
        bottom += drawingY;*/

        final int r = this.r;
        final int g = this.g;
        final int b = this.b;
        final int a = this.a;
        final double z = this.z;

        bufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(left, bottom - bevel, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(left + bevel, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(right - bevel, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(right, bottom - bevel, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(right, top + bevel, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(right - bevel, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(left + bevel, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(left, top + bevel, z).color(r, g, b, a).endVertex();
        bufferBuilder.end();
        BufferUploader.end(bufferBuilder);
    }

    /**
     * Draw four lines around a closed rect area, anti-aliasing is needed
     * Otherwise, there's a missing pixel
     *
     * @param left   rect left
     * @param top    rect top
     * @param right  rect right
     * @param bottom rect bottom
     */
    public void drawRectLines(float left, float top, float right, float bottom) {
        RenderSystem.disableTexture();

        /*left += drawingX;
        top += drawingY;
        right += drawingX;
        bottom += drawingY;*/

        bufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(left, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(right, bottom, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(right, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(left, top, z).color(r, g, b, a).endVertex();
        bufferBuilder.end();
        BufferUploader.end(bufferBuilder);
    }

    /**
     * Draw ring / annulus on screen with given center pos and radius
     * <p>
     * Default feather radius: 1 px
     *
     * @param centerX     center x pos
     * @param centerY     center y pos
     * @param innerRadius inner circle radius
     * @param outerRadius outer circle radius
     */
    public void drawRing(float centerX, float centerY, float innerRadius, float outerRadius) {
        RenderCore.useShader(ring);
        ring.setRadius(innerRadius, outerRadius);
        ring.setCenter(centerX, centerY);
        drawRect(centerX - outerRadius, centerY - outerRadius, centerX + outerRadius, centerY + outerRadius);
        RenderCore.releaseShader();
    }

    /**
     * Draw circle on screen with given center pos and radius
     * <p>
     * Default feather radius: 1 px
     *
     * @param centerX center x pos
     * @param centerY center y pos
     * @param radius  circle radius
     */
    public void drawCircle(float centerX, float centerY, float radius) {
        RenderCore.useShader(circle);
        circle.setRadius(radius);
        circle.setCenter(centerX, centerY);
        drawRect(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        RenderCore.releaseShader();
    }

    /**
     * Draw a line with given two pos
     *
     * @param startX x1
     * @param startY y1
     * @param stopX  x2
     * @param stopY  y2
     */
    public void drawLine(float startX, float startY, float stopX, float stopY) {
        RenderSystem.disableTexture();

        /*startX += drawingX;
        stopX += drawingX;
        startY += drawingY;
        stopY += drawingY;*/

        bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(startX, startY, z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(stopX, stopY, z).color(r, g, b, a).endVertex();
        bufferBuilder.end();
        BufferUploader.end(bufferBuilder);
    }

    /**
     * Draw rounded rectangle on screen with given rect area and rounded radius
     *
     * @param left   the left of the rectangle
     * @param top    the top of the rectangle
     * @param right  the right of the rectangle
     * @param bottom the bottom of the rectangle
     * @param radius the rounded corner radius
     */
    public void drawRoundedRect(float left, float top, float right, float bottom, float radius) {
        RenderCore.useShader(roundedRect);
        roundedRect.setRadius(radius);
        roundedRect.setInnerRect(left + radius, top + radius, right - radius, bottom - radius);
        drawRect(left, top, right, bottom);
        RenderCore.releaseShader();
    }

    /**
     * Draw rounded rectangle frame in a rounded rect on screen
     * with given rect area and rounded radius
     * <p>
     * Default feather radius: 1 px
     * Default frame thickness: 1.5 px
     *
     * @param left   the left of the rectangle
     * @param top    the top of the rectangle
     * @param right  the right of the rectangle
     * @param bottom the bottom of the rectangle
     * @param radius the rounded corner radius
     */
    public void drawRoundedFrame(float left, float top, float right, float bottom, float radius) {
        RenderCore.useShader(roundedFrame);
        roundedFrame.setRadius(radius);
        roundedFrame.setInnerRect(left + radius, top + radius, right - radius, bottom - radius);
        drawRect(left, top, right, bottom);
        RenderCore.releaseShader();
    }

    // Alpha test
    public void drawRoundedFrameT1(float left, float top, float right, float bottom, float radius) {
        RenderCore.useShader(roundedFrame);
        roundedFrame.setRadius(radius);
        roundedFrame.setInnerRect(left + radius, top + radius, right - radius, bottom - radius);
        RenderSystem.disableTexture();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(left, bottom, z).color(170, 220, 240, a).endVertex();
        bufferBuilder.vertex(right, bottom, z).color(201, 200, 232, a).endVertex();
        bufferBuilder.vertex(right, top, z).color(232, 180, 223, a).endVertex();
        bufferBuilder.vertex(left, top, z).color(201, 200, 232, a).endVertex();
        bufferBuilder.end();
        BufferUploader.end(bufferBuilder);
        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderCore.releaseShader();
    }

    /**
     * Draw feathered rectangle frame in a rounded rect on screen
     * with given rect area and feather thickness (not radius)
     * A replacement for rounded rect when radius is too small.
     *
     * @param left      rect left
     * @param top       rect top
     * @param right     rect right
     * @param bottom    rect bottom
     * @param thickness feather thickness (&lt;= 0.5 is better)
     */
    public void drawFeatheredRect(float left, float top, float right, float bottom, float thickness) {
        RenderCore.useShader(featheredRect);
        featheredRect.setThickness(thickness);
        featheredRect.setInnerRect(left + thickness, top + thickness, right - thickness, bottom - thickness);
        drawRect(left, top, right, bottom);
        RenderCore.releaseShader();
    }

    /**
     * Draw icon on screen fitting to given rect area
     *
     * @param icon   icon
     * @param left   rect left
     * @param top    rect top
     * @param right  rect right
     * @param bottom rect bottom
     */
    public void drawIcon(@Nonnull Icon icon, float left, float top, float right, float bottom) {
        RenderSystem.enableTexture();
        icon.bindTexture();
        BufferBuilder bufferBuilder = this.bufferBuilder;

        /*left += drawingX;
        top += drawingY;
        right += drawingX;
        bottom += drawingY;*/

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        bufferBuilder.vertex(left, bottom, z).color(r, g, b, a).uv(icon.getLeft(), icon.getBottom()).endVertex();
        bufferBuilder.vertex(right, bottom, z).color(r, g, b, a).uv(icon.getRight(), icon.getBottom()).endVertex();
        bufferBuilder.vertex(right, top, z).color(r, g, b, a).uv(icon.getRight(), icon.getTop()).endVertex();
        bufferBuilder.vertex(left, top, z).color(r, g, b, a).uv(icon.getLeft(), icon.getTop()).endVertex();
        bufferBuilder.end();
        BufferUploader.end(bufferBuilder);
    }

    /**
     * Draw item default instance, without any NBT data
     * Size on screen: 16 * 16 * GuiScale
     *
     * @param item item
     * @param x    x pos
     * @param y    y pos
     */
    public void drawItem(@Nonnull Item item, float x, float y) {
        itemRenderer.renderGuiItem(item.getDefaultInstance(), (int) (x), (int) (y));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /**
     * Draw item stack with NBT
     *
     * @param stack item stack to draw
     * @param x     x pos
     * @param y     y pos
     */
    public void drawItemStack(@Nonnull ItemStack stack, float x, float y) {
        itemRenderer.renderGuiItem(stack, (int) (x), (int) (y));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /**
     * Draw item stack with NBT and their damage bar, amount etc
     *
     * @param stack item stack to draw
     * @param x     x pos
     * @param y     y pos
     */
    public void drawItemStackWithOverlays(@Nonnull ItemStack stack, float x, float y) {
        itemRenderer.renderGuiItem(stack, (int) (x), (int) (y));
        itemRenderer.renderGuiItemDecorations(Minecraft.getInstance().font, stack, (int) (x), (int) (y));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /**
     * At most cases, you've to call this
     * in view's onDraw() method
     *
     * @param view view to move
     */
    @Deprecated
    public void moveTo(@Nonnull View view) {
        /*drawingX = view.getLeft();
        drawingY = view.getTop();*/
    }

    /**
     * At most cases, you've to call this
     * in drawable's draw() method
     *
     * @param drawable drawable to move
     */
    @Deprecated
    public void moveTo(@Nonnull Drawable drawable) {
        /*drawingX = drawable.getLeft();
        drawingY = drawable.getTop();*/
    }

    @Deprecated
    public void moveToZero() {
        /*drawingX = 0;
        drawingY = 0;*/
    }

    public void save() {
        RenderSystem.pushMatrix();
    }

    public void restore() {
        RenderSystem.popMatrix();
    }

    public void translate(float dx, float dy) {
        RenderSystem.translatef(dx, dy, 0.0f);
    }

    public void scale(float sx, float sy) {
        RenderSystem.scalef(sx, sy, 1.0f);
    }

    /**
     * Scale the canvas and translate to pos
     *
     * @param sx x scale
     * @param sy y scale
     * @param px pivot x pos
     * @param py pivot y pos
     */
    public void scale(float sx, float sy, float px, float py) {
        RenderSystem.scalef(sx, sy, 1.0f);
        float dx;
        float dy;
        if (sx < 1) {
            dx = 1.0f / sx - 1.0f;
        } else {
            dx = sx - 1.0f;
        }
        dx *= px;
        if (sy < 1) {
            dy = 1.0f / sy - 1.0f;
        } else {
            dy = sy - 1.0f;
        }
        dy *= py;
        RenderSystem.translatef(dx, dy, 0.0f);
    }

    public void clipVertical(@Nonnull View view) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(0, mainWindow.getHeight() - view.getBottom(),
                mainWindow.getWidth(), view.getHeight());
    }

    public void clipStart(float x, float y, float width, float height) {
        double scale = mainWindow.getGuiScale();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x * scale), (int) (mainWindow.getHeight() - ((y + height) * scale)),
                (int) (width * scale), (int) (height * scale));
    }

    public void clipEnd() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
