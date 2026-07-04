package com.garden.flowerid

object WeedDatabase {

    data class WeedInfo(
        val commonName: String,
        val reason: String,
        val removal: String
    )

    // Keyed by lowercase genus name
    private val weeds = mapOf(
        "taraxacum" to WeedInfo(
            "Dandelion",
            "Invasive taproot, spreads rapidly via wind-blown seeds, competes aggressively with lawn grass",
            "Dig out the entire taproot with a narrow weeding knife or dandelion fork, ideally after rain when the soil is soft. Remove flower heads before they turn to seed. For lawns, spot-treat with a selective broadleaf weedkiller (e.g. 2,4-D / dicamba)."
        ),
        "cirsium" to WeedInfo(
            "Thistle",
            "Deep taproots and spiny rosettes crowd out grass, spreads via wind",
            "Wear thick gloves. Dig out the full taproot; repeated cutting at the base slowly starves it. Never let it flower — remove seed heads. Spot-treat regrowth with a selective broadleaf weedkiller."
        ),
        "convolvulus" to WeedInfo(
            "Bindweed",
            "Extremely invasive climber, strangles other plants, nearly impossible to fully eradicate",
            "It won't pull out fully — dig out as much of the white root as you can. Let stems climb a cane, then paint the leaves with a glyphosate-based systemic weedkiller so it travels to the roots. Repeat through the season; mulch to suppress regrowth."
        ),
        "calystegia" to WeedInfo(
            "Hedge Bindweed",
            "Fast-spreading climber, rhizomes spread underground",
            "Trace and dig out the underground rhizomes, or let it climb a cane and treat the foliage with glyphosate. Mulch heavily and pull any regrowth promptly."
        ),
        "aegopodium" to WeedInfo(
            "Ground Elder",
            "Extremely invasive, underground rhizomes spread rapidly and suppress all other plants",
            "Dig out every scrap of white rhizome — any fragment left behind regrows. Smother cleared ground with cardboard or black plastic for a full season, or treat the leafy growth with glyphosate. Persistence is essential."
        ),
        "stellaria" to WeedInfo(
            "Chickweed",
            "Rapid grower that smothers lawn grass, thrives in cool weather",
            "Hoe or hand-pull before it sets seed — the roots are shallow and lift easily. Mulch bare soil to block seedlings. Rarely needs weedkiller."
        ),
        "plantago" to WeedInfo(
            "Plantain Weed",
            "Flat rosettes smother grass, very drought-resistant once established",
            "Lever out the whole rosette and taproot with a hand fork. For lawns, spot-treat with a selective broadleaf weedkiller. Keep the lawn thick to stop it returning."
        ),
        "rumex" to WeedInfo(
            "Dock",
            "Deep taproot regrows from fragments — removing it often makes it worse",
            "Dig out the entire long taproot — any piece left behind resprouts. Remove seed heads promptly. Spot-treat stubborn regrowth with a systemic (glyphosate) or selective broadleaf weedkiller."
        ),
        "ambrosia" to WeedInfo(
            "Ragweed",
            "Causes severe allergies, aggressive annual that self-seeds prolifically",
            "Pull or hoe young plants (wear gloves and ideally a mask — the pollen is highly allergenic). Remove every plant before it flowers. Mulch bare soil to prevent seedlings."
        ),
        "digitaria" to WeedInfo(
            "Crabgrass",
            "Invasive annual grass that dominates lawns in summer heat",
            "Pull clumps before they seed. Keep the lawn thick and mow high to shade it out. Apply a pre-emergent crabgrass preventer in early spring before soil warms."
        ),
        "cyperus" to WeedInfo(
            "Nutsedge",
            "Grass-like weed spreading via underground tubers, resistant to most herbicides",
            "Pull young plants weekly before the underground tubers (\"nutlets\") form — persistence starves the roots. It shrugs off most weedkillers; for heavy infestations use a sedge-specific herbicide (e.g. halosulfuron)."
        ),
        "euphorbia" to WeedInfo(
            "Spurge",
            "Spreads very quickly, produces toxic milky sap harmful to other plants",
            "Wear gloves — the milky sap irritates skin and eyes. Hand-pull young plants with the root and hoe seedlings before they seed. Mulch bare soil."
        ),
        "chenopodium" to WeedInfo(
            "Fat Hen / Goosefoot",
            "One plant produces up to 75,000 seeds — prolific annual weed",
            "Hoe or hand-pull before it flowers — stopping the seed is everything. Mulch bare ground. Easy to control if caught early."
        ),
        "reynoutria" to WeedInfo(
            "Japanese Knotweed",
            "EXTREMELY invasive — legally notifiable in many countries, can damage building foundations",
            "Do NOT dig, strim or compost it — fragments spread it and disposal is legally controlled as hazardous waste. This needs professional treatment (repeated glyphosate stem-injection over several years). Check and follow your local reporting rules."
        ),
        "fallopia" to WeedInfo(
            "Japanese Knotweed",
            "EXTREMELY invasive — legally notifiable in many countries, can damage building foundations",
            "Do NOT dig, strim or compost it — fragments spread it and disposal is legally controlled as hazardous waste. This needs professional treatment (repeated glyphosate stem-injection over several years). Check and follow your local reporting rules."
        ),
        "polygonum" to WeedInfo(
            "Knotweed",
            "Fast-spreading invasive, very difficult to control once established",
            "Hoe or pull young plants; dig established roots out fully. It's persistent — repeat, and treat any regrowth with a systemic (glyphosate) weedkiller."
        ),
        "persicaria" to WeedInfo(
            "Redshank / Knotweed",
            "Aggressive annual, self-seeds freely in moist areas",
            "Hoe or hand-pull before it seeds. It loves damp ground, so improving drainage helps. Mulch bare soil."
        ),
        "oxalis" to WeedInfo(
            "Wood Sorrel",
            "Spreads by both seeds and underground bulbils, very hard to fully remove",
            "Dig out carefully with every tiny bulbil — sieve the soil, as any left behind regrow. Mulch cleared areas and spot-treat regrowth with glyphosate. Very persistent, so keep at it."
        ),
        "veronica" to WeedInfo(
            "Speedwell",
            "Creeping mat that smothers fine lawn grass, spreads by stems and seeds",
            "Rake up the creeping stems and hand-pull; scarify lawns to lift the mats. It resists most selective weedkillers, so feed and thicken the lawn to crowd it out."
        ),
        "cardamine" to WeedInfo(
            "Hairy Bittercress",
            "Explosive seed capsules disperse seeds 1m+ away, rapid lifecycle",
            "Hoe or hand-pull before the slender seed pods form (they fire seeds when touched). Mulch bare soil. Quick and easy to control if caught early."
        ),
        "lamium" to WeedInfo(
            "Dead Nettle",
            "Spreads aggressively in garden beds via stems and seeds",
            "Hand-pull or hoe — it's shallow-rooted and lifts easily. Mulch beds to suppress seedlings."
        ),
        "galium" to WeedInfo(
            "Cleavers / Goosegrass",
            "Clings via tiny hooks, smothers other plants, very prolific seeder",
            "Pull whole plants before the sticky seeds form (they cling to clothing and fur and spread everywhere). Mulch bare ground. Easy to remove while young."
        ),
        "senecio" to WeedInfo(
            "Groundsel",
            "Year-round annual weed, seeds freely in all seasons",
            "Hoe or hand-pull before the fluffy seed heads open. Mulch bare soil — it seeds almost year-round, so stay on top of it."
        ),
        "sonchus" to WeedInfo(
            "Sowthistle",
            "Wind-dispersed seeds, fast growing annual taking over bare soil",
            "Hoe or pull young plants with the root before they flower. Wear gloves. Mulch bare ground to block seedlings."
        ),
        "urtica" to WeedInfo(
            "Stinging Nettle",
            "Painful sting, spreads aggressively via underground rhizomes",
            "Wear thick gloves and long sleeves. Dig out the yellow underground rhizomes completely; repeated cutting weakens clumps over time. Treat regrowth with glyphosate."
        ),
        "capsella" to WeedInfo(
            "Shepherd's Purse",
            "Common in lawns and beds, germinates very early in spring",
            "Hoe or hand-pull before the heart-shaped seed pods form. Mulch bare soil. Simple to control if tackled early."
        ),
        "poa" to WeedInfo(
            "Annual Meadow Grass",
            "Self-seeds prolifically in lawns, dies in summer leaving bare patches",
            "Hand-pull tufts from lawns and beds before they seed. It's a grass, so lawn selective weedkillers won't touch it — thicken and feed the lawn to crowd it out instead."
        ),
        "bromus" to WeedInfo(
            "Brome Grass",
            "Invasive annual grass that blends in with lawn until it seeds",
            "Pull or dig clumps before the seed heads ripen. It can't be selectively sprayed within a lawn — in beds and borders spot-treat with glyphosate."
        ),
        "elytrigia" to WeedInfo(
            "Couch Grass",
            "Highly invasive perennial grass, spreads via underground rhizomes",
            "Dig out every white underground rhizome — each fragment regrows. Sieve the soil as you go. Treat regrowth foliage with glyphosate and repeat; mulch cleared ground."
        ),
        "elymus" to WeedInfo(
            "Couch Grass",
            "Underground rhizomes are nearly impossible to fully remove",
            "Dig out every white underground rhizome — each fragment regrows. Sieve the soil as you go. Treat regrowth foliage with glyphosate and repeat; mulch cleared ground."
        ),
        "agrostis" to WeedInfo(
            "Creeping Bent Grass",
            "Forms dense mats that suppress fine lawn grass",
            "Rake and scarify hard to lift the mats, then dig out patches. Overseed lawns with your chosen grass to outcompete it. In beds, spot-treat with glyphosate."
        ),
        "trifolium" to WeedInfo(
            "Clover",
            "Spreads aggressively, makes lawn uneven and attracts stinging insects near paths",
            "Rake up the runners and hand-pull. Clover thrives in poor soil, so feed the lawn with a nitrogen-rich fertiliser to suppress it. Spot-treat with a selective clover weedkiller if needed."
        ),
        "medicago" to WeedInfo(
            "Medick",
            "Annual weed that spreads quickly, competes with lawn grass",
            "Hoe or hand-pull before it seeds. Feed and thicken the lawn to crowd it out. Mulch beds to block seedlings."
        ),
        "hieracium" to WeedInfo(
            "Hawkweed",
            "Spreads via runners and wind seeds, forms invasive rosettes",
            "Dig out the rosettes with the root before the runners spread. Remove seed heads before they blow. Spot-treat regrowth with a selective broadleaf weedkiller."
        ),
        "hypochaeris" to WeedInfo(
            "Cat's Ear",
            "Rosette weed resembling dandelion, deep taproots and wind seeds",
            "Lever out the whole taproot and rosette with a hand fork. Remove flower stems before they seed. Spot-treat lawns with a selective broadleaf weedkiller."
        ),
        "leontodon" to WeedInfo(
            "Hawkbit",
            "Dandelion-like rosette suppresses grass, spreads via wind",
            "Dig out the taproot and rosette. Remove seed heads promptly. A selective broadleaf weedkiller works well on lawn infestations."
        ),
        "achillea" to WeedInfo(
            "Yarrow",
            "Spreads via rhizomes, can take over lawns and flower beds",
            "Dig out the creeping rhizomes fully. Rake to lift the ferny foliage before mowing. Spot-treat regrowth with a selective broadleaf weedkiller."
        ),
        "ranunculus" to WeedInfo(
            "Creeping Buttercup",
            "Spreads via runners (stolons) that invade lawns and borders rapidly",
            "Dig out the plant along with its rooting runners. It loves wet ground, so improving drainage helps a lot. Spot-treat regrowth with a selective broadleaf weedkiller."
        ),
        "ficaria" to WeedInfo(
            "Lesser Celandine",
            "Underground tubers and bulbils carpet entire areas, very hard to eradicate",
            "Dig out carefully with every tiny tuber and bulbil — sieve the soil, as any left behind regrow. Smother cleared ground with thick mulch. Very persistent; treat foliage with glyphosate if needed."
        ),
        "geum" to WeedInfo(
            "Wood Avens / Herb Bennet",
            "Self-seeds prolifically in borders, hooks seeds onto clothing and animals",
            "Hand-pull or dig out the root before the hooked seeds form. Mulch beds to block seedlings. Easy to control if caught before it seeds."
        ),
        "lolium" to WeedInfo(
            "Ryegrass (Weed Type)",
            "Can outcompete fine-leaved lawn grasses when growing as a weed",
            "Hand-pull clumps from fine lawns and beds — it's a grass, so lawn selective weedkillers won't touch it. Overseed with your desired grass to blend it out."
        ),
        "bellis" to WeedInfo(
            "Common Daisy",
            "Forms mats that crowd out lawn grass (can look decorative but spreads aggressively)",
            "Lever out the rosettes with a hand fork or daisy grubber. Mow regularly and feed the lawn to keep it dense. A selective broadleaf weedkiller controls heavy infestations."
        ),
    )

    fun identify(scientificName: String): WeedInfo? {
        val genus = scientificName.lowercase().trim().split(" ").firstOrNull() ?: return null
        return weeds[genus]
    }
}
