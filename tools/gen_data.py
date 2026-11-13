#!/usr/bin/env python3
"""Generates blockstates, models, item definitions, loot tables, recipes, recipe advancements, tags and lang files.
Run from the repository root: python3 tools/gen_data.py  (textures: tools/gen_textures.py)"""
import json, os, shutil

NS = "storageback"
RES = "src/main/resources"
A = f"{RES}/assets/{NS}"
D = f"{RES}/data/{NS}"

# id, ingredient item, model kind (cube | column), tool tag, EN name, PL name
BLOCKS = [
    ("flint_block", "flint", "cube", "pickaxe", "Flint Block", "Blok krzemienia"),
    ("sugar_cane_bundle", "sugar_cane", "pillar", "hoe", "Sugar Cane Bundle", "Snopek trzciny cukrowej"),
    ("charcoal_block", "charcoal", "cube", "pickaxe", "Charcoal Block", "Blok węgla drzewnego"),
    ("gunpowder_block", "gunpowder", "cube", "shovel", "Gunpowder Block", "Blok prochu"),
    ("blaze_rod_bundle", "blaze_rod", "pillar", "pickaxe", "Blaze Rod Bundle", "Wiązka płomiennych różdżek"),
    ("leather_block", "leather", "cube", "hoe", "Leather Block", "Blok skóry"),
    ("feather_block", "feather", "cube", "hoe", "Feather Block", "Blok piór"),
    ("ender_pearl_block", "ender_pearl", "pearls", "pickaxe", "Ender Pearl Block", "Blok pereł Kresu"),
    ("rotten_flesh_block", "rotten_flesh", "cube", "hoe", "Rotten Flesh Block", "Blok zgniłego mięsa"),
    ("carrot_crate", "carrot", "column", "axe", "Carrot Crate", "Skrzynka marchewek"),
    ("potato_crate", "potato", "column", "axe", "Potato Crate", "Skrzynka ziemniaków"),
    ("beetroot_crate", "beetroot", "column", "axe", "Beetroot Crate", "Skrzynka buraków"),
    ("apple_crate", "apple", "column", "axe", "Apple Crate", "Skrzynka jabłek"),
]

# vanilla block, result item, count, recipe-book category folder, extra condition option
BACK = [
    ("quartz_block", "quartz", 4, None),
    ("amethyst_block", "amethyst_shard", 4, "balancing_changes"),
    ("clay", "clay_ball", 4, None),
    ("glowstone", "glowstone_dust", 4, None),
    ("snow_block", "snowball", 4, None),
    ("honeycomb_block", "honeycomb", 4, None),
    ("bricks", "brick", 4, None),
    ("nether_bricks", "nether_brick", 4, None),
    ("dripstone_block", "pointed_dripstone", 4, None),
    ("prismarine", "prismarine_shard", 4, None),
    ("prismarine_bricks", "prismarine_shard", 9, None),
    ("dark_prismarine", "prismarine_shard", 8, None),
    ("magma_block", "magma_cream", 4, "balancing_changes"),
    ("sandstone", "sand", 4, None),
    ("red_sandstone", "red_sand", 4, None),
]


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def advancement(recipe_id, item, conditions=None):
    adv = {
        "parent": "minecraft:recipes/root",
        "criteria": {
            "has_item": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": item}]}},
            "has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": recipe_id}},  # 26.3: "recipes"
        },
        "requirements": [["has_the_recipe", "has_item"]],
        "rewards": {"recipes": [recipe_id]},
    }
    if conditions:
        adv = {"fabric:load_conditions": conditions, **adv}
    return adv


