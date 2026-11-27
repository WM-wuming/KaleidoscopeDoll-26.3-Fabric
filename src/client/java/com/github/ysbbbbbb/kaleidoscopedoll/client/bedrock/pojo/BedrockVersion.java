package com.github.ysbbbbbb.kaleidoscopedoll.client.bedrock.pojo;

import com.github.ysbbbbbb.kaleidoscopedoll.KaleidoscopeDoll;

public enum BedrockVersion {
    /**
     * 旧版本基岩版模型，仅限 1.10.0
     */
    LEGACY,
    /**
     * 新版本基岩版模型，往后的 1.14.0，1.16.0 1.21.0 通通用此版本读取
     */
    NEW;

    public static boolean isNewVersion(BedrockModelPOJO bedrockModel) {
        int[] version = parseVersion(bedrockModel.getFormatVersion());
        return compare(version, new int[]{1, 12, 0}) >= 0;
    }

    public static boolean isLegacyVersion(BedrockModelPOJO bedrockModel) {
        int[] version = parseVersion(bedrockModel.getFormatVersion());
        return compare(version, new int[]{1, 10, 0}) == 0;
    }

    private static int[] parseVersion(String version) {
        try {
            String[] parts = version.split("\\.");
            int[] result = new int[3];
            for (int i = 0; i < 3 && i < parts.length; i++) {
                result[i] = Integer.parseInt(parts[i].trim());
            }
            return result;
        } catch (NumberFormatException e) {
            KaleidoscopeDoll.LOGGER.error("Failed to parse model format version: {}", version);
            return new int[]{0, 0, 0};
        }
    }

    private static int compare(int[] a, int[] b) {
        for (int i = 0; i < 3; i++) {
            if (a[i] != b[i]) {
                return Integer.compare(a[i], b[i]);
            }
        }
        return 0;
    }

    public static BedrockVersion getVersion(BedrockModelPOJO pojo) {
        if (isNewVersion(pojo)) {
            return NEW;
        } else if (isLegacyVersion(pojo)) {
            return LEGACY;
        }
        throw new IllegalArgumentException("Invalid version for model: " + pojo.getFormatVersion());
    }
}