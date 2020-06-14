/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.master;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.ui.animation.Animation;
import icyllis.modernui.ui.layout.MeasureSpec;
import icyllis.modernui.ui.test.IModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Modern UI's UI system service, almost everything is here
 */
@OnlyIn(Dist.CLIENT)
public enum UIManager implements IViewParent {
    /**
     * Render thread instance only
     */
    INSTANCE;

    // logger marker
    public static final Marker MARKER = MarkerManager.getMarker("UI");

    // cached minecraft instance
    private final Minecraft minecraft = Minecraft.getInstance();

    // cached gui to open
    @Nullable
    private Screen guiToOpen;

    // current screen instance, must be ModernScreen or ModernContainerScreen or null
    @Nullable
    private Screen modernScreen;

    // main fragment of a UI
    private Fragment fragment;

    // main view that created from main fragment
    private View view;

    @Deprecated
    @Nullable
    private IModule popup;

    // scaled game window width / height
    private int width, height;

    // scaled mouseX, mouseY on screen
    private double mouseX, mouseY;

    // a list of animations in render loop
    private final List<Animation> animations = new ArrayList<>();

    // a list of UI tasks
    private final List<DelayedTask> tasks = new CopyOnWriteArrayList<>();

    // elapsed ticks from a gui open, update every tick, 20 = 1 second
    private int ticks = 0;

    // elapsed time from a gui open, update every frame, 20.0 = 1 second
    private float time = 0;

    // lazy loading, should be final
    private Canvas canvas = null;

    // only post events to focused views
    @Nullable
    private View mHovered = null;
    @Nullable
    private View mDragging = null;
    @Nullable
    private View mKeyboard = null;

    // for double click check, 10 tick = 0.5s
    private int doubleClickTime = -10;

    // to schedule layout on next frame
    private boolean layoutRequested = false;

    // to fix layout freq at 60Hz at most
    private float lastLayoutTime = 0;

    UIManager() {

    }

    /**
     * Open a gui screen
     *
     * @param mainFragment main fragment of the UI
     */
    public void openGuiScreen(@Nonnull Fragment mainFragment) {
        this.fragment = mainFragment;
        minecraft.displayGuiScreen(new ModernScreen());
    }

    /**
     * Close current gui screen
     */
    public void closeGuiScreen() {
        minecraft.displayGuiScreen(null);
    }

