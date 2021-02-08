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

package icyllis.modernui.text;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class MeasuredText {

    public static class Builder {

        private final List<Run> mRuns = new ArrayList<>();

        @Nonnull
        private final char[] mText;
        private int mCurrentOffset = 0;

        public Builder(@Nonnull char[] text) {
            mText = text;
        }

        public Builder appendStyleRun(@Nonnull TextPaint paint, int length, boolean isRtl) {
            Preconditions.checkArgument(length > 0, "length can not be negative");
            final int end = mCurrentOffset + length;
            Preconditions.checkArgument(end <= mText.length, "Style exceeds the text length");
            mRuns.add(new StyleRun(mCurrentOffset, end, paint, isRtl));
            mCurrentOffset = end;
            return this;
        }
    }

    // logical run, sub-run of bidi run
    public static class Run {

        protected int mStart;
        protected int mEnd;

        public Run(int start, int end) {
            mStart = start;
            mEnd = end;
        }
    }

    public static class StyleRun extends Run {

        public final TextPaint mPaint;
        public final boolean mIsRtl;

        public StyleRun(int start, int end, TextPaint paint, boolean isRtl) {
            super(start, end);
            mPaint = paint;
            mIsRtl = isRtl;
        }
    }
}