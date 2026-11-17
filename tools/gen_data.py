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
        "book.storageback.intro": "The new storage blocks, the back recipes, and the config toggles.",
        "book.storageback.heading.index": "Contents",
        "book.storageback.heading.back": "Back recipes",
        "book.storageback.heading.config": "Config",
        "book.storageback.heading.crafting": "Crafting",
        "book.storageback.heading.uncrafting": "Uncrafting",
        "book.storageback.heading.shapeless": "Shapeless",
        "book.storageback.category.storage": "Storage blocks",
        "book.storageback.category.back": "Back recipes",
        "book.storageback.category.config": "Config",
        "book.storageback.disabled": "Currently disabled in the config.",
        "book.storageback.balancing": "Balancing recipe",
        "book.storageback.see_also": "See also:",
        "book.storageback.link.guide_recipe": "Guide book recipe",
        "book.storageback.nav.index": "Contents",
        "book.storageback.nav.back": "Back",
        "book.storageback.status.on": "Currently enabled.",
        "book.storageback.status.off": "Currently disabled.",
        "book.storageback.page.flint_block": "A stone-like block. Needs a pickaxe to mine.\n\nUsed with a Book to craft this guide.",
        "book.storageback.page.sugar_cane_bundle": "Rotates like a log.\nBurns. Compostable.",
        "book.storageback.page.charcoal_block": "Furnace fuel: smelts 80 items, the same as a Block of Coal.\nBurns.",
        "book.storageback.page.gunpowder_block": "Falls like sand.\nDoes not explode.",
        "book.storageback.page.blaze_rod_bundle": "Rotates like a log.\nGives off a little light (level 5).\nFurnace fuel: smelts 108 items.",
        "book.storageback.page.leather_block": "A soft, wool-like block with stitching.\nBurns.",
        "book.storageback.page.feather_block": "Landing on it deals only 20% fall damage, like a Hay Bale.\nBurns.",
        "book.storageback.page.ender_pearl_block": "A cluster of 9 touching pearls (8 at the corners, 1 in the centre) that you can see between.\n\nEnder Pearls only stack to 16; this stores 9 per slot.",
        "book.storageback.page.rotten_flesh_block": "Handy storage for mob farms.\nCompostable.",
        "book.storageback.page.carrot_crate": "A wooden crate. Mine it with an axe.\nBurns. Compostable.",
        "book.storageback.page.potato_crate": "A wooden crate. Mine it with an axe.\nBurns. Compostable.",
        "book.storageback.page.beetroot_crate": "A wooden crate. Mine it with an axe.\nBurns. Compostable.",
        "book.storageback.page.apple_crate": "A wooden crate. Mine it with an axe.\nBurns. Compostable.",
        "book.storageback.page.back.intro": "Recipes marked * can be disabled.",
        "book.storageback.page.back.quartz": "Gives 4 Nether Quartz.",
        "book.storageback.page.back.amethyst": "Gives 4 Amethyst Shards.\n\nThis is a balancing recipe and can be turned off in the config.",
        "book.storageback.page.back.clay": "Gives 4 Clay Balls.",
        "book.storageback.page.back.glowstone": "Gives 4 Glowstone Dust.",
        "book.storageback.page.back.snow": "Gives 4 Snowballs.",
        "book.storageback.page.back.honeycomb": "Gives 4 Honeycomb.",
        "book.storageback.page.back.bricks": "Gives 4 Bricks.",
        "book.storageback.page.back.nether_bricks": "Gives 4 Nether Bricks.",
        "book.storageback.page.back.dripstone": "Gives 4 Pointed Dripstone.",
        "book.storageback.page.back.prismarine": "Gives 4 Prismarine Shards.",
        "book.storageback.page.back.prismarine_bricks": "Gives 9 Prismarine Shards.",
        "book.storageback.page.back.dark_prismarine": "Gives 8 Prismarine Shards. The black dye is lost.",
        "book.storageback.page.back.magma": "Gives 4 Magma Cream.\n\nThis is a balancing recipe and can be turned off in the config.",
        "book.storageback.page.back.sandstone": "Gives 4 Sand.",
        "book.storageback.page.back.red_sandstone": "Gives 4 Red Sand.",
        "book.storageback.page.config.balancing": "Balancing changes (on by default): Amethyst Block → 4 Amethyst Shards, and Magma Block → 4 Magma Cream. Turn this off to remove only those two.",
        "book.storageback.page.config.back": "On by default. Every recipe that turns a vanilla block back into its items, including the two balancing ones.",
        "book.storageback.page.config.how": "Edit storageback.json in config or Mod Menu, then /reload.",
    })
    pl.update({
        "item.storageback.guide_book": "Przewodnik Storage & Back",
        "item.storageback.guide_book.desc": "Przewodnik po nowych blokach i recepturach",
        "book.storageback.title_heading": "Storage & Back",
        "book.storageback.intro": "Nowe bloki magazynowe, receptury odwrotne i ustawienia.",
        "book.storageback.heading.index": "Spis treści",
        "book.storageback.heading.back": "Receptury odwrotne",
        "book.storageback.heading.config": "Ustawienia",
        "book.storageback.heading.crafting": "Wytwarzanie",
        "book.storageback.heading.uncrafting": "Rozbieranie",
        "book.storageback.heading.shapeless": "Bez układu",
        "book.storageback.category.storage": "Bloki magazynowe",
        "book.storageback.category.back": "Receptury odwrotne",
        "book.storageback.category.config": "Ustawienia",
        "book.storageback.disabled": "Obecnie wyłączone w ustawieniach.",
        "book.storageback.balancing": "Receptura balansująca",
        "book.storageback.see_also": "Zobacz też:",
        "book.storageback.link.guide_recipe": "Receptura przewodnika",
        "book.storageback.nav.index": "Spis treści",
        "book.storageback.nav.back": "Wstecz",
        "book.storageback.status.on": "Obecnie włączone.",
        "book.storageback.status.off": "Obecnie wyłączone.",
        "book.storageback.page.flint_block": "Kamienny blok. Do wykopania potrzebny jest kilof. Z książką służy do wytworzenia tego przewodnika.",
        "book.storageback.page.sugar_cane_bundle": "Obraca się jak kłoda.\nPali się. Nadaje się do kompostownika.",
        "book.storageback.page.charcoal_block": "Paliwo do pieca: wystarcza na 80 przedmiotów, tak jak Blok węgla.\nPali się.",
        "book.storageback.page.gunpowder_block": "Spada jak piasek.\nNie wybucha.",
        "book.storageback.page.blaze_rod_bundle": "Ustawia się jak kłoda. Słabo świeci (poziom 5).\nPaliwo: wystarcza na 108 przedmiotów.",
        "book.storageback.page.leather_block": "Miękki blok, jak wełna, ze szwem.\nPali się.",
        "book.storageback.page.feather_block": "Lądowanie na nim zadaje tylko 20% obrażeń od upadku, jak snop siana.\nPali się.",
        "book.storageback.page.ender_pearl_block": "To 9 stykających się pereł (8 w rogach, 1 w środku). Widać między nimi.\n\nEnderperła układa się tylko do 16 sztuk; blok mieści 9.",
        "book.storageback.page.rotten_flesh_block": "Przydatny do łupu z farm potworów.\nNadaje się do kompostownika.",
        "book.storageback.page.carrot_crate": "Drewniana skrzynka. Wykop ją siekierą.\nPali się. Nadaje się do kompostownika.",
        "book.storageback.page.potato_crate": "Drewniana skrzynka. Wykop ją siekierą.\nPali się. Nadaje się do kompostownika.",
        "book.storageback.page.beetroot_crate": "Drewniana skrzynka. Wykop ją siekierą.\nPali się. Nadaje się do kompostownika.",
        "book.storageback.page.apple_crate": "Drewniana skrzynka. Wykop ją siekierą.\nPali się. Nadaje się do kompostownika.",
        "book.storageback.page.back.intro": "Receptury z * można wyłączyć.",
        "book.storageback.page.back.quartz": "Daje 4 netherowe kwarce.",
        "book.storageback.page.back.amethyst": "Daje 4 odłamki ametystu.\n\nTo receptura balansująca; można ją wyłączyć w ustawieniach.",
        "book.storageback.page.back.clay": "Daje 4 kulki gliny.",
        "book.storageback.page.back.glowstone": "Daje 4 jasnopyły.",
        "book.storageback.page.back.snow": "Daje 4 śnieżki.",
        "book.storageback.page.back.honeycomb": "Daje 4 plastry miodu.",
        "book.storageback.page.back.bricks": "Daje 4 cegły.",
        "book.storageback.page.back.nether_bricks": "Daje 4 netherowe cegły.",
        "book.storageback.page.back.dripstone": "Daje 4 spiczaste nacieki.",
        "book.storageback.page.back.prismarine": "Daje 4 odłamki pryzmarynu.",
        "book.storageback.page.back.prismarine_bricks": "Daje 9 odłamków pryzmarynu.",
        "book.storageback.page.back.dark_prismarine": "Daje 8 odłamków pryzmarynu. Czarny barwnik przepada.",
        "book.storageback.page.back.magma": "Daje 4 magmowe kremy.\n\nTo receptura balansująca; można ją wyłączyć w ustawieniach.",
        "book.storageback.page.back.sandstone": "Daje 4 piasek.",
        "book.storageback.page.back.red_sandstone": "Daje 4 czerwony piasek.",
        "book.storageback.page.config.balancing": "Zmiany balansu (domyślnie włączone): Blok ametystu → 4 odłamki ametystu i Blok magmy → 4 magmowe kremy. Wyłączenie usuwa tylko te dwie.",
        "book.storageback.page.config.back": "Domyślnie włączone. Wszystkie receptury zamieniające blok z gry z powrotem w przedmioty, w tym obie balansujące.",
        "book.storageback.page.config.how": "Edytuj storageback.json w config albo Mod Menu, potem /reload.",
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
