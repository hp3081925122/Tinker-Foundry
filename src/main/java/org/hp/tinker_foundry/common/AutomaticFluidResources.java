package org.hp.tinker_foundry.common;

import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.bus.api.IEventBus;
import org.hp.tinker_foundry.registry.TFFluids;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public final class AutomaticFluidResources {
    private static final String ID = "tinker_foundry:automatic_fluids";
    private static final Component TITLE = Component.literal("Tinker Foundry Automatic Fluids");

    private AutomaticFluidResources() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(AutomaticFluidResources::addPacks);
    }

    private static void addPacks(AddPackFindersEvent event) {
        PackType type = event.getPackType();
        event.addRepositorySource(consumer -> {
            PackLocationInfo location = new PackLocationInfo(ID, TITLE, PackSource.BUILT_IN, Optional.empty());
            Pack pack = Pack.readMetaAndCreate(location, new Pack.ResourcesSupplier() {
                @Override
                public PackResources openPrimary(PackLocationInfo info) {
                    return new MemoryPack(type, info);
                }

                @Override
                public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                    return new MemoryPack(type, info);
                }
            }, type, new PackSelectionConfig(true, Pack.Position.TOP, true));
            if (pack != null) {
                consumer.accept(pack.hidden());
            }
        });
    }

    private static final class MemoryPack implements PackResources {
        private final PackType type;
        private final PackLocationInfo location;
        private Map<ResourceLocation, byte[]> resources;

        private MemoryPack(PackType type, PackLocationInfo location) {
            this.type = type;
            this.location = location;
        }

        private Map<ResourceLocation, byte[]> resources() {
            if (resources == null) {
                resources = Map.copyOf(TFFluids.automaticResources(type == PackType.CLIENT_RESOURCES));
            }
            return resources;
        }

        private JsonObject metadata() {
            JsonObject pack = new JsonObject();
            pack.addProperty("description", "Tinker Foundry Automatic Fluids");
            pack.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(type));
            return pack;
        }

        private byte[] rootMetadata() {
            JsonObject root = new JsonObject();
            root.add("pack", metadata());
            return root.toString().getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public IoSupplier<InputStream> getRootResource(String... path) {
            return path.length == 1 && "pack.mcmeta".equals(path[0])
                    ? () -> new ByteArrayInputStream(rootMetadata()) : null;
        }

        @Override
        public IoSupplier<InputStream> getResource(PackType requestedType, ResourceLocation location) {
            byte[] data = requestedType == type ? resources().get(location) : null;
            return data == null ? null : () -> new ByteArrayInputStream(data);
        }

        @Override
        public void listResources(PackType requestedType, String namespace, String directory, ResourceOutput output) {
            if (requestedType != type) {
                return;
            }
            String prefix = directory.isEmpty() ? "" : directory + "/";
            resources().forEach((location, data) -> {
                if (location.getNamespace().equals(namespace) && location.getPath().startsWith(prefix)) {
                    output.accept(location, () -> new ByteArrayInputStream(data));
                }
            });
        }

        @Override
        public Set<String> getNamespaces(PackType requestedType) {
            Set<String> namespaces = new TreeSet<>();
            if (requestedType == type) {
                resources().keySet().forEach(location -> namespaces.add(location.getNamespace()));
            }
            return namespaces;
        }

        @Override
        public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
            return "pack".equals(serializer.getMetadataSectionName()) ? serializer.fromJson(metadata()) : null;
        }

        @Override
        public PackLocationInfo location() {
            return location;
        }

        @Override
        public void close() {
        }
    }
}
