package me.ichun.mods.ichunutil.common.network;

import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import me.ichun.mods.ichunutil.common.iChunUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.function.Consumer;

/**
 * Compatibility wrapper around the new NeoForge 1.21 IPayload networking system.
 * Internally uses a single PacketHolder payload that dispatches to AbstractPacket subclasses.
 */
public class PacketChannel
{
    private final ResourceLocation channelId;
    private final Object2ByteOpenHashMap<Class<? extends AbstractPacket>> clzToId;
    private final Class<? extends AbstractPacket>[] idToClz;

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public PacketChannel(ResourceLocation name, String protocolVersion, Class<? extends AbstractPacket>... packetTypes)
    {
        this.channelId = name;
        clzToId = new Object2ByteOpenHashMap<>(packetTypes.length);
        for (int i = 0; i < packetTypes.length; i++)
        {
            clzToId.put(packetTypes[i], (byte) i);
        }
        idToClz = packetTypes;
    }

    /**
     * Register this channel's payload type with NeoForge.
     * Call this inside your mod's {@link RegisterPayloadHandlersEvent} handler,
     * or call {@link #registerWithBus(IEventBus)} to do it automatically.
     */
    public void register(PayloadRegistrar registrar)
    {
        registrar.playBidirectional(
                PacketHolderPayload.type(channelId),
                PacketHolderPayload.codec(this),
                // Client handler
                (payload, context) -> payload.inner().process(context),
                // Server handler
                (payload, context) -> payload.inner().process(context)
        );
    }


    /** Convenience: auto-register on the given mod event bus. */
    public void registerWithBus(IEventBus bus)
    {
        bus.addListener((RegisterPayloadHandlersEvent event) -> {
            PayloadRegistrar registrar = event.registrar(channelId.getNamespace());
            register(registrar);
        });
    }

    public void sendToServer(AbstractPacket packet)
    {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientPacketDistributor.sendToServer((CustomPacketPayload) new PacketHolderPayload(channelId, packet));
        }
    }

    public void sendTo(AbstractPacket packet, ServerPlayer player)
    {
        PacketDistributor.sendToPlayer(player, (CustomPacketPayload) new PacketHolderPayload(channelId, packet));
    }

    public void sendToPlayersTrackingEntityAndSelf(AbstractPacket packet, net.minecraft.world.entity.Entity entity)
    {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, (CustomPacketPayload) new PacketHolderPayload(channelId, packet));
    }

    public void sendToAll(AbstractPacket packet)
    {
        PacketDistributor.sendToAllPlayers((CustomPacketPayload) new PacketHolderPayload(channelId, packet));
    }

    public void sendToAllExcept(AbstractPacket packet, ServerPlayer except)
    {
        if (ServerLifecycleHooks.getCurrentServer() != null)
        {
            for (ServerPlayer sp : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers())
            {
                if (sp != except) PacketDistributor.sendToPlayer(sp, (CustomPacketPayload) new PacketHolderPayload(channelId, packet));
            }
        }
    }

    AbstractPacket decode(FriendlyByteBuf buf)
    {
        byte id = buf.readByte();
        if (id < 0 || id >= idToClz.length) throw new IllegalStateException("Unknown packet id: " + id);
        try
        {
            AbstractPacket pkt = idToClz[id].getDeclaredConstructor().newInstance();
            pkt.readFrom(buf);
            return pkt;
        }
        catch (Exception e)
        {
            iChunUtil.LOGGER.error("Failed to decode packet id {}", id, e);
            throw new RuntimeException(e);
        }
    }

    void encode(AbstractPacket packet, FriendlyByteBuf buf)
    {
        buf.writeByte(clzToId.getByte(packet.getClass()));
        packet.writeTo(buf);
    }

    // ---- inner payload record ----

    record PacketHolderPayload(ResourceLocation channelId, AbstractPacket inner) implements CustomPacketPayload
    {
        @Override
        public CustomPacketPayload.Type<PacketHolderPayload> type()
        {
            return type(channelId);
        }

        static CustomPacketPayload.Type<PacketHolderPayload> type(ResourceLocation id)
        {
            return new CustomPacketPayload.Type<>(id);
        }

        static StreamCodec<FriendlyByteBuf, PacketHolderPayload> codec(PacketChannel channel)
        {
            return StreamCodec.of(
                    (buf, holder) -> channel.encode(holder.inner(), buf),
                    (buf) -> new PacketHolderPayload(channel.channelId, channel.decode(buf))
            );
        }
    }
}
