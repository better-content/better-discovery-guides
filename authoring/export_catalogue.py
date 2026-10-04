#!/usr/bin/env python3
"""Export the fixed bundled definitions with reviewed card artwork."""
import json
from pathlib import Path
repo = Path(__file__).resolve().parent.parent
cards = json.loads((repo / 'authoring/discoveries.json').read_text())['cards']
assert len(cards) == 52 and sorted(c['order'] for c in cards) == [i for i in range(1, 54) if i != 2]
assert len({c['id'] for c in cards}) == 52
assert all(c.get('short_title') and len(c['short_title']) <= 24 for c in cards)
textures = repo / 'src/main/resources/assets/better_discovery_guides/textures/gui/threads'
threads = []
for card in cards:
    row = {k: v for k, v in card.items() if k not in {'scene', 'evidence'}}
    if not (textures / f"{card['id']}.png").is_file():
        raise FileNotFoundError(f"Missing reviewed card art: {card['id']}")
    threads.append(row)
output = {'schema': 'bc.better_discovery_guides.cards.v3', 'threads': threads}
(repo / 'src/main/resources/data/better_discovery_guides/threads/catalogue.json').write_text(json.dumps(output, indent=2) + '\n')
