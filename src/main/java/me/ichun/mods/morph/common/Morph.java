package me.ichun.mods.morph.common;

import me.ichun.mods.ichunutil.common.data.AdvancementGen;
import me.ichun.mods.ichunutil.common.network.PacketChannel;
import me.ichun.mods.morph.api.MorphApi;
import me.ichun.mods.morph.api.mob.MobData;
import me.ichun.mods.morph.api.mob.trait.Trait;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.client.config.ConfigClient;
import me.ichun.mods.morph.client.core.EventHandlerClient;
import me.ichun.mods.morph.client.core.KeyBinds;
import me.ichun.mods.morph.client.entity.EntityAcquisition;
import me.ichun.mods.morph.client.entity.EntityBiomassAbility;
import me.ichun.mods.morph.client.render.RenderEntityAcquisition;
import me.ichun.mods.morph.client.render.RenderEntityBiomassAbility;
import me.ichun.mods.morph.common.config.ConfigServer;
import me.ichun.mods.morph.common.core.EventHandlerServer;
import me.ichun.mods.morph.common.mob.MobDataHandler;
import me.ichun.mods.morph.common.mob.TraitHandler;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.packet.*;
import me.ichun.mods.morph.common.resource.ResourceHandler;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.data.DataGenerator;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
// Capability system removed in 1.21 - use Data Attachments
// CapabilityManager removed in 1.21 - use Data Attachments
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
// DistExecutor removed in NeoForge 1.21
// ExtensionPoint removed in NeoForge 1.21
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.registries.DeferredHolder;
// RenderingRegistry removed - use EntityRenderersEvent.RegisterRenderers
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.*;
// FMLJavaModLoadingContext removed in 1.21
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mod(Morph.MOD_ID)
public class Morph
{
    public static final String MOD_NAME = "Morph";
    public static final String MOD_ID = "morph";
    public static final String PROTOCOL = "2"; //Network protocol

    public static final Logger LOGGER = LogManager.getLogger();

    public static ConfigServer configServer;
    public static ConfigClient configClient;

    public static EventHandlerClient eventHandlerClient;
    public static EventHandlerServer eventHandlerServer;

