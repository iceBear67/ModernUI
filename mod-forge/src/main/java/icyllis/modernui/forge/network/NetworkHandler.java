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

package icyllis.modernui.forge.network;

import icyllis.modernui.ModernUI;
import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.event.EventNetworkChannel;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.commons.codec.digest.DigestUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * For handling a network channel more faster, you can create an instance for your mod
 */
@SuppressWarnings("unused")
public class NetworkHandler {

    private final ResourceLocation channel;

    private final String protocol;
    private final boolean optional;

    @Nullable
    private final IClientMsgHandler clientHandler;
    @Nullable
    private final IServerMsgHandler serverHandler;

    protected final Pool<Broadcaster> pool = Pools.concurrent(1);

    /**
     * Create a network handler of a mod. Note that this is a dist-sensitive operation,
     * you may consider the following example:
     *
     * <pre>
     * network = new NetworkHandler(MODID, "main_network", () -> NetMessages::handle, NetMessages::handle, null, false);
     * </pre>
     *
     * @param modid         the mod id
     * @param name          the network channel name, should be short
     * @param clientHandler a handler to handle server-to-client messages, the inner supplier must be in another class
     * @param serverHandler a handler to handle client-to-server messages
     * @param protocol      network protocol version, when null or empty it will request the same version of the mod
     * @param optional      when true it will pass if the mod missing on one side, or request same protocol
     * @see icyllis.modernui.forge.NetMessages
     */
    public NetworkHandler(@Nonnull String modid, @Nonnull String name,
                          @Nonnull Supplier<Supplier<IClientMsgHandler>> clientHandler,
                          @Nullable IServerMsgHandler serverHandler, @Nullable String protocol, boolean optional) {
        if (protocol == null || protocol.isEmpty())
            protocol = DigestUtils.md5Hex(ModList.get().getModFileById(modid).getMods().stream()
                    .map(iModInfo -> iModInfo.getVersion().getQualifier())
                    .collect(Collectors.joining(",")).getBytes(StandardCharsets.UTF_8));
        this.protocol = protocol;
        this.optional = optional;
        this.clientHandler = FMLEnvironment.dist.isClient() ? clientHandler.get().get() : null;
        this.serverHandler = serverHandler;
        EventNetworkChannel network = NetworkRegistry.ChannelBuilder
                .named(channel = new ResourceLocation(modid, name))
                .networkProtocolVersion(this::getProtocolVersion)
                .clientAcceptedVersions(this::checkS2CProtocol)
                .serverAcceptedVersions(this::checkC2SProtocol)
                .eventNetworkChannel();
        if (FMLEnvironment.dist.isClient()) {
            network.addListener(this::onS2CMessageReceived);
        }
        network.addListener(this::onC2SMessageReceived);
    }

    /**
     * Get the protocol version of this channel on current side
     *
     * @return the protocol
     */
    public String getProtocolVersion() {
        return protocol;
    }

    /**
     * This method will run on client to verify the server protocol that sent by handshake network channel
     *
     * @param serverProtocol the protocol of this channel sent from server side
     * @return {@code true} to accept the protocol, {@code false} otherwise
     */
    private boolean checkS2CProtocol(@Nonnull String serverProtocol) {
        boolean allowAbsent = optional && serverProtocol.equals(NetworkRegistry.ABSENT);
        if (allowAbsent) {
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Connecting to a server that does not have {} channel available", channel);
        }
        return allowAbsent || serverProtocol.equals(protocol);
    }

    /**
     * This method will run on server to verify the remote client protocol that sent by handshake network channel
     *
     * @param clientProtocol the protocol of this channel sent from client side
     * @return {@code true} to accept the protocol, {@code false} otherwise
     */
    private boolean checkC2SProtocol(@Nonnull String clientProtocol) {
        if (clientProtocol.equals(NetworkRegistry.ACCEPTVANILLA)) {
            return false;
        }
        return clientProtocol.equals(protocol) || optional && clientProtocol.equals(NetworkRegistry.ABSENT);
    }