    /**
     * Register a container screen. To open the gui screen,
     * see {@link net.minecraftforge.fml.network.NetworkHooks#openGui(ServerPlayerEntity, INamedContainerProvider, Consumer)}
     *
     * @param type    container type
     * @param factory main fragment factory
     * @param <T>     container
     */
    public <T extends Container> void registerContainerScreen(@Nonnull ContainerType<? extends T> type,
                                                              @Nonnull Function<T, Fragment> factory) {
        ScreenManager.registerFactory(type, castModernScreen(factory));
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Nonnull
    private <T extends Container, U extends Screen & IHasContainer<T>> ScreenManager.IScreenFactory<T, U> castModernScreen(
            @Nonnull Function<T, Fragment> factory) {
        return (c, p, t) -> {
            // The client container can be null sometimes, but a container screen doesn't allow the container to be null
            // so return null, there's no gui will be open, and the server container will be closed automatically
            if (c == null) {
                return null;
            }
            this.fragment = factory.apply(c);
            return (U) new ModernContainerScreen<>(c, p, t);
        };
    }

    /**
     * Open a popup module, a special module
     *
     * @param popup   popup module
     * @param refresh true will post mouseMoved(-1, -1) to root module
     *                confirm window should reset mouse
     *                context menu should not reset mouse
     */
    //TODO new popup system
    public void openPopup(IModule popup, boolean refresh) {
        /*if (root == null) {
            ModernUI.LOGGER.fatal(MARKER, "#openPopup() shouldn't be called when there's NO gui open");
            return;
        }*/
        if (this.popup != null) {
            ModernUI.LOGGER.warn(MARKER, "#openPopup() shouldn't be called when there's already a popup, the previous one has been overwritten");
        }
        if (refresh) {
            this.mouseMoved(-1, -1);
        }
        this.popup = popup;
        this.popup.resize(width, height);
        refreshMouse();
    }

    /**
     * Close current popup
     */
    public void closePopup() {
        if (popup != null) {
            popup = null;
            refreshMouse();
        }
    }

    /**
     * Called when open a gui screen, or back to the gui screen
     *
     * @param mui    modern screen or modern container screen
     * @param width  screen width (= game main window width)
     * @param height screen height (= game main window height)
     */
    void init(@Nonnull Screen mui, int width, int height) {
        this.modernScreen = mui;
        this.width = width;
        this.height = height;

        // init view of this UI
        if (view == null) {
            if (fragment == null) {
                ModernUI.LOGGER.fatal(MARKER, "Fragment can't be null when opening a gui screen");
                closeGuiScreen();
                return;
            }
            view = fragment.createView();
            if (view == null) {
                ModernUI.LOGGER.warn(MARKER, "The main view created from the fragment shouldn't be null");
                view = new View();
            }
            view.assignParent(this);
        }

        // create canvas
        if (canvas == null) {
            canvas = new Canvas();
        }

        layout();
    }

    /**
     * Inner method, do not call
     *
     * @return {@code true} to cancel the event
     */
    public boolean handleGuiOpenEvent(@Nullable Screen guiToOpen) {
        this.guiToOpen = guiToOpen;
        if (guiToOpen == null) {
            destroy();
            return false;
        }
        // (modern screen != null)
        if (modernScreen != guiToOpen && ((guiToOpen instanceof ModernScreen) || (guiToOpen instanceof ModernContainerScreen<?>))) {
            if (view != null) {
                ModernUI.LOGGER.fatal(MARKER, "Modern UI doesn't allow to keep other screens. ModernScreen: {}, GuiToOpen: {}", modernScreen, guiToOpen);
                return true;
            }
            resetTicks();
        }
        // hotfix 1.5.2, but there's no way to work with screens that will pause game
        if (modernScreen != guiToOpen && modernScreen != null) {
            mouseMoved(-1, -1);
        }
        // for non-modern-ui screens
        if (modernScreen == null) {
            resetTicks();
        }
        return false;
    }

    private void resetTicks() {
        ticks = 0;
        time = 0;
    }

    /**
     * Get current open screen differently from Minecraft's,
     * which will only return Modern UI's screen or null
     *
     * @return modern screen
     */
    @Nullable
    public Screen getModernScreen() {
        return modernScreen;
    }

    /**
     * Add an active animation, which will be removed from list if finished
     *
     * @param animation animation to add
     */
    public void addAnimation(@Nonnull Animation animation) {
        if (!animations.contains(animation)) {
            animations.add(animation);
        }
    }

    /**
     * Post a task that will run on next pre-tick
     *
     * @param runnable     runnable
     * @param delayedTicks delayed ticks to run the task
     */
    public void postTask(@Nonnull Runnable runnable, int delayedTicks) {
        tasks.add(new DelayedTask(runnable, delayedTicks));
    }

    void mouseMoved(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        if (popup != null) {
            popup.mouseMoved(mouseX, mouseY);
            return;
        }
        if (view != null) {
            if (!view.updateMouseHover(mouseX, mouseY)) {
                setHoveredView(null);
            }
        }
    }

    boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (popup != null) {
            return popup.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (mHovered != null) {
            if (mouseButton == 0) {
                int delta = ticks - doubleClickTime;
                if (delta < 10) {
                    doubleClickTime = -10;
                    if (false) {//vHovered.onMouseDoubleClicked(vHovered.getRelativeMX(), vHovered.getRelativeMY())) {
                        return true;
                    }
                } else {
                    doubleClickTime = ticks;
                }
                return false;//vHovered.onMouseLeftClicked(vHovered.getRelativeMX(), vHovered.getRelativeMY());
            } else if (mouseButton == 1) {
                return false;//vHovered.onMouseRightClicked(vHovered.getRelativeMX(), vHovered.getRelativeMY());
            }
        }
        return false;
    }

    boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (popup != null) {
            return popup.mouseReleased(mouseX, mouseY, mouseButton);
        }
        if (mDragging != null) {
            setDragging(null);
            return true;
        }
        return false;//root.mouseReleased(mouseX, mouseY, mouseButton);
    }

