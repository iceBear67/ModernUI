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

/**
 * A single animation depend on current value
 */
@Deprecated
public class PartialAnimation {
    /*@Override
    public void update(float time) {

    }*/

    /*private final float duration;

    private final boolean useSine;

    private float startTime;

    private Applier applier;

    private boolean finish = false;

    public PartialAnimation(float duration) {
        this(duration, false);
    }

    public PartialAnimation(float duration, boolean useSine) {
        this.duration = duration;
        this.useSine = useSine;
        startTime = GlobalModuleManager.INSTANCE.getAnimationTime();
    }

    public PartialAnimation applyTo(Applier applier, float currentValue) {
        this.applier = applier;
        float p = 1 - (applier.endValue - currentValue) / (applier.endValue - applier.startValue);
        if (useSine) {
            p = (float) (Math.asin(p) * 2 / Math.PI);
        }
        startTime = startTime - duration * p;
        return this;
    }

    @Override
    public void update(float time) {
        float p = Math.min((time - startTime) / duration, 1);
        if (useSine) {
            p = (float) Math.sin(p * Math.PI / 2);
        }
        applier.update(p);
        if (p == 1) {
            finish = true;
        }
    }

    @Override
    public boolean shouldRemove() {
        return finish;
    }*/
}
