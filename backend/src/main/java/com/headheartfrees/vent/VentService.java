package com.headheartfrees.vent;

/**
 * The vent module's public surface.
 *
 * <p>Per PROJECT_BRIEF.md section 4 this interface and the DTOs beside it are
 * the only things another domain may depend on. The entity and repository stay
 * package-private, so the module can be lifted into its own service later
 * without rewriting callers.
 *
 * <p>Note what this interface cannot do: there is no method that accepts text.
 * That is not an omission to be filled in later.
 */
public interface VentService {

    /**
     * Records that someone pressed release.
     *
     * @param mood optional label, or null if none was chosen
     */
    void recordRelease(Mood mood);

    /** The real number of releases. Never padded or seeded. */
    VentStatsResponse stats();
}
