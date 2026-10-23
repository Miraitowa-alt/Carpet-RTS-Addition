/*
 * This file is part of the Carpet-RTS-Addition project, licensed under CC0-1.0.
 * See LICENSE in the repository root.
 */
package carpet_rts_addition;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

/**
 * Fabric entrypoint: reads the mod version and hands control to the Carpet extension.
 */
public class CarpetRtsAdditionMod implements ModInitializer
{
    public static final String MOD_ID = "carpet-rts-addition";
    public static final String MOD_NAMESPACE = MOD_ID.replace("-", "_");
    public static final String MOD_NAME = "Carpet-RTS-Addition";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static String version;

    @Override
    public void onInitialize()
    {
        version = FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .orElseThrow()
                .getMetadata()
                .getVersion()
                .getFriendlyString();

        CarpetRtsAdditionServer.init();

        LOGGER.info("{} v{} loaded", MOD_NAME, version);
    }

    public static String getModId()
    {
        return MOD_ID;
    }

    public static String getVersion()
    {
        return version;
    }
}
