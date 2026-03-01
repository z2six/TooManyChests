package net.z2six.toomanychests;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.z2six.toomanychests.client.TooManyChestsClient;

@Mod(Constants.MOD_ID)
public class TooManyChests {

    public TooManyChests(IEventBus eventBus) {
        if (FMLEnvironment.getDist().isClient()) {
            TooManyChestsClient.init(eventBus);
        }
    }
}
