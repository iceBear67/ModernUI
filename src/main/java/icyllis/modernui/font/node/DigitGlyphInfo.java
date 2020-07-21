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

package icyllis.modernui.font.node;

import icyllis.modernui.font.glyph.TexturedGlyph;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

/**
 * The key to fast render digit
 */
public class DigitGlyphInfo implements IGlyphRenderInfo {

    /**
     * A reference of cached array in GlyphManager, 0-9 textured glyphs (in that order)
     */
    private final TexturedGlyph[] glyphs;

    /**
     * An array of digit char index of the whole original string.
     * The index should skipped all supplementary multilingual plane and formatting codes.
     * This array length equals to this info total digit count to renderer.
     * This array value equals to the char (not code point) index of the original string.
     *
     * @see #glyphs
     */
    private final int stringIndex;

    public DigitGlyphInfo(TexturedGlyph[] glyphs, int stringIndex) {
        this.glyphs = glyphs;
        this.stringIndex = stringIndex;
    }

    /*@Override
    public float drawString(@Nonnull BufferBuilder builder, @Nonnull String raw, int color, float x, float y, int r, int g, int b, int a) {
        if (this.color != -1) {
            r = this.color >> 16 & 0xff;
            g = this.color >> 8 & 0xff;
            b = this.color & 0xff;
        }
        for (int i : indexArray) {
            builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
            x = glyphs[raw.charAt(i) - '0'].drawGlyph(builder, x, y, r, g, b, a);
            builder.finishDrawing();
            WorldVertexBufferUploader.draw(builder);
        }
        return x;
    }*/

    @Override
    public float drawString(@Nonnull BufferBuilder builder, @Nonnull String raw, float x, float y, int r, int g, int b, int a) {
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
        x = glyphs[raw.charAt(stringIndex) - '0'].drawGlyph(builder, x, y, r, g, b, a);
        builder.finishDrawing();
        WorldVertexBufferUploader.draw(builder);
        return x;
    }

    @Override
    public float drawString(Matrix4f matrix, @Nonnull IRenderTypeBuffer buffer, @Nonnull String raw, float x, float y, int r, int g, int b, int a, int packedLight) {
        return glyphs[raw.charAt(stringIndex) - '0'].drawGlyph(matrix, buffer, x, y, r, g, b, a, packedLight);
    }
}