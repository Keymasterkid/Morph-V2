package me.ichun.mods.ichunutil.client.tracker.render;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import me.ichun.mods.ichunutil.client.tracker.entity.EntityTracker;
public class RenderTracker extends EntityRenderer<EntityTracker> {
    public RenderTracker(EntityRendererProvider.Context ctx) { super(ctx); }
    @Override public ResourceLocation getTextureLocation(EntityTracker entity) { return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS; }
    public static class RenderFactory implements EntityRendererProvider<EntityTracker> {
        @Override public EntityRenderer<EntityTracker> create(EntityRendererProvider.Context ctx) { return new RenderTracker(ctx); }
    }
}
