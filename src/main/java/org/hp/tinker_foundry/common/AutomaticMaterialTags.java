package org.hp.tinker_foundry.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.hp.tinker_foundry.TinkerFoundry;

public final class AutomaticMaterialTags {
    public record Material(String name, List<String> fluids, int color, int temperature, boolean declared) {
        public String registryName() { return "molten/" + name; }
        public String generatedId() { return "tinker_foundry:" + registryName(); }
    }

    private record Entry(String id, boolean tag, boolean required) { }

    private final Map<String, List<Entry>> items = new LinkedHashMap<>();
    private final Map<String, List<Entry>> fluids = new LinkedHashMap<>();
    private final Map<String, JsonObject> properties = new HashMap<>();
    private final Set<String> declaredMaterials = new HashSet<>();
    private final Set<String> gemstones = new HashSet<>();
    // 带标准锭标签但不是金属的材料
    private static final Set<String> NON_METALS = Set.of("brick", "nether_brick");
    private final Map<String, AutomaticFluidConfig.Definition> configuredMaterials;
    private final Map<String, Material> materials;

    public record Resource(String path, String text) { }

    public AutomaticMaterialTags(List<Path> roots) {
        this(readResources(roots), true);
    }

    public static AutomaticMaterialTags fromResources(List<Resource> resources) {
        return new AutomaticMaterialTags(resources, true);
    }

    private AutomaticMaterialTags(List<Resource> resources, boolean indexed) {
        for (Resource resource : resources) readResource(resource);
        configuredMaterials = AutomaticFluidConfig.load();
        declaredMaterials.addAll(configuredMaterials.keySet());
        Set<String> found = new LinkedHashSet<>(declaredMaterials);
        // 只出现在 storage_blocks 里的材料（干草块、骨块、海带块等）不是金属，必须有锭、粒、矿石、宝石等形态才算材料
        Set<String> blockOnly = new LinkedHashSet<>();
        Set<String> qualified = new HashSet<>();
        items.forEach((id, entries) -> {
            int colon = id.indexOf(':');
            var form = AutomaticMeltingRules.fromTag(id.substring(0, colon), id.substring(colon + 1));
            if (form != null && !resolve(items, id, new HashSet<>()).isEmpty()) {
                if (form.kind() == AutomaticMeltingRules.Kind.BLOCK) blockOnly.add(form.material());
                else qualified.add(form.material());
                if (form.kind() == AutomaticMeltingRules.Kind.GEM) gemstones.add(form.material());
            }
        });
        // 非金属排除：带锭标签的砖类等，显式声明（流体标签或配置文件）的材料不受影响
        for (String name : qualified) if (!NON_METALS.contains(name) || declaredMaterials.contains(name)) found.add(name);
        blockOnly.removeAll(found);
        if (!blockOnly.isEmpty()) TinkerFoundry.LOGGER.info("Automatic material tags: skipped block-only or non-metal materials {}", blockOnly);
        Map<String, Material> result = new TreeMap<>();
        for (String name : found) {
            Set<String> candidates = new LinkedHashSet<>();
            fluids.keySet().stream().filter(id -> name.equals(fluidMaterial(id))).forEach(id -> candidates.addAll(resolve(fluids, id, new HashSet<>())));
            candidates.removeIf(id -> id.startsWith("tinker_foundry:molten/"));
            JsonObject options = properties.getOrDefault(name, new JsonObject());
            AutomaticFluidConfig.Definition configured = configuredMaterials.get(name);
            int color = configured != null ? configured.color()
                : options.has("color") ? (int) Long.parseLong(options.get("color").getAsString().replace("#", ""), 16) : 0xFFD88943;
            if (configured == null && (color & 0xFF000000) == 0) color |= 0xFF000000;
            int temperature = configured != null ? configured.temperature()
                : options.has("temperature") ? Math.max(300, options.get("temperature").getAsInt()) : 1100;
            result.put(name, new Material(name, List.copyOf(candidates), color, temperature, declaredMaterials.contains(name)));
        }
        materials = Collections.unmodifiableMap(result);
        TinkerFoundry.LOGGER.info("Automatic material tags: materials={}, externalFluidMappings={}", materials.size(), materials.values().stream().filter(material -> !material.fluids().isEmpty()).count());
    }

