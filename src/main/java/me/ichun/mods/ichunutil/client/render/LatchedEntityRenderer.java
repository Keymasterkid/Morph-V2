package me.ichun.mods.ichunutil.client.render;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import me.ichun.mods.ichunutil.common.entity.LatchedEntity;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class LatchedEntityRenderer<T extends LatchedEntity, S extends EntityRenderState> extends EntityRenderer<T, S> {
    private final java.util.function.Supplier<S> stateFactory;

    public LatchedEntityRenderer(EntityRendererProvider.Context ctx, java.util.function.Supplier<S> stateFactory) {
        super(ctx);
        this.stateFactory = stateFactory;
    }

    @Override
    public S createRenderState() {
        return stateFactory.get();
    }

    // @Override
    public ResourceLocation getTextureLocation(S state) {
        return null;
    }

    /*
    @Override
    public S createRenderState(T entity, float partialTick) {
        return stateFactory.get();
    }
    */

    @Override
    public void extractRenderState(T entity, S state, float partialTicks) {
        // super.extractRenderState(entity, state, partialTicks);
    }

    public static class RenderFactory implements EntityRendererProvider<LatchedEntity> {
        @Override
        public EntityRenderer<LatchedEntity, EntityRenderState> create(EntityRendererProvider.Context ctx) {
            return new LatchedEntityRenderer<>(ctx, EntityRenderState::new);
        }
    }
}
