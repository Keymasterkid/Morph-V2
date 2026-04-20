package me.ichun.mods.ichunutil.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * A simple wrapper around DynamicTexture to carry a ResourceLocation identifier.
 * In 1.21.8 the AbstractTexture API changed to use GpuTexture/GpuTextureView;
 * DynamicTexture handles the NativeImage upload correctly without needing
 * bind() / TextureUtil.prepareImage / NativeImage.upload directly.
 */
public class NativeImageTexture extends DynamicTexture
{
    private final ResourceLocation resourceLocation;

    public NativeImageTexture(@Nonnull NativeImage image)
    {
        super(() -> "ichunutil_native_texture", image);
        this.resourceLocation = ResourceLocation.fromNamespaceAndPath("ichunutil", "native_image_" + Math.abs(image.hashCode()));
    }

    public ResourceLocation getResourceLocation()
    {
        return resourceLocation;
    }
}
