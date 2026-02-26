package me.ichun.mods.ichunutil.client.key;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.bus.api.SubscribeEvent;
// import net.minecraftforge.fml.client.registry.ClientRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public class KeyBind
{
    public static final java.util.List<KeyMapping> KEY_MAPPINGS = new java.util.ArrayList<>();

    @Nonnull
    public final KeyMapping keyBinding;
    @Nullable
    public final Consumer<KeyBind> pressConsumer;
    @Nullable
    public final Consumer<KeyBind> releaseConsumer;
    @Nullable
    public Consumer<KeyBind> tickConsumer;

    public boolean pressed = false;
    public int pressTime = 0;

    public boolean holdable = false;
    public int holdTime = 0;

    /**
     * Construct during Client Setup Event
     * @param keyBinding key binding!
     * @param pressConsumer press consumer
     * @param releaseConsumer release consumer
     */
    public KeyBind(KeyMapping keyBinding, @Nullable Consumer<KeyBind> pressConsumer, @Nullable Consumer<KeyBind> releaseConsumer)
    {
        this.keyBinding = keyBinding;
        this.pressConsumer = pressConsumer;
        this.releaseConsumer = releaseConsumer;

        KEY_MAPPINGS.add(this.keyBinding);

        NeoForge.EVENT_BUS.register(this);
    }

    public KeyBind setTickConsumer(Consumer<KeyBind> tickConsumer)
    {
        this.tickConsumer = tickConsumer;
        return this;
    }

    public KeyBind setHoldable()
    {
        this.holdable = true;
        return this;
    }

    @SubscribeEvent
    public void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event)
    {
        if(pressed)
        {
            pressTime++;
            if(!keyBinding.isDown())
            {
                pressed = false;
                holdTime = 0;
                if(releaseConsumer != null)
                {
                    releaseConsumer.accept(this);
                }
            }
            else
            {
                if(tickConsumer != null)
                {
                    tickConsumer.accept(this);
                }
                if(holdTime > 0)
                {
                    holdTime--;
                    if(holdTime == 0)
                    {
                        holdTime = 5;
                        if(pressConsumer != null)
                        {
                            pressConsumer.accept(this);
                        }
                    }
                }
            }
        }
        else
        {
            pressTime = 0;
            if(keyBinding.isDown())
            {
                pressed = true;
                if(pressConsumer != null)
                {
                    pressConsumer.accept(this);
                }
                if(holdable)
                {
                    holdTime = 20;
                }
            }
        }
    }

    public enum ConflictContext implements IKeyConflictContext
    {
        //Allows in-game modifiers (or lack thereof) to conflict
        IN_GAME_MODIFIER_SENSITIVE {
            @Override
            public boolean isActive()
            {
                return !KeyConflictContext.GUI.isActive();
            }

            @Override
            public boolean conflicts(IKeyConflictContext other)
            {
                return this == other;
            }
        }
    }
}
