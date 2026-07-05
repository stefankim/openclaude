package com.garden.flowerid

object WeedDatabase {

    enum class Severity { MILD, AGGRESSIVE, NOTIFIABLE }

    data class WeedInfo(
        val commonName: String,
        val reason: String,
        val removal: String,
        val severity: Severity,
        val hazard: String?
    )

    // Keyed by lowercase genus name
    private val weeds = mapOf(
        "taraxacum" to WeedInfo(
            "Dandelion",
            "Invasive taproot, spreads rapidly via wind-blown seeds, competes aggressively with lawn grass",
            "Dig out the entire taproot with a narrow weeding knife or dandelion fork, ideally after rain when the soil is soft. Remove flower heads before they turn to seed. For lawns, spot-treat with a selective broadleaf weedkiller (e.g. 2,4-D / dicamba).",
            Severity.AGGRESSIVE,
            null
        ),
        "cirsium" to WeedInfo(
            "Thistle",
            "Deep taproots and spiny rosettes crowd out grass, spreads via wind",
            "Wear thick gloves. Dig out the full taproot; repeated cutting at the base slowly starves it. Never let it flower — remove seed heads. Spot-treat regrowth with a selective broadleaf weedkiller.",
            Severity.AGGRESSIVE,
            "Sharp spines can prick skin — wear thick gloves"
        ),
        "convolvulus" to WeedInfo(
            "Bindweed",
            "Extremely invasive climber, strangles other plants, nearly impossible to fully eradicate",
            "It won't pull out fully — dig out as much of the white root as you can. Let stems climb a cane, then paint the leaves with a glyphosate-based systemic weedkiller so it travels to the roots. Repeat through the season; mulch to suppress regrowth.",
            Severity.AGGRESSIVE,
            null
        ),
        "calystegia" to WeedInfo(
            "Hedge Bindweed",
            "Fast-spreading climber, rhizomes spread underground",
            "Trace and dig out the underground rhizomes, or let it climb a cane and treat the foliage with glyphosate. Mulch heavily and pull any regrowth promptly.",
            Severity.AGGRESSIVE,
            null
        ),
        "aegopodium" to WeedInfo(
            "Ground Elder",
            "Extremely invasive, underground rhizomes spread rapidly and suppress all other plants",
            "Dig out every scrap of white rhizome — any fragment left behind regrows. Smother cleared ground with cardboard or black plastic for a full season, or treat the leafy growth with glyphosate. Persistence is essential.",
            Severity.AGGRESSIVE,
            null
        ),
        "stellaria" to WeedInfo(
            "Chickweed",
            "Rapid grower that smothers lawn grass, thrives in cool weather",
            "Hoe or hand-pull before it sets seed — the roots are shallow and lift easily. Mulch bare soil to block seedlings. Rarely needs weedkiller.",
            Severity.MILD,
            null
        ),
        "plantago" to WeedInfo(
            "Plantain Weed",
            "Flat rosettes smother grass, very drought-resistant once established",
            "Lever out the whole rosette and taproot with a hand fork. For lawns, spot-treat with a selective broadleaf weedkiller. Keep the lawn thick to stop it returning.",
            Severity.MILD,
            null
        ),
        "rumex" to WeedInfo(
            "Dock",
            "Deep taproot regrows from fragments — removing it often makes it worse",
            "Dig out the entire long taproot — any piece left behind resprouts. Remove seed heads promptly. Spot-treat stubborn regrowth with a systemic (glyphosate) or selective broadleaf weedkiller.",
            Severity.AGGRESSIVE,
            null
        ),
        "ambrosia" to WeedInfo(
            "Ragweed",
            "Causes severe allergies, aggressive annual that self-seeds prolifically",
            "Pull or hoe young plants (wear gloves and ideally a mask — the pollen is highly allergenic). Remove every plant before it flowers. Mulch bare soil to prevent seedlings.",
            Severity.AGGRESSIVE,
            "Pollen triggers severe hay fever and can worsen asthma"
        ),
        "digitaria" to WeedInfo(
            "Crabgrass",
            "Invasive annual grass that dominates lawns in summer heat",
            "Pull clumps before they seed. Keep the lawn thick and mow high to shade it out. Apply a pre-emergent crabgrass preventer in early spring before soil warms.",
            Severity.AGGRESSIVE,
            null
        ),
        "cyperus" to WeedInfo(
            "Nutsedge",
            "Grass-like weed spreading via underground tubers, resistant to most herbicides",
            "Pull young plants weekly before the underground tubers (\"nutlets\") form — persistence starves the roots. It shrugs off most weedkillers; for heavy infestations use a sedge-specific herbicide (e.g. halosulfuron).",
            Severity.AGGRESSIVE,
            null
        ),
        "euphorbia" to WeedInfo(
            "Spurge",
            "Spreads very quickly, produces toxic milky sap harmful to other plants",
            "Wear gloves — the milky sap irritates skin and eyes. Hand-pull young plants with the root and hoe seedlings before they seed. Mulch bare soil.",
            Severity.AGGRESSIVE,
            "Milky sap is toxic and irritates skin & eyes — keep away from pets & children"
        ),
        "chenopodium" to WeedInfo(
            "Fat Hen / Goosefoot",
            "One plant produces up to 75,000 seeds — prolific annual weed",
            "Hoe or hand-pull before it flowers — stopping the seed is everything. Mulch bare ground. Easy to control if caught early.",
            Severity.MILD,
            null
        ),
        "reynoutria" to WeedInfo(
            "Japanese Knotweed",
            "EXTREMELY invasive — legally notifiable in many countries, can damage building foundations",
            "Do NOT dig, strim or compost it — fragments spread it and disposal is legally controlled as hazardous waste. This needs professional treatment (repeated glyphosate stem-injection over several years). Check and follow your local reporting rules.",
            Severity.NOTIFIABLE,
            null
        ),
        "fallopia" to WeedInfo(
            "Japanese Knotweed",
            "EXTREMELY invasive — legally notifiable in many countries, can damage building foundations",
            "Do NOT dig, strim or compost it — fragments spread it and disposal is legally controlled as hazardous waste. This needs professional treatment (repeated glyphosate stem-injection over several years). Check and follow your local reporting rules.",
            Severity.NOTIFIABLE,
            null
        ),
        "polygonum" to WeedInfo(
            "Knotweed",
            "Fast-spreading invasive, very difficult to control once established",
            "Hoe or pull young plants; dig established roots out fully. It's persistent — repeat, and treat any regrowth with a systemic (glyphosate) weedkiller.",
            Severity.AGGRESSIVE,
            null
        ),
        "persicaria" to WeedInfo(
            "Redshank / Knotweed",
            "Aggressive annual, self-seeds freely in moist areas",
            "Hoe or hand-pull before it seeds. It loves damp ground, so improving drainage helps. Mulch bare soil.",
            Severity.MILD,
            null
        ),
        "oxalis" to WeedInfo(
            "Wood Sorrel",
            "Spreads by both seeds and underground bulbils, very hard to fully remove",
            "Dig out carefully with every tiny bulbil — sieve the soil, as any left behind regrow. Mulch cleared areas and spot-treat regrowth with glyphosate. Very persistent, so keep at it.",
            Severity.AGGRESSIVE,
            "Mildly toxic if eaten in large amounts (high oxalic acid)"
        ),
        "veronica" to WeedInfo(
            "Speedwell",
            "Creeping mat that smothers fine lawn grass, spreads by stems and seeds",
            "Rake up the creeping stems and hand-pull; scarify lawns to lift the mats. It resists most selective weedkillers, so feed and thicken the lawn to crowd it out.",
            Severity.MILD,
            null
        ),
        "cardamine" to WeedInfo(
            "Hairy Bittercress",
            "Explosive seed capsules disperse seeds 1m+ away, rapid lifecycle",
            "Hoe or hand-pull before the slender seed pods form (they fire seeds when touched). Mulch bare soil. Quick and easy to control if caught early.",
            Severity.MILD,
            null
        ),
        "lamium" to WeedInfo(
            "Dead Nettle",
            "Spreads aggressively in garden beds via stems and seeds",
            "Hand-pull or hoe — it's shallow-rooted and lifts easily. Mulch beds to suppress seedlings.",
            Severity.MILD,
            null
        ),
        "galium" to WeedInfo(
            "Cleavers / Goosegrass",
            "Clings via tiny hooks, smothers other plants, very prolific seeder",
            "Pull whole plants before the sticky seeds form (they cling to clothing and fur and spread everywhere). Mulch bare ground. Easy to remove while young.",
            Severity.MILD,
            null
        ),
        "senecio" to WeedInfo(
            "Groundsel",
            "Year-round annual weed, seeds freely in all seasons",
            "Hoe or hand-pull before the fluffy seed heads open. Mulch bare soil — it seeds almost year-round, so stay on top of it.",
            Severity.MILD,
            "Toxic to horses, livestock and pets if eaten"
        ),
        "sonchus" to WeedInfo(
            "Sowthistle",
            "Wind-dispersed seeds, fast growing annual taking over bare soil",
            "Hoe or pull young plants with the root before they flower. Wear gloves. Mulch bare ground to block seedlings.",
            Severity.MILD,
            null
        ),
        "urtica" to WeedInfo(
            "Stinging Nettle",
            "Painful sting, spreads aggressively via underground rhizomes",
            "Wear thick gloves and long sleeves. Dig out the yellow underground rhizomes completely; repeated cutting weakens clumps over time. Treat regrowth with glyphosate.",
            Severity.AGGRESSIVE,
            "Stings on contact — wear gloves and cover skin"
        ),
        "capsella" to WeedInfo(
            "Shepherd's Purse",
            "Common in lawns and beds, germinates very early in spring",
            "Hoe or hand-pull before the heart-shaped seed pods form. Mulch bare soil. Simple to control if tackled early.",
            Severity.MILD,
            null
        ),
        "poa" to WeedInfo(
            "Annual Meadow Grass",
            "Self-seeds prolifically in lawns, dies in summer leaving bare patches",
            "Hand-pull tufts from lawns and beds before they seed. It's a grass, so lawn selective weedkillers won't touch it — thicken and feed the lawn to crowd it out instead.",
            Severity.MILD,
            null
        ),
        "bromus" to WeedInfo(
            "Brome Grass",
            "Invasive annual grass that blends in with lawn until it seeds",
            "Pull or dig clumps before the seed heads ripen. It can't be selectively sprayed within a lawn — in beds and borders spot-treat with glyphosate.",
            Severity.MILD,
            null
        ),
        "elytrigia" to WeedInfo(
            "Couch Grass",
            "Highly invasive perennial grass, spreads via underground rhizomes",
            "Dig out every white underground rhizome — each fragment regrows. Sieve the soil as you go. Treat regrowth foliage with glyphosate and repeat; mulch cleared ground.",
            Severity.AGGRESSIVE,
            null
        ),
        "elymus" to WeedInfo(
            "Couch Grass",
            "Underground rhizomes are nearly impossible to fully remove",
            "Dig out every white underground rhizome — each fragment regrows. Sieve the soil as you go. Treat regrowth foliage with glyphosate and repeat; mulch cleared ground.",
            Severity.AGGRESSIVE,
            null
        ),
        "agrostis" to WeedInfo(
            "Creeping Bent Grass",
            "Forms dense mats that suppress fine lawn grass",
            "Rake and scarify hard to lift the mats, then dig out patches. Overseed lawns with your chosen grass to outcompete it. In beds, spot-treat with glyphosate.",
            Severity.AGGRESSIVE,
            null
        ),
        "trifolium" to WeedInfo(
            "Clover",
            "Spreads aggressively, makes lawn uneven and attracts stinging insects near paths",
            "Rake up the runners and hand-pull. Clover thrives in poor soil, so feed the lawn with a nitrogen-rich fertiliser to suppress it. Spot-treat with a selective clover weedkiller if needed.",
            Severity.MILD,
            null
        ),
        "medicago" to WeedInfo(
            "Medick",
            "Annual weed that spreads quickly, competes with lawn grass",
            "Hoe or hand-pull before it seeds. Feed and thicken the lawn to crowd it out. Mulch beds to block seedlings.",
            Severity.MILD,
            null
        ),
        "hieracium" to WeedInfo(
            "Hawkweed",
            "Spreads via runners and wind seeds, forms invasive rosettes",
            "Dig out the rosettes with the root before the runners spread. Remove seed heads before they blow. Spot-treat regrowth with a selective broadleaf weedkiller.",
            Severity.AGGRESSIVE,
            null
        ),
        "hypochaeris" to WeedInfo(
            "Cat's Ear",
            "Rosette weed resembling dandelion, deep taproots and wind seeds",
            "Lever out the whole taproot and rosette with a hand fork. Remove flower stems before they seed. Spot-treat lawns with a selective broadleaf weedkiller.",
            Severity.MILD,
            null
        ),
        "leontodon" to WeedInfo(
            "Hawkbit",
            "Dandelion-like rosette suppresses grass, spreads via wind",
            "Dig out the taproot and rosette. Remove seed heads promptly. A selective broadleaf weedkiller works well on lawn infestations.",
            Severity.MILD,
            null
        ),
        "achillea" to WeedInfo(
            "Yarrow",
            "Spreads via rhizomes, can take over lawns and flower beds",
            "Dig out the creeping rhizomes fully. Rake to lift the ferny foliage before mowing. Spot-treat regrowth with a selective broadleaf weedkiller.",
            Severity.AGGRESSIVE,
            null
        ),
        "ranunculus" to WeedInfo(
            "Creeping Buttercup",
            "Spreads via runners (stolons) that invade lawns and borders rapidly",
            "Dig out the plant along with its rooting runners. It loves wet ground, so improving drainage helps a lot. Spot-treat regrowth with a selective broadleaf weedkiller.",
            Severity.AGGRESSIVE,
            "Toxic to pets and livestock if eaten; sap can irritate skin"
        ),
        "ficaria" to WeedInfo(
            "Lesser Celandine",
            "Underground tubers and bulbils carpet entire areas, very hard to eradicate",
            "Dig out carefully with every tiny tuber and bulbil — sieve the soil, as any left behind regrow. Smother cleared ground with thick mulch. Very persistent; treat foliage with glyphosate if needed.",
            Severity.AGGRESSIVE,
            "Toxic if eaten raw — keep away from pets & children"
        ),
        "geum" to WeedInfo(
            "Wood Avens / Herb Bennet",
            "Self-seeds prolifically in borders, hooks seeds onto clothing and animals",
            "Hand-pull or dig out the root before the hooked seeds form. Mulch beds to block seedlings. Easy to control if caught before it seeds.",
            Severity.MILD,
            null
        ),
        "lolium" to WeedInfo(
            "Ryegrass (Weed Type)",
            "Can outcompete fine-leaved lawn grasses when growing as a weed",
            "Hand-pull clumps from fine lawns and beds — it's a grass, so lawn selective weedkillers won't touch it. Overseed with your desired grass to blend it out.",
            Severity.MILD,
            null
        ),
        "bellis" to WeedInfo(
            "Common Daisy",
            "Forms mats that crowd out lawn grass (can look decorative but spreads aggressively)",
            "Lever out the rosettes with a hand fork or daisy grubber. Mow regularly and feed the lawn to keep it dense. A selective broadleaf weedkiller controls heavy infestations.",
            Severity.MILD,
            null
        ),
        "heracleum" to WeedInfo(
            "Hogweed / Giant Hogweed",
            "Giant hogweed is a dangerous invasive — sap causes severe burns; regular hogweed also spreads aggressively",
            "Do NOT touch it or strim it — the sap plus sunlight causes severe skin burns and blistering. For giant hogweed, call a professional. Small common hogweed can be dug out wearing full covering, gloves and eye protection.",
            Severity.NOTIFIABLE,
            "Sap causes severe burns and blisters in sunlight — do NOT touch bare-skinned; dangerous to children and pets"
        ),
        "equisetum" to WeedInfo(
            "Horsetail / Marestail",
            "Ancient deep-rooted weed, rhizomes reach 2m down, extremely persistent",
            "You can't dig it out fully — roots go metres deep. Keep cutting shoots to starve it and improve drainage. Crush the stems then treat with a glyphosate-based weedkiller; expect a multi-year effort.",
            Severity.AGGRESSIVE,
            "Toxic to horses and livestock if eaten in quantity"
        ),
        "glechoma" to WeedInfo(
            "Ground Ivy / Creeping Charlie",
            "Creeping stems root at every node and quickly carpet lawns and beds",
            "Rake up the runners and hand-pull after rain — every rooted node must come out. Improve lawn drainage and light. Spot-treat with a selective broadleaf weedkiller in autumn.",
            Severity.AGGRESSIVE,
            null
        ),
        "alliaria" to WeedInfo(
            "Garlic Mustard",
            "Invasive biennial that outcompetes native plants and self-seeds heavily",
            "Hand-pull before it flowers in its second year, taking the S-shaped root. Bag pulled plants — they can still set seed. Recheck the area for two seasons.",
            Severity.AGGRESSIVE,
            null
        ),
        "anthriscus" to WeedInfo(
            "Cow Parsley",
            "Rapid spring grower that swamps borders and self-seeds prolifically",
            "Dig out the taproot of young plants; cut established stands before flowering to stop seeding. Repeated cutting exhausts the root over a couple of seasons.",
            Severity.MILD,
            "Easily confused with toxic hemlock — wear gloves and don't eat any part"
        ),
        "epilobium" to WeedInfo(
            "Willowherb",
            "Wind-blown seeds colonise any bare soil, roots snap when pulled",
            "Pull young plants when the soil is moist so the root comes out whole. Never let it flower — one plant releases tens of thousands of seeds. Mulch beds thickly.",
            Severity.MILD,
            null
        ),
        "conyza" to WeedInfo(
            "Fleabane",
            "Tall annual producing huge numbers of wind-dispersed seeds, herbicide-resistant strains exist",
            "Hand-pull or hoe while it is a small rosette — mature plants resist many weedkillers. Never let it set seed; mulch bare ground.",
            Severity.MILD,
            null
        ),
        "amaranthus" to WeedInfo(
            "Pigweed",
            "Fast-growing annual, one plant can shed over 100,000 seeds",
            "Hoe or pull seedlings early — plants outgrow crops within weeks. Remove before flowering and mulch. Persistent seed bank means checking for several seasons.",
            Severity.AGGRESSIVE,
            null
        ),
        "portulaca" to WeedInfo(
            "Purslane",
            "Succulent mat-former; stem fragments re-root and seeds survive decades",
            "Hoe on a hot dry day and remove the pieces — fragments left on moist soil re-root. Don't compost it. Mulch to block the long-lived seeds.",
            Severity.MILD,
            null
        ),
        "setaria" to WeedInfo(
            "Foxtail Grass",
            "Annual grass whose bristly seed heads dominate thin lawns in summer",
            "Pull or dig clumps before seed heads form. Mow high and overseed to thicken the lawn; a spring pre-emergent herbicide stops the seeds germinating.",
            Severity.MILD,
            "Bristly seed heads can lodge in pets' paws, ears and noses"
        ),
        "echinochloa" to WeedInfo(
            "Barnyard Grass",
            "Vigorous annual grass of damp, disturbed ground; heavy seeder",
            "Pull or hoe young clumps before they seed, ideally when soil is moist. Improve drainage and keep grass dense — it only invades thin, wet patches.",
            Severity.MILD,
            null
        ),
        "mercurialis" to WeedInfo(
            "Dog's Mercury",
            "Shade-loving carpeting weed that spreads by rhizomes under hedges and trees",
            "Dig out the shallow rhizomes with a fork and repeat as regrowth appears. Wear gloves. Planting dense ground cover shades it out over time.",
            Severity.MILD,
            "Poisonous to people and pets if eaten"
        ),
    )

    fun identify(scientificName: String): WeedInfo? {
        val genus = scientificName.lowercase().trim().split(" ").firstOrNull() ?: return null
        return weeds[genus]
    }
}
