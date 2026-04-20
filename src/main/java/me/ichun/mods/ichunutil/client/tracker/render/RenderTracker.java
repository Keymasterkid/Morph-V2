package me.ichun.mods.ichunutil.client.tracker.render;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.ResourceLocation;
import me.ichun.mods.ichunutil.client.tracker.entity.EntityTracker;

public class RenderTracker extends EntityRenderer<EntityTracker, EntityRenderState> {
    public RenderTracker(EntityRendererProvider.Context ctx) { super(ctx); }
    // @Override
    public ResourceLocation getTextureLocation(EntityRenderState state) { return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS; }
    
    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void extractRenderState(EntityTracker entity, EntityRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
    }

    public static class RenderFactory implements EntityRendererProvider<EntityTracker> {
        @Override public EntityRenderer<EntityTracker, EntityRenderState> create(EntityRendererProvider.Context ctx) { return new RenderTracker(ctx); }
    }
}