    @OnlyIn(Dist.CLIENT)
    private void onS2CMessageReceived(NetworkEvent.ServerCustomPayloadEvent event) {
        // received on main thread of effective side
        if (clientHandler != null) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null)
                clientHandler.handle(event.getPayload().readShort(), event.getPayload(), player);
        }
        event.getPayload().release(); // forge disabled this on client, see ClientPacketListener.handleCustomPayload() finally {}
        event.getSource().get().setPacketHandled(true);
    }

    private void onC2SMessageReceived(NetworkEvent.ClientCustomPayloadEvent event) {
        // received on main thread of effective side
        if (serverHandler != null) {
            ServerPlayer player = event.getSource().get().getSender();
            if (player != null)
                serverHandler.handle(event.getPayload().readShort(), event.getPayload(), player);
        }
        event.getSource().get().setPacketHandled(true);
    }

    /**
     * Allocate a buffer to write packet data with index. Once you done that,
     * pass the value returned here to {@link #prepare(FriendlyByteBuf)}
     *
     * @param index The message index used on the opposite side, range from 0 to 32767
     * @return a byte buf to write the packet data (message)
     * @see IClientMsgHandler
     * @see IServerMsgHandler
     */
    @Nonnull
    public FriendlyByteBuf targetAt(int index) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeShort(index);
        return buffer;
    }

    /**
     * Prepare to send a packet, you must dispatch this packet later,
     * for example {@link Broadcaster#sendToPlayer(Player)}
     *
     * @param buf message
     * @return a broadcaster to dispatch/send/broadcast packet
     * @see #targetAt(int)
     */
    @Nonnull
    public Broadcaster prepare(@Nonnull FriendlyByteBuf buf) {
        Broadcaster b = pool.acquire();
        if (b == null) b = new Broadcaster();
        b.buf = buf;
        return b;
    }

    public class Broadcaster {

        // a ByteBuf wrapper for write data more friendly
        private FriendlyByteBuf buf;

        protected void recycle() {
            buf = null;
            pool.release(this);
        }

        /**
         * Send a message to server
         * <p>
         * This is the only method to be called on the client, the rest needs
         * to be called on the server side
         */
        @OnlyIn(Dist.CLIENT)
        public void sendToServer() {
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            if (connection != null)
                connection.send(new ServerboundCustomPayloadPacket(channel, buf));
            recycle();
        }

        /**
         * Send a message to a player
         *
         * @param player the server player
         */
        public void sendToPlayer(@Nonnull Player player) {
            ((ServerPlayer) player).connection.send(new ClientboundCustomPayloadPacket(channel, buf));
            recycle();
        }

        /**
         * Send a message to a player
         *
         * @param player the server player
         */
        public void sendToPlayer(@Nonnull ServerPlayer player) {
            player.connection.send(new ClientboundCustomPayloadPacket(channel, buf));
            recycle();
        }

        /**
         * Send a message to all specific players
         *
         * @param players players on server
         */
        public void sendToPlayers(@Nonnull Iterable<? extends Player> players) {
            final Packet<?> packet = new ClientboundCustomPayloadPacket(channel, buf);
            for (Player player : players)
                ((ServerPlayer) player).connection.send(packet);
            recycle();
        }

        /**
         * Send a message to all players on the server
         */
        public void sendToAll() {
            ServerLifecycleHooks.getCurrentServer().getPlayerList()
                    .broadcastAll(new ClientboundCustomPayloadPacket(channel, buf));
            recycle();
        }

        /**
         * Send a message to all players in specified dimension
         *
         * @param dimension dimension that players in
         */
        public void sendToDimension(@Nonnull ResourceKey<Level> dimension) {
            ServerLifecycleHooks.getCurrentServer().getPlayerList()
                    .broadcastAll(new ClientboundCustomPayloadPacket(channel, buf), dimension);
            recycle();
        }

        /**
         * Send a message to all players nearby a point with specified radius in specified dimension
         *
         * @param excluded  the player that will not be sent the packet
         * @param x         target point x
         * @param y         target point y
         * @param z         target point z
         * @param radius    radius to target point
         * @param dimension dimension that players in
         */
        public void sendToAllNear(@Nullable ServerPlayer excluded,
                                  double x, double y, double z, double radius,
                                  @Nonnull ResourceKey<Level> dimension) {
            ServerLifecycleHooks.getCurrentServer().getPlayerList().broadcast(excluded,
                    x, y, z, radius, dimension, new ClientboundCustomPayloadPacket(channel, buf));
            recycle();
        }

        /**
         * Send a message to all players tracking the specified entity. If a chunk that player loaded
         * on the client contains the chunk where the entity is located, and then the player is
         * tracking the entity changes.
         *
         * @param entity entity is tracking
         */
        public void sendToTrackingEntity(@Nonnull Entity entity) {
            ((ServerLevel) entity.level).getChunkSource().broadcast(
                    entity, new ClientboundCustomPayloadPacket(channel, buf));
            recycle();
        }

        /**
         * Send a message to all players tracking the specified entity, and also send the message to
         * the entity if it is a player. If a chunk that player loaded on the client contains the
         * chunk where the entity is located, and then the player is tracking the entity changes.
         *
         * @param entity the entity is tracking
         */
        public void sendToTrackingAndSelf(@Nonnull Entity entity) {
            ((ServerLevel) entity.level).getChunkSource().broadcastAndSend(
                    entity, new ClientboundCustomPayloadPacket(channel, buf));
            recycle();
        }

        /**
         * Send a message to all players who loaded the specified chunk
         *
         * @param chunk the chunk that players in
         */
        public void sendToTrackingChunk(@Nonnull LevelChunk chunk) {
            final Packet<?> packet = new ClientboundCustomPayloadPacket(channel, buf);
            ((ServerLevel) chunk.getLevel()).getChunkSource().chunkMap.getPlayers(
                    chunk.getPos(), false).forEach(player -> player.connection.send(packet));
            recycle();
        }
    }

    @FunctionalInterface
    public interface IClientMsgHandler {

        /**
         * Handle a server-to-client network message
         *
         * @param index   message index
         * @param payload packet payload
         * @param player  the local player
         */
        void handle(short index, @Nonnull FriendlyByteBuf payload, @Nonnull LocalPlayer player);
    }

    @FunctionalInterface
    public interface IServerMsgHandler {

        /**
         * Handle a client-to-server network message
         *
         * @param index   message index
         * @param payload packet payload
         * @param player  the server player
         */
        void handle(short index, @Nonnull FriendlyByteBuf payload, @Nonnull ServerPlayer player);
    }
}
