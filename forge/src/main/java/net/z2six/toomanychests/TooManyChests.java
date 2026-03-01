package net.z2six.toomanychests;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.z2six.toomanychests.client.TooManyChestsClient;

@Mod(Constants.MOD_ID)
public class TooManyChests {

    public TooManyChests() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TooManyChestsClient.init(FMLJavaModLoadingContext.get().getModEventBus()));
    }
}
