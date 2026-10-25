package org.hp.tinker_foundry.common;

public final class AutomaticMeltingRules {
    public enum Kind {
        NUGGET(10, false), INGOT(90, false), BLOCK(810, false),
        RAW(90, true), RAW_BLOCK(810, true), ORE(90, true),
        DUST(90, false), PLATE(90, false), GEM(90, false);

        public final int amount;
        public final boolean ore;

        Kind(int amount, boolean ore) {
            this.amount = amount;
            this.ore = ore;
        }
    }

    public record Form(String material, Kind kind) {
        public int amount() { return kind.amount; }
        public boolean ore() { return kind.ore; }
    }

    public static Form fromTag(String namespace, String path) {
        if (!namespace.equals("forge") && !namespace.equals("c")) return null;
        int separator = path.indexOf('/');
        if (separator < 0 || separator == path.length() - 1) return null;
        String material = path.substring(separator + 1);
        if (!material.matches("[a-z0-9_.-]+")) return null;
        Kind kind = switch (path.substring(0, separator)) {
            case "nuggets" -> Kind.NUGGET;
            case "ingots" -> Kind.INGOT;
            case "storage_blocks", "blocks" -> Kind.BLOCK;
            case "raw_materials", "raw_ores" -> Kind.RAW;
            case "raw_blocks" -> Kind.RAW_BLOCK;
            case "ores" -> Kind.ORE;
            case "dusts" -> Kind.DUST;
            case "plates" -> Kind.PLATE;
            case "gems" -> Kind.GEM;
            default -> null;
        };
        if (kind == null) return null;
        if (kind == Kind.ORE && (material.equals("singular") || material.equals("sparse") || material.equals("dense"))) return null;
        if (kind == Kind.BLOCK && material.startsWith("raw_")) {
            material = material.substring(4);
            kind = Kind.RAW_BLOCK;
        }
        return material.isEmpty() ? null : new Form(material, kind);
    }

    public static String fromFluidTag(String namespace, String path) {
        if (!namespace.equals("forge") && !namespace.equals("c")) return null;
        String material = path.startsWith("molten_metals/") ? path.substring(14)
            : path.startsWith("molten/") ? path.substring(7)
            : path.startsWith("molten_") ? path.substring(7) : "";
        return material.matches("[a-z0-9_.-]+") ? material : null;
    }

    public static int volume(int standardAmount, int ingotAmount) {
        if (standardAmount <= 0 || ingotAmount <= 0) return 0;
        long amount = (long) standardAmount * ingotAmount / 90;
        return amount > Integer.MAX_VALUE ? 0 : (int) amount;
    }

    public static int ingotVolume(int fluidAmount, int standardAmount, int itemCount) {
        if (fluidAmount <= 0 || standardAmount <= 0 || itemCount <= 0) return 0;
        long amount = (long) fluidAmount * 90 / ((long) standardAmount * itemCount);
        return amount > Integer.MAX_VALUE ? 0 : (int) amount;
    }

    private AutomaticMeltingRules() { }
}
