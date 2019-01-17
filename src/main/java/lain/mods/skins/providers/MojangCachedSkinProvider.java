package lain.mods.skins.providers;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import lain.mods.skins.api.interfaces.IPlayerProfile;
import lain.mods.skins.api.interfaces.ISkin;
import lain.mods.skins.api.interfaces.ISkinProvider;
import lain.mods.skins.impl.Shared;
import lain.mods.skins.impl.SkinData;
import lain.mods.skins.impl.fabric.MinecraftUtils;

public class MojangCachedSkinProvider implements ISkinProvider
{

    private File _dirN;
    private File _dirU;
    private Function<ByteBuffer, ByteBuffer> _filter;
    private Map<String, String> _store;

    public MojangCachedSkinProvider(Path workDir)
    {
        _dirN = new File(workDir.toFile(), "skins");
        _dirN.mkdirs();
        _dirU = new File(_dirN, "uuid");
        _dirU.mkdirs();

        for (File file : _dirN.listFiles())
            if (file.isFile())
                file.delete();
        for (File file : _dirU.listFiles())
            if (file.isFile())
                file.delete();
    }

    @Override
    public ISkin getSkin(IPlayerProfile profile)
    {
        SkinData skin = new SkinData();
        if (_filter != null)
            skin.setSkinFilter(_filter);
        Shared.pool.execute(() -> {
            byte[] data = null;
            UUID uuid = profile.getPlayerID();
            if (!Shared.isOfflinePlayerProfile(profile))
            {
                Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = MinecraftUtils.getProfileTextures(profile);
                if (textures != null && textures.containsKey(MinecraftProfileTexture.Type.SKIN))
                {
                    MinecraftProfileTexture tex = textures.get(MinecraftProfileTexture.Type.SKIN);
                    data = CachedReader.create().setLocal(_dirU, uuid.toString()).setRemote(tex.getUrl()).setDataStore(_store).setProxy(MinecraftUtils.getProxy()).read();
                    if (data != null)
                        skin.put(data, "slim".equals(tex.getMetadata("model")) ? "slim" : "default");
                }
            }
        });
        return skin;
    }

    public MojangCachedSkinProvider withFilter(Function<ByteBuffer, ByteBuffer> filter)
    {
        _filter = filter;
        return this;
    }

}
