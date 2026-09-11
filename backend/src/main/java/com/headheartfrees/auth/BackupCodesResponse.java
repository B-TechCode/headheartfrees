package com.headheartfrees.auth;

import java.util.List;

/**
 * Freshly generated backup codes, replacing every previous one.
 *
 * <p>Same warning as {@code TotpEnableResponse}: shown once, never retrievable,
 * and the old codes stopped working the moment this was issued.
 */
public record BackupCodesResponse(List<String> backupCodes) {
}
