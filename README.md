# Better Discovery Guides

Learning Surfaces is Better Content's Forge 1.20.1 player education mod. It owns a 52-card discovery reader, 18 loading lessons, and 199 contextual tips for the main menu, Esc menu, and death screen. Other mods own their gameplay rules and publish events; this mod turns those events and personal points of need into explanations. It grants no gameplay rewards.

## Authoring

[Card definitions](authoring/discoveries.json) are the editable source for the packaged `bc.better_discovery_guides.cards.v3` catalogue. Each card has a full title, an authored short title for the unread prompt, an event, cause, next action, and bounded trigger routes. [Teaching guidance](authoring/teaching-surfaces.md) describes timing, selection, and authority. The modpack's `docs/better_discovery_guides.md` is the cross-surface policy for cards, lessons, tips, hover text, EMI, Ponder, native guides, and HUD feedback.

All 52 active card illustrations and 18 Lesson illustrations use reviewed Journal copperplate masters. The scene specifications remain in the authoring roster. Run `python3 authoring/prepare_art.py /absolute/path/to/review-bundle` to rebuild runtime derivatives, then `python3 authoring/export_catalogue.py` after changing card definitions. Missing masters fail preparation. [Art grammar](authoring/art-grammar.txt) excludes humans, humanoids, body parts, and humanlike silhouettes.

Cards, lessons, and tips load from packaged resources once per process. Resource packs can replace textures, but catalogue text and trigger rules are fixed for a release. Their current schemas are `bc.better_discovery_guides.cards.v3`, `bc.better_discovery_guides.lessons.v1`, and `bc.better_discovery_guides.tips.v1`.

## Evidence and persistence

Owned gameplay outcomes are credited from provider events at success, including when the owner is offline. First-use and inspection cards use bounded personal cues such as item acquisition, closing an EMI recipe, a completed food use, or approaching a feature. The EMI report is client supplied and may award only non-reward personal teaching cards. The server owns outcome credit and persisted lineage history. Repeated signals count once per card per generation. There is no quota or quiet period.

The reader shows discovered cards only, unread first, across seven topics. Facsimiles are cosmetic. A successor starts a new generation score while retaining the archive and read state; ordinary death does not reset it. The persisted state remains at version 1 and the card network protocol is version 2 under the new mod identity. There is no migration from the former mod identity.

## Verification

`./gradlew verifyFast` runs local contracts without launching GameTests. Before committing or pushing, run `./gradlew verifyFull stageRuntimeJar` as required by [AGENTS.md](AGENTS.md). The isolated GameTest profile exercises core discovery behavior. Provider-route scenarios compile under `src/packIntegration` and need the installed providers in a full-pack runtime.
