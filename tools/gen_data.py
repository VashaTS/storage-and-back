#!/usr/bin/env python3
"""Generates blockstates, models, item definitions, loot tables, recipes, recipe advancements, tags and lang files.
Run from the repository root: python3 tools/gen_data.py  (textures: tools/gen_textures.py).
The guide book texture is assets/storageback/textures/item/guide_book.png (not regenerated here)."""
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
            "has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipes": recipe_id}},
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

    # Guide book: shapeless Book + Flint Block. Lang keys are merged below.
    write(f"{A}/models/item/guide_book.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"{NS}:item/guide_book"},
    })
    write(f"{A}/items/guide_book.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/guide_book"}})
    write(f"{D}/recipe/guide_book.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": ["minecraft:book", f"{NS}:flint_block"],
        "result": {"id": f"{NS}:guide_book"},
    })
    write(f"{D}/advancement/recipes/misc/guide_book.json", advancement(f"{NS}:guide_book", f"{NS}:flint_block"))

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

    # fuel: 9 blaze rods (9 x 2400 ticks); charcoal block reuses minecraft:cooking/time_coal_block
    write(f"{D}/context_int_provider/cooking/time_blaze_rod_bundle.json", {
        "type": "minecraft:div", "left": 21600,
        "right": {"type": "minecraft:conditional", "condition": "minecraft:block/fast_cooking",
                  "on_false": "minecraft:cooking/normal_burn_time_reduction_factor",
                  "on_true": "minecraft:cooking/fast_burn_time_reduction_factor"},
    })

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
    en.update({
        "item.storageback.guide_book": "Storage & Back Guide",
        "item.storageback.guide_book.desc": "A guide to the new blocks and recipes",
        "book.storageback.title_heading": "Storage & Back",
        "book.storageback.heading.back": "Back recipes",
        "book.storageback.heading.config": "Config",
        "book.storageback.page.title": "A guide to the new storage blocks, how to craft them, the back (uncrafting) recipes, and the config toggles.\n\nCrafted from 1 Book and 1 Flint Block, shapeless.",
        "book.storageback.page.flint_block": "Crafted from 9 Flint in a 3×3 grid.\nCrafts back into 9 Flint.\n\nA stone-like block.\nNeeds a pickaxe to mine.",
        "book.storageback.page.sugar_cane_bundle": "Crafted from 9 Sugar Cane in a 3×3 grid.\nCrafts back into 9 Sugar Cane.\n\nRotates like a log.\nBurns. Compostable.",
        "book.storageback.page.charcoal_block": "Crafted from 9 Charcoal in a 3×3 grid.\nCrafts back into 9 Charcoal.\n\nFurnace fuel: smelts 80 items, the same as a Block of Coal.\nBurns.",
        "book.storageback.page.gunpowder_block": "Crafted from 9 Gunpowder in a 3×3 grid.\nCrafts back into 9 Gunpowder.\n\nFalls like sand.\nDoes not explode.",
        "book.storageback.page.blaze_rod_bundle": "Crafted from 9 Blaze Rods in a 3×3 grid.\nCrafts back into 9 Blaze Rods.\n\nRotates like a log.\nGives off a little light (level 5).\nFurnace fuel: smelts 108 items.",
        "book.storageback.page.leather_block": "Crafted from 9 Leather in a 3×3 grid.\nCrafts back into 9 Leather.\n\nA soft, wool-like block with stitching.\nBurns.",
        "book.storageback.page.feather_block": "Crafted from 9 Feathers in a 3×3 grid.\nCrafts back into 9 Feathers.\n\nLanding on it deals only 20% fall damage, like a Hay Bale.\nBurns.",
        "book.storageback.page.ender_pearl_block": "Crafted from 9 Ender Pearls in a 3×3 grid; crafts back into 9.\n\nA cluster of 9 touching pearls (8 at the corners, 1 in the centre) that you can see between.\nThey only stack to 16; this stores 9 per slot.",
        "book.storageback.page.rotten_flesh_block": "Crafted from 9 Rotten Flesh in a 3×3 grid.\nCrafts back into 9 Rotten Flesh.\n\nHandy storage for mob farms.\nCompostable.",
        "book.storageback.page.carrot_crate": "Crafted from 9 Carrots in a 3×3 grid.\nCrafts back into 9 Carrots.\n\nA wooden crate. Mine it with an axe.\nBurns. Compostable.",
        "book.storageback.page.potato_crate": "Crafted from 9 Potatoes in a 3×3 grid.\nCrafts back into 9 Potatoes.\n\nA wooden crate. Mine it with an axe.\nBurns. Compostable.",
        "book.storageback.page.beetroot_crate": "Crafted from 9 Beetroots in a 3×3 grid.\nCrafts back into 9 Beetroots.\n\nA wooden crate. Mine it with an axe.\nBurns. Compostable.",
        "book.storageback.page.apple_crate": "Crafted from 9 Apples in a 3×3 grid.\nCrafts back into 9 Apples.\n\nA wooden crate. Mine it with an axe.\nBurns. Compostable.",
        "book.storageback.page.back_intro": "One block, crafted shapeless, gives back the items it is made from.\n\nRecipes marked * can be turned off in the config.\nDark Prismarine loses the black dye.",
        "book.storageback.page.back_1": "(1/5)\nBlock of Quartz →\n4 Nether Quartz\nBlock of Amethyst →\n4 Amethyst Shards *\nClay →\n4 Clay Balls",
        "book.storageback.page.back_2": "(2/5)\nGlowstone →\n4 Glowstone Dust\nSnow Block →\n4 Snowballs\nHoneycomb Block →\n4 Honeycomb",
        "book.storageback.page.back_3": "(3/5)\nBricks →\n4 Bricks\nNether Bricks →\n4 Nether Bricks\nDripstone Block →\n4 Pointed Dripstone",
        "book.storageback.page.config": "Balancing changes (on):\noff = no amethyst or magma recipes.\nBack recipes (on):\noff = no back recipes.\nStorage blocks stay.\n\nEdit storageback.json in config, or Mod Menu, then /reload.",
        "book.storageback.page.back_4": "(4/5)\nPrismarine →\n4 Prismarine Shards\nPrismarine Bricks →\n9 Prismarine Shards\nDark Prismarine →\n8 Prismarine Shards",
        "book.storageback.page.back_5": "(5/5)\nMagma Block →\n4 Magma Cream *\nSandstone →\n4 Sand\nRed Sandstone →\n4 Red Sand",
    })
    pl.update({
        "item.storageback.guide_book": "Przewodnik Storage & Back",
        "item.storageback.guide_book.desc": "Przewodnik po nowych blokach i recepturach",
        "book.storageback.title_heading": "Storage & Back",
        "book.storageback.heading.back": "Receptury odwrotne",
        "book.storageback.heading.config": "Ustawienia",
        "book.storageback.page.title": "Przewodnik po nowych blokach magazynujących, ich wytwarzaniu, recepturach odwrotnych i ustawieniach.\n\nWytwarzanie: 1 Książka i 1 Blok krzemienia, bez układu w siatce.",
        "book.storageback.page.flint_block": "Wytwarzanie: 9 krzemieni w siatce 3×3.\nZ bloku z powrotem: 9 krzemieni.\n\nTwardy, kamienny blok.\nDo wykopania potrzebny jest kilof.",
        "book.storageback.page.sugar_cane_bundle": "Wytwarzanie: 9 trzcin cukrowych w siatce 3×3.\nZ powrotem: 9 trzcin cukrowych.\n\nObraca się jak kłoda.\nPali się. Nadaje się do kompostownika.",
        "book.storageback.page.charcoal_block": "Wytwarzanie: 9 węgla drzewnego w siatce 3×3.\nZ powrotem: 9 węgla drzewnego.\n\nPaliwo do pieca: wystarcza na 80 przedmiotów, tak jak Blok węgla.\nPali się.",
        "book.storageback.page.gunpowder_block": "Wytwarzanie: 9 prochu w siatce 3×3.\nZ powrotem: 9 prochu.\n\nSpada jak piasek.\nNie wybucha.",
        "book.storageback.page.blaze_rod_bundle": "Wytwarzanie: 9 płomiennych różdżek w siatce 3×3; z powrotem też 9.\n\nUstawia się jak kłoda. Słabo świeci (poziom 5).\nPaliwo: wystarcza na 108 przedmiotów.",
        "book.storageback.page.leather_block": "Wytwarzanie: 9 skór w siatce 3×3.\nZ powrotem: 9 skór.\n\nMiękki blok, jak wełna, ze szwem.\nPali się.",
        "book.storageback.page.feather_block": "Wytwarzanie: 9 piór w siatce 3×3.\nZ powrotem: 9 piór.\n\nLądowanie na nim zadaje tylko 20% obrażeń od upadku, jak snop siana.\nPali się.",
        "book.storageback.page.ender_pearl_block": "Wytwarzanie: 9 enderpereł w siatce 3×3; z powrotem też 9.\n\nTo 9 stykających się pereł (8 w rogach, 1 w środku). Widać między nimi.\nEnderperła układa się tylko do 16 sztuk; blok mieści 9.",
        "book.storageback.page.rotten_flesh_block": "Wytwarzanie: 9 zgniłego mięsa w siatce 3×3.\nZ powrotem: 9 zgniłego mięsa.\n\nPrzydatny do łupu z farm potworów.\nNadaje się do kompostownika.",
        "book.storageback.page.carrot_crate": "Wytwarzanie: 9 marchewek w siatce 3×3.\nZ powrotem: 9 marchewek.\n\nDrewniana skrzynka. Wykop ją siekierą.\nPali się. Nadaje się do kompostownika.",
        "book.storageback.page.potato_crate": "Wytwarzanie: 9 ziemniaków w siatce 3×3.\nZ powrotem: 9 ziemniaków.\n\nDrewniana skrzynka. Wykop ją siekierą.\nPali się. Nadaje się do kompostownika.",
        "book.storageback.page.beetroot_crate": "Wytwarzanie: 9 buraków w siatce 3×3.\nZ powrotem: 9 buraków.\n\nDrewniana skrzynka. Wykop ją siekierą.\nPali się. Nadaje się do kompostownika.",
        "book.storageback.page.apple_crate": "Wytwarzanie: 9 jabłek w siatce 3×3.\nZ powrotem: 9 jabłek.\n\nDrewniana skrzynka. Wykop ją siekierą.\nPali się. Nadaje się do kompostownika.",
        "book.storageback.page.back_intro": "Jeden blok, bez układu w siatce, oddaje przedmioty, z których powstał.\n\nReceptury oznaczone * można wyłączyć w ustawieniach.\nCzarny barwnik z ciemnego pryzmarynu przepada.",
        "book.storageback.page.back_1": "(1/5)\nBlok kwarcu →\n4 netherowe kwarce\nBlok ametystu →\n4 odłamki ametystu *\nGlina →\n4 kulki gliny",
        "book.storageback.page.back_2": "(2/5)\nJasnogłaz →\n4 jasnopyły\nBlok śniegu →\n4 śnieżki\nBlok plastra miodu →\n4 plastry miodu",
        "book.storageback.page.back_3": "(3/5)\nCegły →\n4 cegły\nNetherowe cegły →\n4 netherowe cegły\nBlok nacieku →\n4 spiczaste nacieki",
        "book.storageback.page.config": "Zmiany balansu (wł.):\nbez nich znikają receptury ametystu i magmy.\nReceptury odwrotne (wł.):\nusuwa je wszystkie. Bloki zostają.\nZmiana: Mod Menu albo plik w folderze config, potem /reload.",
        "book.storageback.page.back_4": "(4/5)\nPryzmaryn →\n4 odłamki pryzmarynu\nPryzmarynowe cegły →\n9 odłamków pryzmarynu\nCiemny pryzmaryn →\n8 odłamków pryzmarynu",
        "book.storageback.page.back_5": "(5/5)\nBlok magmy →\n4 magmowe kremy *\nPiaskowiec →\n4 piasek\nCzerwony piaskowiec →\n4 czerwony piasek",
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
