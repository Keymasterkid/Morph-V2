package me.ichun.mods.morph.api.mob.trait;

import net.minecraft.util.Mth;

public class StepHeightTrait extends Trait<StepHeightTrait>
{
    public Double amount;

    public StepHeightTrait()
    {
        type = "traitStepHeight";
    }

    @Override
    public void removeHooks()
    {
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT).setBaseValue(0.6F); //return to default, next trait will handle it... hopefully.
    }

    @Override
    public void tick(float strength)
    {
        if(amount != null)
        {
            setStepHeight(amount * strength);
        }
    }

    @Override
    public void transitionalTick(StepHeightTrait prevTrait, float transitionProgress)
    {
        if(prevTrait.amount != null && amount != null)
        {
            setStepHeight(Mth.lerp(transitionProgress, prevTrait.amount, amount));
        }
    }

    private void setStepHeight(double amount)
    {
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT).setBaseValue((float)amount); //Step heights below the default of 0.6 are allowed
    }

    @Override
    public StepHeightTrait copy()
    {
        StepHeightTrait trait = new StepHeightTrait();
        trait.amount = this.amount;
        return trait;
    }
}
