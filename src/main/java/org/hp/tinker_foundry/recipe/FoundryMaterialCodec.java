package org.hp.tinker_foundry.recipe;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.stream.Stream;
import org.hp.tinker_foundry.registry.TFFluids;

public final class FoundryMaterialCodec {
    public static <A> MapCodec<A> wrap(MapCodec<A> codec) {
        return new MapCodec<>() {
            @Override
            public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
                var json = ops.convertTo(JsonOps.INSTANCE, ops.createMap(input.entries()));
                var remapped = TFFluids.remapRecipe(json.getAsJsonObject());
                return ops.getMap(JsonOps.INSTANCE.convertTo(ops, remapped)).flatMap(map -> codec.decode(ops, map));
            }

            @Override
            public <T> RecordBuilder<T> encode(A value, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                return codec.encode(value, ops, prefix);
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return codec.keys(ops);
            }
        };
    }

    private FoundryMaterialCodec() { }
}
