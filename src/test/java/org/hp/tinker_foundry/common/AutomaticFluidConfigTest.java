package org.hp.tinker_foundry.common;

import com.google.gson.JsonParser;

public final class AutomaticFluidConfigTest {
    private static int checks;

    public static void run() {
        checks = 0;
        var rgb = parse("{\"material\":\"tin\",\"color\":\"#aabbcc\",\"temperature\":1100}");
        check(rgb.material().equals("tin"), "Material ID");
        check(rgb.color() == 0xFFAABBCC, "RGB must become opaque ARGB");
        check(rgb.temperature() == 1100, "Kelvin temperature");
        var full = parse("{\"material\":\"tinker_foundry:molten/tin\",\"color\":\"#40aabbcc\",\"temperature\":0}");
        check(full.material().equals("tin"), "Explicit generated fluid ID");
        check(full.color() == 0x40AABBCC, "ARGB alpha must be preserved");
        check(full.temperature() == 0, "Minimum temperature");
        check(parse("{\"material\":\"tin\",\"color\":\"#00000000\",\"temperature\":1100}").color() == 0, "Transparent ARGB must be preserved");
        rejects("[]");
        rejects("null");
        rejects("{\"color\":\"#aabbcc\",\"temperature\":1100}");
        rejects("{\"material\":\"tin\",\"temperature\":1100}");
        rejects("{\"material\":\"tin\",\"color\":\"#aabbcc\"}");
        rejects("{\"material\":\"other:tin\",\"color\":\"#aabbcc\",\"temperature\":1100}");
        rejects("{\"material\":\"../tin\",\"color\":\"#aabbcc\",\"temperature\":1100}");
        rejects("{\"material\":\"TIN\",\"color\":\"#aabbcc\",\"temperature\":1100}");
        rejects("{\"material\":\"tin\",\"color\":\"#ggbbcc\",\"temperature\":1100}");
        rejects("{\"material\":\"tin\",\"color\":12,\"temperature\":1100}");
        rejects("{\"material\":\"tin\",\"color\":\"#aabbcc\",\"temperature\":-1}");
        rejects("{\"material\":\"tin\",\"color\":\"#aabbcc\",\"temperature\":1100.5}");
        rejects("{\"material\":\"tin\",\"color\":\"#aabbcc\",\"temperature\":2147483648}");
        rejects("{\"material\":\"tin\",\"color\":\"#aabbcc\",\"temperature\":\"1100\"}");
        System.out.println("Fluid config parser checks passed: " + checks);
    }

    private static AutomaticFluidConfig.Definition parse(String json) {
        return AutomaticFluidConfig.parse(JsonParser.parseString(json));
    }

    private static void rejects(String json) {
        try {
            parse(json);
        } catch (IllegalArgumentException exception) {
            checks++;
            return;
        }
        throw new AssertionError("Invalid fluid config accepted: " + json);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }

    private AutomaticFluidConfigTest() { }
}
