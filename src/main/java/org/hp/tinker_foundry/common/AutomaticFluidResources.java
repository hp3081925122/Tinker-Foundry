package org.hp.tinker_foundry.common;

import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.hp.tinker_foundry.registry.TFFluids;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.function.Predicate;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
        event.addRepositorySource((consumer, constructor) -> consumer.accept(new Pack(ID, true,
                () -> new MemoryPack(type), TITLE, TITLE, PackCompatibility.COMPATIBLE,
                Pack.Position.TOP, true, PackSource.BUILT_IN, true)));
    }

    private static final class MemoryPack implements PackResources {
        private final PackType type;
        private Map<ResourceLocation, byte[]> resources;

        private MemoryPack(PackType type) {
            this.type = type;
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
            pack.addProperty("pack_format", type == PackType.SERVER_DATA ? SharedConstants.DATA_PACK_FORMAT : SharedConstants.RESOURCE_PACK_FORMAT);
            return pack;
        }

        private byte[] rootMetadata() {
            JsonObject root = new JsonObject();
            root.add("pack", metadata());
            return root.toString().getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public InputStream getRootResource(String path) throws FileNotFoundException {
            if ("pack.mcmeta".equals(path)) {
                return new ByteArrayInputStream(rootMetadata());
            }
            throw new FileNotFoundException(path);
        }

        @Override
        public InputStream getResource(PackType requestedType, ResourceLocation location) throws FileNotFoundException {
            byte[] data = requestedType == type ? resources().get(location) : null;
            if (data == null) {
                throw new FileNotFoundException(location.toString());
            }
            return new ByteArrayInputStream(data);
        }

        @Override
        public Collection<ResourceLocation> getResources(PackType requestedType, String namespace, String directory,
                                                       Predicate<ResourceLocation> filter) {
            if (requestedType != type) {
                return java.util.List.of();
            }
            String prefix = directory.isEmpty() ? "" : directory + "/";
            return resources().keySet().stream().filter(location -> location.getNamespace().equals(namespace)
                    && location.getPath().startsWith(prefix) && filter.test(location)).toList();
        }

        @Override
        public boolean hasResource(PackType requestedType, ResourceLocation location) {
            return requestedType == type && resources().containsKey(location);
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
        public String getName() {
            return ID;
        }

        @Override
        public void close() {
        }
    }
}