    public static final net.neoforged.neoforge.registries.DeferredRegister<net.neoforged.neoforge.attachment.AttachmentType<?>> ATTACHMENT_TYPES = net.neoforged.neoforge.registries.DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.ATTACHMENT_TYPES, "morph");
    public static final java.util.function.Supplier<net.neoforged.neoforge.attachment.AttachmentType<me.ichun.mods.morph.api.morph.MorphInfo>> MORPH_INFO = ATTACHMENT_TYPES.register("morph_info", () -> net.neoforged.neoforge.attachment.AttachmentType.builder(holder -> {
        if (holder instanceof Player player) {
            return (me.ichun.mods.morph.api.morph.MorphInfo) new me.ichun.mods.morph.common.morph.MorphInfoImpl(player);
        }
        return null;
    }).serialize(new net.neoforged.neoforge.attachment.IAttachmentSerializer<me.ichun.mods.morph.api.morph.MorphInfo>() {
                    @Override
                    public MorphInfo read(IAttachmentHolder holder, ValueInput input)
                    {
                        MorphInfo info = new me.ichun.mods.morph.common.morph.MorphInfoImpl((Player) holder);
                        info.read(input);
                        return info;
                    }
    
                    @Override
                    public boolean write(MorphInfo info, ValueOutput output)
                    {
                        info.write(output);
                        return true;
                    }
    }).copyOnDeath().build());

    public static PacketChannel channel;

    public Morph(net.neoforged.bus.api.IEventBus bus, net.neoforged.fml.ModContainer modContainer)
    {
        if(!ResourceHandler.setupEnv())
        {
            LOGGER.fatal("Error initialising Morph Resource Handler! Terminating init.");
            return;
        }

        configServer = new ConfigServer().init();

        Sounds.REGISTRY.register(bus);
        ATTACHMENT_TYPES.register(bus);

        bus.addListener(this::onCommonSetup);
        bus.addListener(this::processIMC);
        bus.addListener(this::finishLoading);

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(eventHandlerServer = new EventHandlerServer());

        MorphApi.setApiImpl(MorphHandler.INSTANCE);

        channel = new PacketChannel(ResourceLocation.fromNamespaceAndPath("morph", "morph_info"), PROTOCOL,
                PacketPlayerData.class,
                PacketRequestMorphInfo.class,
                PacketMorphInfo.class,
                PacketUpdateMorph.class,
                PacketSessionSync.class,
                PacketMorphInput.class,
                PacketAcquisition.class,
                PacketUpdateBiomassValue.class,
                PacketUpdateBiomassUpgrades.class,
                PacketInvalidateClientHealth.class,
                PacketOpenGenerator.class
        );
        channel.registerWithBus(bus);

        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            configClient = new ConfigClient().init();
            bus.addListener(Morph.EntityTypes::onEntityTypeRegistry);
            bus.addListener(this::onRegisterRenderers);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(eventHandlerClient = new EventHandlerClient());
            KeyBinds.init();

            me.ichun.mods.morph.client.network.ClientPayloadHandler.registerConfigScreen(modContainer);
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event)
    {
        // Capability system replaced by Data Attachments in 1.21 - nothing to register here
    }

    @OnlyIn(Dist.CLIENT)
    private void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event)
    {
    }


    @OnlyIn(Dist.CLIENT)
    private void onRegisterRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(EntityTypes.ACQUISITION, ctx -> new RenderEntityAcquisition(ctx));
        event.registerEntityRenderer(EntityTypes.BIOMASS_ABILITY, ctx -> new RenderEntityBiomassAbility(ctx));
    }

    private void processIMC(InterModProcessEvent event)
    {
        //Register mod trait.
        event.getIMCStream(m -> m.equalsIgnoreCase("trait")).forEach(msg -> {
            Object o = msg.messageSupplier().get();
            if(o instanceof Class)
            {
                Class clz = (Class)o;
                if(Trait.class.isAssignableFrom(clz))
                {
                    try
                    {
                        Trait t = (Trait)clz.newInstance();
                        if(t.type != null && !t.type.isEmpty())
                        {
                            TraitHandler.registerTrait(t.type, clz);
                            LOGGER.info("IMC: Registering trait type {} from mod {}", t.type, msg.senderModId());
                        }
                        else
                        {
                            LOGGER.warn("IMC: Invalid trait type from {}", msg.senderModId());
                        }
                    }
                    catch(InstantiationException | IllegalAccessException e)
                    {
                        LOGGER.error("IMC: Error retrieving trait type from {}", msg.senderModId());
                        e.printStackTrace();
                    }
                }
                else
                {
                    LOGGER.warn("IMC: Non-Trait class type trait from {}", msg.senderModId());
                }
            }
            else
            {
                LOGGER.warn("IMC: Non-class type trait from {}", msg.senderModId());
            }
        });

        //Register mod mob data
        event.getIMCStream(m -> m.equalsIgnoreCase("mob")).forEach(msg -> {
            Object o = msg.messageSupplier().get();
            if(o instanceof MobData)
            {
                MobData data = (MobData)o;
                if(data.forEntity != null && !data.forEntity.isEmpty())
                {
                    ResourceLocation rl = ResourceLocation.parse(data.forEntity);

                    MobDataHandler.registerMobData(rl, data);

                    LOGGER.info("IMC: Registering MobData for {} from mod {}", rl.toString(), msg.senderModId());
                }
                else
                {
                    LOGGER.warn("IMC: Invalid MobData forEntity from {}", msg.senderModId());
                }
            }
            else
            {
                LOGGER.warn("IMC: Non-MobData object from {}", msg.senderModId());
            }
        });

        //Register mod morph synchers
        event.getIMCStream(m -> m.equalsIgnoreCase("morphSync")).forEach(msg -> {
            Object o = msg.messageSupplier().get();
            if(o instanceof BiConsumer)
            {
                BiConsumer consumer = (BiConsumer)o;

                MorphHandler.INSTANCE.getModPlayerMorphSyncConsumers().add(consumer);

                LOGGER.info("IMC: Registering morph sync BiConsumer from mod {}", msg.senderModId());
            }
            else
            {
                LOGGER.warn("IMC: Non-BiConsumer morph sync object from {}", msg.senderModId());
            }
        });

        //Register third party mob NBT tag setters
        event.getIMCStream(m -> m.equalsIgnoreCase("variantNbtSetter")).forEach(msg -> {
            Object o = msg.messageSupplier().get();
            if(o instanceof BiConsumer)
            {
                BiConsumer consumer = (BiConsumer)o;

                MorphHandler.INSTANCE.getVariantNbtTagSetters().add(consumer);

                LOGGER.info("IMC: Registering variant NBT setter BiConsumer from mod {}", msg.senderModId());
            }
            else
            {
                LOGGER.warn("IMC: Non-BiConsumer NBT setter object from {}", msg.senderModId());
            }
        });

        //Register third party mob NBT tag readers
        event.getIMCStream(m -> m.equalsIgnoreCase("variantNbtReader")).forEach(msg -> {
            Object o = msg.messageSupplier().get();
            if(o instanceof BiConsumer)
            {
                BiConsumer consumer = (BiConsumer)o;

                MorphHandler.INSTANCE.getVariantNbtTagReaders().add(consumer);

                LOGGER.info("IMC: Registering variant NBT reader BiConsumer from mod {}", msg.senderModId());
            }
            else
            {
                LOGGER.warn("IMC: Non-BiConsumer NBT reader object from {}", msg.senderModId());
            }
        });
    }

    private void finishLoading(FMLLoadCompleteEvent event)
    {
        ResourceHandler.loadResources();
    }

    public static class Advancements implements Consumer<Consumer<Advancement>>
    {
        @Override
        public void accept(Consumer<Advancement> consumer)
        {
            // Advancement data gen is not implemented for 1.21 compat
        }

        public static final ResourceLocation UNLOCK_BIOMASS = ResourceLocation.fromNamespaceAndPath("morph", "unlock_biomass");
    }

    public static class Sounds
    {
        private static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(net.minecraft.core.registries.Registries.SOUND_EVENT, MOD_ID);

        public static final DeferredHolder<SoundEvent, SoundEvent> MORPH = REGISTRY.register("morph", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("morph", "morph")));
    }

    public static class EntityTypes
    {
        public static EntityType<EntityAcquisition> ACQUISITION;
        public static EntityType<EntityBiomassAbility> BIOMASS_ABILITY;
        private static void onEntityTypeRegistry(final RegisterEvent event) //we're doing it this way because it's a client-side entity and we don't want to sync registry values
        {
            if (event.getRegistryKey().equals(Registries.ENTITY_TYPE)) {
                EntityType<EntityAcquisition> acq = EntityType.Builder.<EntityAcquisition>of(EntityAcquisition::new, MobCategory.MISC)
                        .sized(0.1F, 0.1F)
                        .build(ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "acquisition")));
                event.register(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "acquisition"), () -> acq);
                ACQUISITION = acq;

                EntityType<EntityBiomassAbility> bio = EntityType.Builder.<EntityBiomassAbility>of(EntityBiomassAbility::new, MobCategory.MISC)
                        .sized(0.1F, 0.1F)
                        .build(ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "biomass_ability")));
                event.register(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "biomass_ability"), () -> bio);
                BIOMASS_ABILITY = bio;
            }
        }
    }
}
