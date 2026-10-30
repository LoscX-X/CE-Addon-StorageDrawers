import json, pathlib, zipfile, sys

project = pathlib.Path(__file__).resolve().parent.parent
if len(sys.argv) != 2:
    raise SystemExit('Usage: Export-Vanilla-Models.py <Minecraft 26.3 client JAR>')
client = pathlib.Path(sys.argv[1])
with zipfile.ZipFile(client) as archive:
    models = {name.removeprefix('assets/minecraft/models/').removesuffix('.json'): json.loads(archive.read(name)) for name in archive.namelist() if name.startswith('assets/minecraft/models/block/') and name.endswith('.json')}
    states = {name.removeprefix('assets/minecraft/blockstates/').removesuffix('.json'): json.loads(archive.read(name)) for name in archive.namelist() if name.startswith('assets/minecraft/blockstates/') and name.endswith('.json')}
target = project / 'build/vanilla-models.json'
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text(json.dumps({'models': models, 'states': states}), encoding='utf8')