    private static List<Resource> readResources(List<Path> roots) {
        List<Resource> result = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) continue;
            try (var paths = Files.walk(root)) {
                for (Path file : paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".json")).sorted().toList()) {
                    String relative = root.relativize(file).toString().replace('\\', '/');
                    if (!relative.contains("/tags/")) continue;
                    try {
                        result.add(new Resource(relative, Files.readString(file, StandardCharsets.UTF_8)));
                    } catch (IOException exception) {
                        TinkerFoundry.LOGGER.warn("Unable to read automatic material resource {}: {}", file, exception.toString());
                    }
                }
            } catch (IOException exception) {
                TinkerFoundry.LOGGER.warn("Unable to scan automatic material root {}: {}", root, exception.toString());
            }
        }
        return result;
    }

    private void readResource(Resource resource) {
        String relative = resource.path().startsWith("data/") ? resource.path().substring(5) : resource.path();
        int slash = relative.indexOf('/');
        if (slash < 0 || !relative.endsWith(".json")) return;
        String namespace = relative.substring(0, slash);
        String path = relative.substring(slash + 1);
        boolean item = path.startsWith("tags/item/") || path.startsWith("tags/items/");
        boolean fluid = path.startsWith("tags/fluid/") || path.startsWith("tags/fluids/");
        if (!item && !fluid) return;
        try {
            JsonObject json = JsonParser.parseString(resource.text()).getAsJsonObject();
            if (json.has("forge:conditions") || json.has("neoforge:conditions")) return;
            String tagPath = path.substring(path.indexOf('/', 5) + 1, path.length() - 5);
            String id = namespace + ":" + tagPath;
            Map<String, List<Entry>> table = item ? items : fluids;
            List<Entry> entries = table.computeIfAbsent(id, key -> new ArrayList<>());
            if (json.has("replace") && json.get("replace").getAsBoolean()) entries.clear();
            if (json.has("values")) for (JsonElement value : json.getAsJsonArray("values")) {
                String target = value.isJsonPrimitive() ? value.getAsString() : value.getAsJsonObject().get("id").getAsString();
                boolean required = value.isJsonPrimitive() || !value.getAsJsonObject().has("required") || value.getAsJsonObject().get("required").getAsBoolean();
                entries.add(new Entry(target.startsWith("#") ? target.substring(1) : target, target.startsWith("#"), required));
                if (fluid && target.startsWith("tinker_foundry:molten/") && target.substring(22).equals(fluidMaterial(id))) declaredMaterials.add(target.substring(22));
            }
            if (fluid && json.has("tinker_foundry")) {
                String material = fluidMaterial(id);
                if (material != null) properties.put(material, json.getAsJsonObject("tinker_foundry"));
            }
        } catch (RuntimeException exception) {
            TinkerFoundry.LOGGER.warn("Unable to read automatic material resource {}: {}", relative, exception.toString());
        }
    }

    private Set<String> resolve(Map<String, List<Entry>> table, String id, Set<String> visiting) {
        Set<String> result = new LinkedHashSet<>();
        if (!visiting.add(id)) return result;
        for (Entry entry : table.getOrDefault(id, List.of())) {
            if (entry.tag()) result.addAll(resolve(table, entry.id(), visiting));
            else {
                int colon = entry.id().indexOf(':');
                if (colon > 0 && entry.required()) result.add(entry.id());
            }
        }
        visiting.remove(id);
        return result;
    }

    private static String fluidMaterial(String id) {
        int colon = id.indexOf(':');
        return AutomaticMeltingRules.fromFluidTag(id.substring(0, colon), id.substring(colon + 1));
    }


    public Map<String, Material> materials() { return materials; }
    public boolean isGemstone(String material) { return gemstones.contains(material); }
    public boolean isConfigured(String material) { return configuredMaterials.containsKey(material); }
}
