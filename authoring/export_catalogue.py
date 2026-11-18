#!/usr/bin/env python3
"""Export the fixed bundled definitions with reviewed card artwork."""
import json
from pathlib import Path
repo = Path(__file__).resolve().parent.parent
cards = json.loads((repo / 'authoring/discoveries.json').read_text())['cards']
# Keep the remaining historical order IDs stable so existing item predicates and
# saved discovery identities are not renumbered after retiring Hazards lessons.
assert len(cards) == 45 and sorted(c['order'] for c in cards) == [i for i in range(1, 54) if i not in {2, 6, 7, 21, 27, 28, 29, 39}]
assert len({c['id'] for c in cards}) == 45
assert all(c.get('short_title') and len(c['short_title']) <= 24 for c in cards)
textures = repo / 'src/main/resources/assets/better_discovery_guides/textures/gui/threads'
threads = []
for card in cards:
    row = {k: v for k, v in card.items() if k not in {'scene', 'evidence'}}
    if not (textures / f"{card['id']}.png").is_file():
        raise FileNotFoundError(f"Missing reviewed card art: {card['id']}")
    threads.append(row)
output = {'schema': 'bc.better_discovery_guides.cards.v4', 'threads': threads}
(repo / 'src/main/resources/data/better_discovery_guides/threads/catalogue.json').write_text(json.dumps(output, indent=2) + '\n')
