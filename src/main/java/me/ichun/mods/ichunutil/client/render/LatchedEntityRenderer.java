package me.ichun.mods.ichunutil.client.render;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import me.ichun.mods.ichunutil.common.entity.LatchedEntity;
public class LatchedEntityRenderer<T extends LatchedEntity> extends EntityRenderer<T> {
    public LatchedEntityRenderer(EntityRendererProvider.Context ctx) { super(ctx); }
    @Override public ResourceLocation getTextureLocation(T entity) { return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS; }
    public static class RenderFactory implements EntityRendererProvider<LatchedEntity> {
        @Override public EntityRenderer<LatchedEntity> create(EntityRendererProvider.Context ctx) { return new LatchedEntityRenderer<>(ctx); }
    }
}
