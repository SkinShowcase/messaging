package com.skinsshowcase.messaging.config;

/**
 * Служебный «собеседник» поддержки:
 * RAW_VALUE — внешний SteamID64 для API,
 * HASH_VALUE — формат хранения в БД.
 */
public final class SupportSyntheticSteamId {

    public static final String RAW_VALUE = "0".repeat(17);
    public static final String HASH_VALUE = "c1c0605291078b71f6fae716e2e497fe15ec1a977c269b2cb2229dfaf61f7a13";
    public static final String VALUE = HASH_VALUE;

    private SupportSyntheticSteamId() {
    }
}