    boolean mouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (popup != null) {
            return popup.mouseDragged(mouseX, mouseY, deltaX, deltaY);
        }
        if (mDragging != null) {
            return false;//vDragging.onMouseDragged(vDragging.getRelativeMX(), vDragging.getRelativeMY(), deltaX, deltaY);
        }
        return false;
    }

    boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (popup != null) {
            return popup.mouseScrolled(mouseX, mouseY, amount);
        }
        if (mHovered != null) {
            return false;//vHovered.onMouseScrolled(vHovered.getRelativeMX(), vHovered.getRelativeMY(), amount);
        }
        return false;
    }

    boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (popup != null) {
            return popup.keyPressed(keyCode, scanCode, modifiers);
        } else {
            return false;//root.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (popup != null) {
            return popup.keyReleased(keyCode, scanCode, modifiers);
        } else {
            return false;//root.keyReleased(keyCode, scanCode, modifiers);
        }
    }

    boolean charTyped(char codePoint, int modifiers) {
        if (popup != null) {
            return popup.charTyped(codePoint, modifiers);
        } else {
            return false;//root.charTyped(codePoint, modifiers);
        }
    }

    boolean changeKeyboardListener(boolean searchNext) {
        //TODO change focus implementation
        return false;
    }

    boolean onBack() {
        if (popup != null) {
            closePopup();
            return true;
        }
        return false;//root.onBack();
    }

    /**
     * Refocus mouse cursor and update mouse position
     */
    public void refreshMouse() {
        mouseMoved(mouseX, mouseY);
    }

    /**
     * Raw draw method, draw entire UI
     */
    void draw() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();
        RenderSystem.disableDepthTest();
        view.draw(canvas, time);
        /*if (popup != null) {
            popup.draw(drawTime);
        }*/
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    /**
     * Called when game window size changed, used to re-layout the UI
     *
     * @param width  scaled game window width
     * @param height scaled game window height
     */
    void resize(int width, int height) {
        this.width = width;
        this.height = height;
        double scale = minecraft.getMainWindow().getGuiScaleFactor();
        mouseX = minecraft.mouseHelper.getMouseX() / scale;
        mouseY = minecraft.mouseHelper.getMouseY() / scale;
        layout();
    }

    /**
     * Layout entire UI views
     * {@link #requestLayout()}
     */
    private void layout() {
        int widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.Mode.EXACTLY);
        int heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.Mode.EXACTLY);
        view.measure(widthSpec, heightSpec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        /*if (popup != null) {
            popup.resize(width, height);
        }*/
        refreshMouse();
        layoutRequested = false;
        ModernUI.LOGGER.debug(MARKER, "Actively Layout Performed");
    }

    void destroy() {
        // Hotfix 1.4.7
        if (guiToOpen == null) {
            animations.clear();
            tasks.clear();
            view = null;
            popup = null;
            fragment = null;
            modernScreen = null;
            doubleClickTime = -10;
            lastLayoutTime = 0;
            layoutRequested = false;
            UIEditor.INSTANCE.setHoveredWidget(null);
            UITools.useDefaultCursor();
            // Hotfix 1.5.8
            minecraft.keyboardListener.enableRepeatEvents(false);
        }
    }

    /**
     * Inner method, do not call
     */
    public void clientTick() {
        ++ticks;
        if (popup != null) {
            popup.tick(ticks);
        }
        if (view != null) {
            view.tick(ticks);
        }
        // view tick() is always called before tasks
        for (DelayedTask task : tasks) {
            task.tick(ticks);
        }
        tasks.removeIf(DelayedTask::shouldRemove);
    }

    /**
     * Inner method, do not call
     */
    @SuppressWarnings("ForLoopReplaceableByForEach")
    public void renderTick(float partialTick) {
        time = ticks + partialTick;

        // remove animations from loop in next frame
        animations.removeIf(Animation::shouldRemove);

        // list size is dynamically changeable, because updating animation may add new animation to the list
        for (int i = 0; i < animations.size(); i++) {
            animations.get(i).update(time);
        }

        // layout after updating animations and before drawing
        if (layoutRequested) {
            // fixed at 60Hz
            if (time - lastLayoutTime > 0.3333333f) {
                lastLayoutTime = time;
                layout();
            }
        }
    }

    /**
     * Get elapsed time from a gui open, update every frame, 20.0 = 1 second
     *
     * @return drawing time
     */
    public float getDrawingTime() {
        return time;
    }

    /**
     * Get elapsed ticks from a gui open, update every tick, 20 = 1 second
     *
     * @return elapsed ticks
     */
    public int getElapsedTicks() {
        return ticks;
    }

    /**
     * Get scaled UI screen width which is equal to game main window width
     *
     * @return window width
     */
    public int getWindowWidth() {
        return width;
    }

    /**
     * Get scaled UI screen height which is equal to game main window height
     *
     * @return window height
     */
    public int getWindowHeight() {
        return height;
    }

    /**
     * Get scaled mouse X position on screen
     *
     * @return mouse x
     */
    public double getMouseX() {
        return mouseX;
    }

    /**
     * Get scaled mouse Y position on screen
     *
     * @return mouse y
     */
    public double getMouseY() {
        return mouseY;
    }

    /**
     * Get main view of current UI
     *
     * @return main view
     */
    public View getMainView() {
        return view;
    }

    /**
     * Request layout all views of current UI in next pre-frame
     */
    public void requestLayout() {
        layoutRequested = true;
    }

    void setHoveredView(@Nullable View view) {
        if (mHovered != view) {
            if (mHovered != null) {
                mHovered.onMouseHoverExit();
            }
            mHovered = view;
            if (mHovered != null) {
                mHovered.onMouseHoverEnter();
            }
            doubleClickTime = -10;
            UIEditor.INSTANCE.setHoveredWidget(view);
        }
    }

    @Nullable
    public View getHoveredView() {
        return mHovered;
    }

    /**
     * Set current active dragging view
     *
     * @param view dragging view
     */
    public void setDragging(@Nullable View view) {
        if (mDragging != view) {
            if (mDragging != null) {
                mDragging.onStopDragging();
            }
            mDragging = view;
            if (mDragging != null) {
                mDragging.onStartDragging();
            }
        }
    }

    @Nullable
    public View getDragging() {
        return mDragging;
    }

    /**
     * Set active keyboard listener to listen key events
     *
     * @param view keyboard view
     */
    public void setKeyboard(@Nullable View view) {
        if (mKeyboard != view) {
            minecraft.keyboardListener.enableRepeatEvents(view != null);
            if (mKeyboard != null) {
                mKeyboard.onStopKeyboard();
            }
            mKeyboard = view;
            if (mKeyboard != null) {
                mKeyboard.onStartKeyboard();
            }
        }
    }

    @Nullable
    public View getKeyboard() {
        return mKeyboard;
    }

    /**
     * Inner method, do not call
     */
    @Override
    public IViewParent getParent() {
        throw new RuntimeException("System view!");
    }

    /**
     * Inner method, do not call
     */
    @Override
    public float getScrollX() {
        return 0;
    }

    /**
     * Inner method, do not call
     */
    @Override
    public float getScrollY() {
        return 0;
    }

}
