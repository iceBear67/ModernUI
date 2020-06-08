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

package icyllis.modernui.system;

import icyllis.modernui.ui.example.ContainerTest;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nonnull;

/**
 * All references are final, read-only
 */
public class RegistryLibrary {

    /** Used to register things, will be null after setup completed **/
    static RegistryLibrary INSTANCE = new RegistryLibrary();

    /** Sounds **/
    public static SoundEvent BUTTON_CLICK_1 = null;
    public static SoundEvent BUTTON_CLICK_2 = null;

    /** Containers **/
    public static ContainerType<ContainerTest> TEST_CONTAINER = null;

    @OnlyIn(Dist.CLIENT)
    void registerSounds(IForgeRegistry<SoundEvent> registry) {
        BUTTON_CLICK_1 = registerSound(registry, "button1");
        BUTTON_CLICK_2 = registerSound(registry, "button2");
    }

    void registerContainers(IForgeRegistry<ContainerType<?>> registry) {
        TEST_CONTAINER = registerContainer(registry, ContainerTest::new, "test");
    }

    @Nonnull
    @OnlyIn(Dist.CLIENT)
    public static SoundEvent registerSound(@Nonnull IForgeRegistry<SoundEvent> registry, String name) {
        ResourceLocation soundID = new ResourceLocation(ModernUI.MODID, name);
        SoundEvent event = new SoundEvent(soundID).setRegistryName(soundID);
        registry.register(event);
        return event;
    }

    @Nonnull
    public static <T extends Container> ContainerType<T> registerContainer(@Nonnull IForgeRegistry<ContainerType<?>> registry, IContainerFactory<T> factory, String name) {
        ContainerType<T> type = IForgeContainerType.create(factory);
        type.setRegistryName(name);
        registry.register(type);
        return type;
    }
}