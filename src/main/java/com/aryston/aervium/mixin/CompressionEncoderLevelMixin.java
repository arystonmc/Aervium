package com.aryston.aervium.mixin;

import com.aryston.aervium.world.network.PacketCompression;
import java.util.zip.Deflater;
import net.minecraft.network.CompressionEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CompressionEncoder.class)
abstract class CompressionEncoderLevelMixin {
    @Redirect(method = "<init>", at = @At(value = "NEW", target = "()Ljava/util/zip/Deflater;"))
    private Deflater aervium$fasterDeflater() {
        return PacketCompression.newDeflater();
    }
}
