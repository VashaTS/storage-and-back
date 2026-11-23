#!/usr/bin/env python3
"""Convert the 26.x resources of Storage & Back to the 1.21.1 or 1.20.1 format. Run from src/main/resources: backport_resources.py 1.21.1|1.20.1"""
import glob, json, os, shutil, sys

target = sys.argv[1]
legacy = target.startswith("1.20")

def dump(p, d):
    with open(p, "w") as f:
        json.dump(d, f, indent=2)
        f.write("\n")

def ing(v):
    if isinstance(v, list):
        return [ing(x) for x in v]
    if isinstance(v, str):
        return {"tag": v[1:]} if v.startswith("#") else {"item": v}
    return v

# recipes: plain id strings -> {"item": ...}; 1.20.1 results use "item" instead of "id"
for p in glob.glob("data/storageback/recipe/*.json"):
    d = json.load(open(p))
    if "key" in d:
        d["key"] = {k: ing(v) for k, v in d["key"].items()}
    if "ingredients" in d:
        d["ingredients"] = [ing(v) for v in d["ingredients"]]
    if legacy and isinstance(d.get("result"), dict) and "id" in d["result"]:
        d["result"]["item"] = d["result"].pop("id")
    dump(p, d)

# advancements: 1.20.1 item predicates need "items" as a list
def fix_adv(o):
    if isinstance(o, dict):
        if "items" in o and isinstance(o["items"], str):
            o["items"] = [o["items"]]
        for v in o.values():
            fix_adv(v)
    elif isinstance(o, list):
        for v in o:
            fix_adv(v)
if legacy:
    for p in glob.glob("data/storageback/advancement/**/*.json", recursive=True):
        d = json.load(open(p))
        fix_adv(d.get("criteria"))
        dump(p, d)
    for p in glob.glob("data/storageback/loot_table/**/*.json", recursive=True):
        d = json.load(open(p))
        d.pop("random_sequence", None)
        dump(p, d)

# item model definitions (assets/<ns>/items) only exist from 1.21.4: block items get models/item/<block>.json
for p in glob.glob("assets/storageback/items/*.json"):
    name = os.path.basename(p)[:-5]
    model = json.load(open(p))["model"]["model"]
    if not os.path.exists(f"assets/storageback/models/item/{name}.json"):
        dump(f"assets/storageback/models/item/{name}.json", {"parent": model})
shutil.rmtree("assets/storageback/items")

if legacy:
    for ns in os.listdir("data"):
        for a, b in (("recipe", "recipes"), ("advancement", "advancements"), ("loot_table", "loot_tables")):
            if os.path.isdir(f"data/{ns}/{a}"):
                shutil.move(f"data/{ns}/{a}", f"data/{ns}/{b}")
        for a, b in (("item", "items"), ("block", "blocks")):
            if os.path.isdir(f"data/{ns}/tags/{a}"):
                shutil.move(f"data/{ns}/tags/{a}", f"data/{ns}/tags/{b}")
print("converted for", target)
