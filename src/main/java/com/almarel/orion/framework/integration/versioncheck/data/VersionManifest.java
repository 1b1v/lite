package com.almarel.orion.framework.integration.versioncheck.data;

import lombok.Getter;

/**
 * Root DTO for manifest.json served from the orion-network repository.
 * Populated purely via Gson reflection — no logic here.
 */
@Getter
public class VersionManifest {
    private EditionManifest lite;
    private EditionManifest full;
    private PromotionInfo promotion;
}