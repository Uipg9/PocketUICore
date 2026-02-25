package com.pocketuicore.data;

import com.mojang.serialization.Codec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

/**
 * Pocket Data Store — Generic persistent server-side storage.
 * <p>
 * A reusable wrapper around Minecraft's {@link PersistentState} system
 * that makes it trivial to persist arbitrary data using Codec-based
 * serialisation.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     // 1. Define a Codec for your data class:
 *     public static final Codec<MyData> CODEC = RecordCodecBuilder.create(i -> ...);
 *
 *     // 2. Create the data store type (once, static):
 *     public static final PocketDataStore.Type<MyData> MY_STORE =
 *             PocketDataStore.type("pocketuicore_mydata",
 *                     MyData::new, MyData.CODEC,
 *                     data -> data);
 *
 *     // 3. Access from server-side code:
 *     PocketDataStore<MyData> store = PocketDataStore.get(server, MY_STORE);
 *     MyData data = store.getData();
 *     data.someField = 42;
 *     store.markDirty();
 * }</pre>
 *
 * @param <T> the data type to persist
 */
public class PocketDataStore<T> extends PersistentState {

    private T data;

    public PocketDataStore(T data) {
        this.data = data;
    }

    /** @return the stored data. */
    public T getData() { return data; }

    /** Set the stored data and mark dirty for saving. */
    public void setData(T data) {
        this.data = data;
        markDirty();
    }

    // =====================================================================
    //  Type descriptor
    // =====================================================================

    /**
     * Descriptor for creating a {@link PocketDataStore} type that can be
     * registered with the overworld's PersistentStateManager.
     *
     * @param <T> the data type
     */
    public static final class Type<T> {
        private final PersistentStateType<PocketDataStore<T>> stateType;

        Type(PersistentStateType<PocketDataStore<T>> stateType) {
            this.stateType = stateType;
        }

        public PersistentStateType<PocketDataStore<T>> getStateType() {
            return stateType;
        }
    }

    // =====================================================================
    //  Factory
    // =====================================================================

    /**
     * Define a data store type.
     *
     * @param id       unique file identifier (e.g. "pocketuicore_mydata")
     * @param factory  supplier for a fresh empty instance of T
     * @param codec    Codec for serialising/deserialising T
     * @param toRaw    extractor from PocketDataStore to its raw data for the codec
     * @param <T>      the data type
     * @return a reusable Type descriptor
     */
    public static <T> Type<T> type(String id,
                                    java.util.function.Supplier<T> factory,
                                    Codec<T> codec,
                                    java.util.function.Function<PocketDataStore<T>, T> toRaw) {
        Codec<PocketDataStore<T>> storeCodec = codec.xmap(
                PocketDataStore::new,
                toRaw
        );

        PersistentStateType<PocketDataStore<T>> pst = new PersistentStateType<>(
                id,
                () -> new PocketDataStore<>(factory.get()),
                storeCodec,
                null
        );

        return new Type<>(pst);
    }

    // =====================================================================
    //  Access
    // =====================================================================

    /**
     * Retrieve (or create) a data store from the server's overworld
     * persistent state manager.
     *
     * @param server the running MinecraftServer
     * @param type   the store type descriptor
     * @param <T>    the data type
     * @return the singleton PocketDataStore for this type
     */
    public static <T> PocketDataStore<T> get(MinecraftServer server, Type<T> type) {
        return server.getOverworld()
                     .getPersistentStateManager()
                     .getOrCreate(type.getStateType());
    }
}
