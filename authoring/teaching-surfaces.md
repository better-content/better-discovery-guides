# Teaching surfaces and authoritative advice

Learning Surfaces explains a personal event or point of need, its cause, and a useful response. Contextual cues such as first recipe inspection, first cave-pressure rise, and Font approach can teach before the player commits to a risky operation. It does not use these advice snapshots as discovery evidence. The resource `assets/better_discovery_guides/death_hints/catalogue.json` uses `bc.teaching_hints.v2`; each entry explicitly declares its eligible `surfaces`, optional context `requirements`, mechanical `sources`, and required mods.

## Selection and exposure

- The main menu chooses one mixed preparation/discovery tip per application launch. It remains stable across world visits, death, reconnect, and resizing. Teasers are eligible only here.
- Esc selects practical advice on opening. Its text remains still while open. Reopening with the same context retains the tip; a changed context selects again. A heartbeat older than five seconds becomes general advice on the next opening.
- Death uses the committed final source. With the injury mod present, only `InjuryEvent.FinalDeath` supplies that source; Forge's earlier cancellable notification cannot override it. Death tips remain fixed for reading and are deduplicated until respawn/logout.
- Each surface has its own local exposure history (`better-discovery-guides-{menu,pause,death}-hints.json`). Selection does not consume a tip. Only successfully rendered copy records exposure. Matching current evidence wins over novelty.

The server refreshes Esc context once per second. Optional mod APIs are isolated behind loaded-mod guards; absent data never implies that a condition happened. Priority is Death's Door, active fire/low air/thirst/body thermal stress/hunger/low HP, injury care, harmful effects and metabolism, held frozen food, recent accepted damage, then the player's actual campaign phase.

Injury care follows native snapshots and treatment tags: healing lock; available healing at Death's Door; an inventory cure matching an active injury; trauma-amplified functional injuries; otherwise untreated injuries. The cure scan includes the entire native inventory. Metabolism uses the owning presentation flags and supported-nutrition threshold. Thermal stress uses Heat Sync's authoritative body-stress boundary (75), never biome temperature. Campaign preparation, attackers already materialized, and recovery have separate copy. Inventory ownership never claims machine operation.

## Death's Door and Body

Death's Door has no active discovery card. The native Body UI, loading/Lessons and contextual advice explain the state, persistent injuries, trauma, and treatment without spending discovery slots. Final-death advice still follows the committed native event.

`DeathRecapOverlay` owns death-button placement. Learning Surfaces reserves its measured tip area through that provider; the recap uses the remaining area. Compact layouts show all six regions and paired active/treated counts in the shared Cracked/Burnt/Opened order. If copy cannot fit while preserving the native recap and controls, it is omitted and not recorded as seen. Notices pause while Death's Door is urgent or Body is open, preserving their remaining reading lifetime.

The 18 loading/Lessons definitions include separate Death's Door and treatment/trauma fundamentals. There are no bleed-out, give-up, hunger-powered rescue, or helper-stacking instructions. Loading exposure behavior and voluntary Lessons access remain independent of Thread discovery.

## Local verification

`HintLifecycleTest` covers menu stability, independent histories, context changes between Esc openings, relevance over novelty, and final-death deduplication. `DeathHintsTest` checks manifest constraints, absent evidence/mods, final-source attribution, stale context, and persisted exposure.

The isolated learning visual source set also includes `CombinedDeathVisualReview`. Set `-Dbc.learningVisual.combinedDeathOnly=true` for three compact/wide captures using the native `DeathScreen` renderer and both production overlays. The fixture supplies an injury recap and inert controls without creating a world. Pass `-PcombinedDeathVisual=true` to add the deobfuscated local injury runtime to that visual client's classpath. It proves layout and rendering, not gameplay death or treatment behavior; those remain native provider GameTests.
