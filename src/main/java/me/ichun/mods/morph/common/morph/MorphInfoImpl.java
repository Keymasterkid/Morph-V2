package me.ichun.mods.morph.common.morph;

import me.ichun.mods.ichunutil.common.entity.util.EntityHelper;
import me.ichun.mods.morph.api.mob.trait.Trait;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.api.morph.MorphVariant;
import me.ichun.mods.morph.client.entity.EntityBiomassAbility;
import me.ichun.mods.morph.client.render.MorphRenderHandler;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.packet.PacketInvalidateClientHealth;
import me.ichun.mods.morph.mixin.EntityInvokerMixin;
import me.ichun.mods.morph.mixin.LivingEntityInvokerMixin;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

public class MorphInfoImpl extends MorphInfo
{
    public static final ResourceLocation MORPH_ATTRIBUTE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Morph.MOD_ID, "morph_attribute_modifier");

    private final Random rand = new Random();

    @OnlyIn(Dist.CLIENT)
    public MorphRenderHandler.MorphTransitionState transitionState;
    @OnlyIn(Dist.CLIENT)
    public EntityBiomassAbility entityBiomassAbility;

    public MorphInfoImpl(Player player)
    {
        super(player);
    }

    @Override
    public void tick()
    {
        if(!isMorphed())
        {
            return;
        }

        float transitionProgress = getTransitionProgressLinear(1F);

        if(firstTick)
        {
            firstTick = false;
            player.refreshDimensions();
            applyAttributeModifiers(transitionProgress);
        }

        if(transitionProgress < 1.0F) // is morphing
        {
            if(!player.level().isClientSide)
            {
                if(playSoundTime < 0)
                {
                    playSoundTime = Math.max(0, (int)((morphingTime - 60) / 2F)); // our sounds are 3 seconds long. play it in the middle of the morph
                }

                if(morphTime == playSoundTime)
                {
                    // player.level().playMovingSound(null, player, Morph.Sounds.MORPH.get(), player.getSoundSource(), 1.0F, 1.0F);
                }
            }
            prevState.tick(player, transitionProgress > 0F);
            nextState.tick(player, true);

            float prevStateTraitStrength = 1F - Mth.clamp(transitionProgress / 0.5F, 0F, 1F);
            float nextStateTraitStrength = Mth.clamp((transitionProgress - 0.5F) / 0.5F, 0F, 1F);

            ArrayList<Trait<?>> prevTraits = new ArrayList<>(prevState.traits);
            for(Trait trait : nextState.traits)
            {
                boolean foundTranslatableTrait = false;
                for(int i = prevTraits.size() - 1; i >= 0; i--)
                {
                    Trait<?> prevTrait = prevTraits.get(i);
                    if(prevTrait.canTransitionTo(trait))
                    {
                        prevTraits.remove(i); //remove it

                        foundTranslatableTrait = true;

                        trait.doTransitionalTick(prevTrait, transitionProgress);

                        break;
                    }
                }

                if(!foundTranslatableTrait) //only nextState has this trait
                {
                    trait.doTick(nextStateTraitStrength);
                }
            }

            for(Trait<?> value : prevTraits)
            {
                value.doTick(prevStateTraitStrength);
            }
        }
        else
        {
            nextState.tick(player, false);
            nextState.tickTraits();
        }

        morphTime++;
        if(morphTime <= morphingTime) //still morphing
        {
            player.refreshDimensions();
            applyAttributeModifiers(transitionProgress);
        }
        else if(Morph.configServer.aggressiveSizeRecalculation)
        {
            player.refreshDimensions();
        }

        if(morphTime == morphingTime)
        {
            removeAttributeModifiersFromPrevState();
            setPrevState(null); //bye bye last state. We don't need you anymore.

            if(player.level().isClientSide)
            {
                endMorphOnClient();
            }

            if(nextState.variant.id.equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER)) && nextState.variant.thisVariant.identifier.equals(MorphVariant.IDENTIFIER_DEFAULT_PLAYER_STATE))
            {
                setNextState(null);
            }
        }

        if(player.level().isClientSide)
        {
            if(entityBiomassAbility != null && entityBiomassAbility.isRemoved())
            {
                entityBiomassAbility = null; //have it, GC
            }
        }
    }

    public void applyAttributeModifiers(float transitionProgress)
    {
        if(player.level().isClientSide) //we don't touch the attributes on the client
        {
            return;
        }

        HashMap<net.minecraft.world.entity.ai.attributes.Attribute, Double> attributeModifierAmount = new HashMap<>();


        //Add the next state's attribute modifier amounts
        for(String key : nextState.variant.nbtMorph.getAllKeys())
        {
            if(key.startsWith("attr_")) //it's an attribute key
            {
                ResourceLocation id = ResourceLocation.parse(key.substring(5));

                if(id.toString().equals("minecraft:generic.max_health") && !Morph.configServer.healthScale)
                {
                    continue;
                }

                BuiltInRegistries.ATTRIBUTE.getHolder(id).ifPresent(holder -> {
                    AttributeInstance playerAttribute = player.getAttribute(holder);
                    if(playerAttribute != null)
                    {
                        double baseValue = playerAttribute.getBaseValue();
                        double modifierValue = nextState.variant.nbtMorph.getDouble(key) - baseValue;

                        // If we're transitioning and this is max health, don't apply nextState's modifier yet
                        if (transitionProgress < 1.0F && holder.value() == net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH.value()) {
                            modifierValue = 0;
                        }

                        attributeModifierAmount.put(holder.value(), modifierValue);
                    }
                });
            }
        }

        if(transitionProgress < 1.0F && prevState != null) //we still have a prev state, aka still morphing
        {
            HashSet<net.minecraft.world.entity.ai.attributes.Attribute> prevStateAttrs = new HashSet<>();
            for(String key : prevState.variant.nbtMorph.getAllKeys())
            {
                if(key.startsWith("attr_")) //it's an attribute key
                {
                    ResourceLocation id = ResourceLocation.parse(key.substring(5));

                    if(id.toString().equals("minecraft:generic.max_health") && !Morph.configServer.healthScale)
                    {
                        continue;
                    }

                    BuiltInRegistries.ATTRIBUTE.getHolder(id).ifPresent(holder -> {
                        AttributeInstance playerAttribute = player.getAttribute(holder);
                        if(playerAttribute != null)
                        {
                            double baseValue = playerAttribute.getBaseValue();
                            double modifierValue = prevState.variant.nbtMorph.getDouble(key) - baseValue;

                            if(attributeModifierAmount.containsKey(holder.value())) //the nextState also has this attribute
                            {
                                double val;
                                if (holder.value() == net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH.value()) {
                                    val = modifierValue; // Stay at prevState's health during transition
                                } else {
                                    val = modifierValue + (attributeModifierAmount.get(holder.value()) - modifierValue) * transitionProgress;
                                }
                                attributeModifierAmount.put(holder.value(), val);
                            }
                            else
                            {
                                double val;
                                if (holder.value() == net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH.value()) {
                                    val = modifierValue; // Stay at prevState's health during transition
                                } else {
                                    val = modifierValue * (1F - transitionProgress);
                                }
                                attributeModifierAmount.put(holder.value(), val);
                            }
                            prevStateAttrs.add(holder.value());
                        }
                    });
                }
            }

            for(Map.Entry<net.minecraft.world.entity.ai.attributes.Attribute, Double> e : attributeModifierAmount.entrySet())
            {
                if(!prevStateAttrs.contains(e.getKey())) //this is added by nextState, we need to decrease the modifier since we're still transitioning
                {
                    // Skip max health here, we handled it above
                    if (e.getKey() != net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH.value()) {
                        e.setValue(e.getValue() * transitionProgress);
                    }
                }
            }
        }

        //add these modifiers to the player
        for(Map.Entry<net.minecraft.world.entity.ai.attributes.Attribute, Double> e : attributeModifierAmount.entrySet())
        {
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> holder = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(e.getKey());
            AttributeInstance playerAttribute = player.getAttribute(holder);
            if(playerAttribute != null)
            {
                rand.setSeed(Math.abs("MorphAttr".hashCode() * 1231543L + BuiltInRegistries.ATTRIBUTE.getKey(e.getKey()).toString().hashCode() * 268L));
                ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath("morph", "attr_" + BuiltInRegistries.ATTRIBUTE.getKey(e.getKey()).getPath());

                double lastRatio = 0D;
                boolean isMaxHealth = e.getKey() == net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH.value();

                if(isMaxHealth) //special casing for the max health
                {
                    lastRatio = player.getHealth() / player.getMaxHealth();
                }

                //you can't reapply the same modifier, so lets remove it first
                playerAttribute.removeModifier(modifierId);

                if(e.getValue() != 0) //if the modifier is non-zero, add it
                {
                    playerAttribute.addTransientModifier(new AttributeModifier(modifierId, e.getValue(), AttributeModifier.Operation.ADD_VALUE));

                    if(isMaxHealth && lastRatio > 0D) //we're doing the max health
                    {
                        double currentRatio = player.getHealth() / player.getMaxHealth();

                        if(currentRatio != lastRatio) //if ratio is different, change the health
                        {
                            double targetHealth = lastRatio * player.getMaxHealth();
                            double extraHealth = targetHealth - player.getHealth();

                            // Morph.channel.sendTo(new PacketInvalidateClientHealth(), (net.minecraft.server.level.ServerPlayer) player);
                            player.setHealth(player.getHealth() + (float) extraHealth);
                        }
                    }
                }
            }
        }
    }

    public void removeAttributeModifiersFromPrevState()
    {
        if(prevState != null) //just in case?
        {
            HashSet<net.minecraft.world.entity.ai.attributes.Attribute> attributesToRemove = new HashSet<>();

            //Add the prev state's attributes
            for(String key : prevState.variant.nbtMorph.getAllKeys())
            {
                if(key.startsWith("attr_")) //it's an attribute key
                {
                    ResourceLocation id = ResourceLocation.parse(key.substring(5));
                    BuiltInRegistries.ATTRIBUTE.getHolder(id).ifPresent(holder -> attributesToRemove.add(holder.value()));
                }
            }

            //Remove attributes that nextState also has (we keep those active, they stay morphed)
            for(String key : nextState.variant.nbtMorph.getAllKeys())
            {
                if(key.startsWith("attr_")) //it's an attribute key
                {
                    ResourceLocation id = ResourceLocation.parse(key.substring(5));
                    BuiltInRegistries.ATTRIBUTE.getHolder(id).ifPresent(holder -> attributesToRemove.remove(holder.value()));
                }
            }

            for(net.minecraft.world.entity.ai.attributes.Attribute attribute : attributesToRemove)
            {
                net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> holder = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute);
                AttributeInstance playerAttribute = player.getAttribute(holder);
                if(playerAttribute != null)
                {
                    ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath("morph", "attr_" + BuiltInRegistries.ATTRIBUTE.getKey(attribute).getPath());
                    playerAttribute.removeModifier(modifierId);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void endMorphOnClient()
    {
        if(transitionState != null)
        {
            transitionState = null;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected float getAbilitySkinAlpha(float partialTick)
    {
        if(entityBiomassAbility != null)
        {
            float alpha;
            if(entityBiomassAbility.age < entityBiomassAbility.fadeTime)
            {
                alpha = EntityHelper.sineifyProgress(Mth.clamp((entityBiomassAbility.age + partialTick) / entityBiomassAbility.fadeTime, 0F, 1F));
            }
            else if(entityBiomassAbility.age >= entityBiomassAbility.fadeTime + entityBiomassAbility.solidTime)
            {
                alpha = EntityHelper.sineifyProgress(1F - Mth.clamp((entityBiomassAbility.age - (entityBiomassAbility.fadeTime + entityBiomassAbility.solidTime) + partialTick) / entityBiomassAbility.fadeTime, 0F, 1F));
            }
            else
            {
                alpha = 1F;
            }
            return alpha;
        }
        return 0F;
    }

    @Override
    public void playStepSound(BlockPos pos, BlockState blockState)
    {
        ((EntityInvokerMixin)getActiveMorphEntityOrPlayer()).callPlayStepSound(pos, blockState);
    }

    @Override
    public void playSwimSound()
    {
        ((EntityInvokerMixin)getActiveMorphEntityOrPlayer()).callPlaySwimSound(1.0F);
    }

    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        return ((LivingEntityInvokerMixin)getActiveMorphEntityOrPlayer())
                .callGetHurtSound(source);
    }

    @Override
    public SoundEvent getDeathSound() {
        return ((LivingEntityInvokerMixin)getActiveMorphEntityOrPlayer())
                .callGetDeathSound();
    }

    @Override
    public SoundEvent getFallSound(int height)
    {
        // 1.21.1: Player no longer exposes getFallDamageSound
        // so for now we do not override fall sounds for morphs
        return null;
    }

    @Override
    public SoundEvent getDrinkSound(ItemStack stack) {
        return ((LivingEntityInvokerMixin)getActiveMorphEntityOrPlayer()).callGetDrinkingSound(stack);
    }

    @Override
    public SoundEvent getEatSound(ItemStack stack) {
        return getActiveMorphEntityOrPlayer().getEatingSound(stack);
    }

    @Override
    public float getSoundVolume()
    {
        if(nextState != null)
        {
            if(prevState != null)
            {
                float transitionProg = getTransitionProgressLinear(1F);

                float prevVolume = ((LivingEntityInvokerMixin)prevState.getEntityInstance(player.level(), player)).callGetSoundVolume();
                float nextVolume = ((LivingEntityInvokerMixin)nextState.getEntityInstance(player.level(), player)).callGetSoundVolume();

                return prevVolume + (nextVolume - prevVolume) * transitionProg;
            }
            else
            {
                return ((LivingEntityInvokerMixin)nextState.getEntityInstance(player.level(), player)).callGetSoundVolume();
            }
        }

        return 1F;
    }

    @Override
    public float getSoundPitch()
    {
        if(nextState != null)
        {
            if(prevState != null)
            {
                float transitionProg = getTransitionProgressLinear(1F);

                float prevPitch = ((LivingEntityInvokerMixin)prevState.getEntityInstance(player.level(), player)).callGetVoicePitch();
                float nextPitch = ((LivingEntityInvokerMixin)nextState.getEntityInstance(player.level(), player)).callGetVoicePitch();

                return prevPitch + (nextPitch - prevPitch) * transitionProg;
            }
            else
            {
                return ((LivingEntityInvokerMixin)nextState.getEntityInstance(player.level(), player)).callGetVoicePitch();
            }
        }

        return 1F;
    }
}
