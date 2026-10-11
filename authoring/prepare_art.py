#!/usr/bin/env python3
"""Build packaged derivatives from reviewed imagegen masters. No image generation here."""
import argparse
import json
import subprocess
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('review_bundle', type=Path, nargs='?',
                    help='optional active-task review input; defaults to canonical authored masters')
parser.add_argument('--check', action='store_true', help='validate authored inputs without writing derivatives')
args = parser.parse_args()
repo = Path(__file__).resolve().parent.parent
assets = repo / 'src/main/resources/assets/better_discovery_guides'
cards = json.loads((repo / 'authoring/discoveries.json').read_text())['cards']
briefs = json.loads((assets / 'loading_briefs/catalogue.json').read_text())['briefs']

def convert(source, target, *options):
    if args.check:
        with source.open('rb') as image:
            if image.read(8) != b'\x89PNG\r\n\x1a\n':
                raise ValueError(f'Not a PNG source master: {source}')
        return
    target.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(['gm', 'convert', str(source), *options, '-strip', str(target)], check=True)

for card in cards:
    name = card['id']
    source = (args.review_bundle / 'masters' / (name + '.png')) if args.review_bundle else repo / 'authoring/masters' / (name + '.png')
    if not source.is_file():
        source = repo / 'authoring/masters' / (name + '.png')
    target = assets / 'textures/gui/threads' / (name + '.png')
    if not source.is_file():
        raise FileNotFoundError(f'No reviewed card master for {name}')
    convert(source, target, '-resize', '256x384!')
    convert(source, target.with_name(name + '_thumb.png'), '-resize', '256x384!', '-colorspace', 'GRAY')
    convert(source, assets / 'textures/item/thread_cards' / (name + '.png'), '-resize', '256x384!')
    layer = 'better_discovery_guides:item/thread_cards/' + name
    model = {'parent': 'minecraft:item/generated', 'textures': {'layer0': layer}}
    if not args.check:
        (assets / 'models/item/thread_cards' / (name + '.json')).write_text(json.dumps(model, indent=2) + '\n')
model = {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'minecraft:item/paper', 'particle': 'minecraft:item/paper'},
         'overrides': [{'predicate': {'better_discovery_guides:thread_index': c['order']},
                        'model': 'better_discovery_guides:item/thread_cards/' + c['id']} for c in cards]}
if not args.check:
    (assets / 'models/item/thread_facsimile.json').write_text(json.dumps(model, indent=2) + '\n')
for brief in briefs:
    source = (args.review_bundle / 'lessons' / (brief['id'] + '.png')) if args.review_bundle else repo / 'authoring/lessons' / (brief['id'] + '.png')
    if not source.is_file():
        source = repo / 'authoring/lessons' / (brief['id'] + '.png')
    if not source.is_file():
        raise FileNotFoundError(f'No reviewed lesson master for {brief["id"]}')
    convert(source, assets / 'textures/gui/loading_briefs' / source.name, '-resize', '512x256!')
print(f'{"Checked authored inputs" if args.check else "Prepared reviewed art and models"} for {len(cards)} cards and {len(briefs)} lessons.')
