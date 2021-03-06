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

package icyllis.modernui.impl.stats;

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.view.UITools;
import icyllis.modernui.graphics.math.TextAlign;
import icyllis.modernui.graphics.math.Color3i;
import icyllis.modernui.test.discard.ScrollWindow;
import icyllis.modernui.test.widget.UniformScrollEntry;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.stats.Stats;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ItemStatsEntry extends UniformScrollEntry {

    private final Item item;

    private final Color3i color;

    private final String itemName;

    private final List<String> vars = new ArrayList<>();

    private boolean drawTooltip = false;

    public ItemStatsEntry(ScrollWindow<?> window, @Nonnull Item item, Color3i color) {
        super(window, 288, ItemStatsGroup.ENTRY_HEIGHT);
        this.item = item;
        this.itemName = item.getName().getString();
        this.color = color;
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        //itemRenderer.renderItemIntoGUI(item.getDefaultInstance(), (int) x1 + 2, (int) y1 + 2);
        canvas.setColor(color, 1);
        canvas.setTextAlign(TextAlign.RIGHT);
        int i = 0;
        for (String var : vars) {
            canvas.drawText(var, x1 + 80 + i * 50, y1 + 6);
            i++;
        }
        if (drawTooltip) {
            //canvas.setColor(0.5f, 0.5f, 0.5f, 0.25f);
            canvas.drawRect(x1 + 1, y1 + 1, x1 + 19, y2 - 1);
            float l = UITools.getTextWidth(itemName);
            //canvas.setColor(0, 0, 0, 0.5f);
            canvas.drawRect(x1 + 22, y1 + 3, x1 + 28 + l, y2 - 3);
            canvas.setTextAlign(TextAlign.LEFT);
            canvas.resetColor();
            canvas.drawText(itemName, x1 + 25, y1 + 6);
        }
        canvas.drawItem(item, x1 + 2, y1 + 2);
    }

    public void updateValue(StatisticsManager manager) {
        vars.clear();
        if (item instanceof BlockItem) {
            BlockItem blockItem = (BlockItem) item;
            Stat<?> stat = Stats.BLOCK_MINED.get(blockItem.getBlock());
            vars.add(stat.format(manager.getValue(stat)));
            stat = Stats.ITEM_CRAFTED.get(blockItem);
            vars.add(stat.format(manager.getValue(stat)));
            stat = Stats.ITEM_USED.get(blockItem);
            vars.add(stat.format(manager.getValue(stat)));
            stat = Stats.ITEM_PICKED_UP.get(blockItem);
            vars.add(stat.format(manager.getValue(stat)));
            stat = Stats.ITEM_DROPPED.get(blockItem);
            vars.add(stat.format(manager.getValue(stat)));
        } else {
            Stat<?> stat = Stats.ITEM_BROKEN.get(item);
            vars.add(stat.format(manager.getValue(stat)));
            stat = Stats.ITEM_CRAFTED.get(item);
            vars.add(stat.format(manager.getValue(stat)));
            stat = Stats.ITEM_USED.get(item);
            vars.add(stat.format(manager.getValue(stat)));
            stat = Stats.ITEM_PICKED_UP.get(item);
            vars.add(stat.format(manager.getValue(stat)));
            stat = Stats.ITEM_DROPPED.get(item);
            vars.add(stat.format(manager.getValue(stat)));
        }
    }

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (super.updateMouseHover(mouseX, mouseY)) {
            drawTooltip = mouseX >= x1 + 1 && mouseX <= x1 + 19 && mouseY >= y1 + 1 && mouseY <= y2 - 1;
            return true;
        }
        return false;
    }

    @Override
    public void onMouseHoverExit() {
        drawTooltip = false;
    }
}