def main():
    for d in ("blockstates", "models", "items"):
        shutil.rmtree(f"{A}/{d}", ignore_errors=True)
    for d in ("recipe", "advancement", "loot_table"):
        shutil.rmtree(f"{D}/{d}", ignore_errors=True)

    en = {"itemGroup.storageback.storageback": "Storage & Back"}
    pl = {"itemGroup.storageback.storageback": "Storage & Back"}
    tools = {}
    storage_tag = []

    for bid, ing, kind, tool, en_name, pl_name in BLOCKS:
        full = f"{NS}:{bid}"
        en[f"block.{NS}.{bid}"] = en_name
        pl[f"block.{NS}.{bid}"] = pl_name
        tools.setdefault(tool, []).append(full)
        storage_tag.append(full)

        # blockstate + models
        if kind == "pillar":
            write(f"{A}/blockstates/{bid}.json", {"variants": {
                "axis=x": {"model": f"{NS}:block/{bid}_horizontal", "x": 90, "y": 90},
                "axis=y": {"model": f"{NS}:block/{bid}"},
                "axis=z": {"model": f"{NS}:block/{bid}_horizontal", "x": 90},
            }})
            tex = {"end": f"{NS}:block/{bid}_top", "side": f"{NS}:block/{bid}_side"}
            write(f"{A}/models/block/{bid}.json", {"parent": "minecraft:block/cube_column", "textures": tex})
            write(f"{A}/models/block/{bid}_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal", "textures": tex})
        elif kind == "column":
            write(f"{A}/blockstates/{bid}.json", {"variants": {"": {"model": f"{NS}:block/{bid}"}}})
            write(f"{A}/models/block/{bid}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
                "top": f"{NS}:block/{bid}_top", "side": f"{NS}:block/{bid}_side", "bottom": f"{NS}:block/crate_bottom"}})
        elif kind == "pearls":
            write(f"{A}/blockstates/{bid}.json", {"variants": {"": {"model": f"{NS}:block/{bid}"}}})
            # 9-sphere model + texture: tools/gen_pearl_model.py (must run after this rmtree)
        else:
            write(f"{A}/blockstates/{bid}.json", {"variants": {"": {"model": f"{NS}:block/{bid}"}}})
            write(f"{A}/models/block/{bid}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{bid}"}})
        write(f"{A}/items/{bid}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:block/{bid}"}})

        # loot table: drops itself (explosions may destroy it, like vanilla storage blocks)
        write(f"{D}/loot_table/blocks/{bid}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
                       "entries": [{"type": "minecraft:item", "name": full}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"{NS}:blocks/{bid}",
        })

        # recipes: 9 items -> block (shaped 3x3), block -> 9 items
        write(f"{D}/recipe/{bid}.json", {
            "type": "minecraft:crafting_shaped", "category": "building",
            "key": {"#": f"minecraft:{ing}"}, "pattern": ["###", "###", "###"],
            "result": {"id": full},
        })
        unpack = f"{ing}_from_{bid}"
        write(f"{D}/recipe/{unpack}.json", {
            "type": "minecraft:crafting_shapeless", "category": "misc",
            "ingredients": [full], "result": {"id": f"minecraft:{ing}", "count": 9},
        })
        write(f"{D}/advancement/recipes/building_blocks/{bid}.json", advancement(f"{NS}:{bid}", f"minecraft:{ing}"))
        write(f"{D}/advancement/recipes/misc/{unpack}.json", advancement(f"{NS}:{unpack}", full))

    for block, item, count, extra in BACK:
        rid = f"{item}_from_{block}"
        conds = [{"condition": f"{NS}:config", "option": "back_recipes"}]
        if extra:
            conds.append({"condition": f"{NS}:config", "option": extra})
        write(f"{D}/recipe/{rid}.json", {
            "fabric:load_conditions": conds,
            "type": "minecraft:crafting_shapeless", "category": "misc",
            "ingredients": [f"minecraft:{block}"], "result": {"id": f"minecraft:{item}", "count": count},
        })
        write(f"{D}/advancement/recipes/misc/{rid}.json", advancement(f"{NS}:{rid}", f"minecraft:{block}", conds))

    # tags
    mc = f"{RES}/data/minecraft/tags/block"
    for tool, ids in tools.items():
        write(f"{mc}/mineable/{tool}.json", {"replace": False, "values": ids})
    c_block = f"{RES}/data/c/tags/block"
    c_item = f"{RES}/data/c/tags/item"
    for base in (c_block, c_item):
        shutil.rmtree(f"{base}/storage_blocks", ignore_errors=True)
        write(f"{base}/storage_blocks.json", {"replace": False, "values": [f"#c:storage_blocks/{b[1]}" for b in BLOCKS]})
        for bid, ing, *_ in BLOCKS:
            write(f"{base}/storage_blocks/{ing}.json", {"replace": False, "values": [f"{NS}:{bid}"]})

    # 26.2: fuel and composting values are registered in code (ModBlocks), not data
    shutil.rmtree(f"{D}/context_int_provider", ignore_errors=True)

    en.update({
        "config.storageback.title": "Storage & Back",
        "config.storageback.category.recipes": "Recipes",
        "config.storageback.balancing_changes": "Balancing changes",
        "config.storageback.balancing_changes.tooltip": "Recipes that change vanilla balance: Amethyst Block → 4 Amethyst Shards and Magma Block → 4 Magma Cream.",
        "config.storageback.back_recipes": "Back recipes",
        "config.storageback.back_recipes.tooltip": "All recipes that turn vanilla blocks back into their items (Quartz Block → 4 Nether Quartz, Bricks → 4 Bricks, ...).",
        "config.storageback.reload_note": "Recipes are server data: applies after /reload or rejoining the world. On a server, edit config/storageback.json there.",
        "modmenu.descriptionTranslation.storageback": "Storage blocks for flint, sugar cane, charcoal, gunpowder, blaze rods, leather, feathers, ender pearls, rotten flesh and crops, plus recipes that turn vanilla blocks back into their items.",
    })
    pl.update({
        "config.storageback.title": "Storage & Back",
        "config.storageback.category.recipes": "Receptury",
        "config.storageback.balancing_changes": "Zmiany balansu",
        "config.storageback.balancing_changes.tooltip": "Receptury zmieniające balans gry: Blok ametystu → 4 odłamki ametystu i Blok magmy → 4 kremy magmowe.",
        "config.storageback.back_recipes": "Receptury odwrotne",
        "config.storageback.back_recipes.tooltip": "Wszystkie receptury zamieniające bloki z gry z powrotem w przedmioty (Blok kwarcu → 4 kwarce z Netheru, Cegły → 4 cegły, ...).",
        "config.storageback.reload_note": "Receptury to dane serwera: zmiana działa po /reload lub ponownym wejściu do świata. Na serwerze edytuj tam plik config/storageback.json.",
        "modmenu.descriptionTranslation.storageback": "Bloki do przechowywania krzemienia, trzciny cukrowej, węgla drzewnego, prochu, płomiennych różdżek, skóry, piór, pereł Kresu, zgniłego mięsa i warzyw oraz receptury zamieniające bloki z gry z powrotem w przedmioty.",
    })
    # Custom 3D model for the Ender Pearl Block (deleted by the models rmtree above).
    import importlib.util, pathlib
    pearl_path = pathlib.Path(__file__).with_name("gen_pearl_model.py")
    spec = importlib.util.spec_from_file_location("gen_pearl_model", pearl_path)
    pearl = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(pearl)
    pearl.write_model()
    pearl.write_texture()

    write(f"{A}/lang/en_us.json", en)
    write(f"{A}/lang/pl_pl.json", pl)


if __name__ == "__main__":
    main()
