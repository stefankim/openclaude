package com.garden.flowerid

object WeedDatabase {

    data class WeedInfo(
        val commonName: String,
        val reason: String
    )

    // Keyed by lowercase genus name
    private val weeds = mapOf(
        "taraxacum" to WeedInfo("Dandelion", "Invasive taproot, spreads rapidly via wind-blown seeds, competes aggressively with lawn grass"),
        "cirsium" to WeedInfo("Thistle", "Deep taproots and spiny rosettes crowd out grass, spreads via wind"),
        "convolvulus" to WeedInfo("Bindweed", "Extremely invasive climber, strangles other plants, nearly impossible to fully eradicate"),
        "calystegia" to WeedInfo("Hedge Bindweed", "Fast-spreading climber, rhizomes spread underground"),
        "aegopodium" to WeedInfo("Ground Elder", "Extremely invasive, underground rhizomes spread rapidly and suppress all other plants"),
        "stellaria" to WeedInfo("Chickweed", "Rapid grower that smothers lawn grass, thrives in cool weather"),
        "plantago" to WeedInfo("Plantain Weed", "Flat rosettes smother grass, very drought-resistant once established"),
        "rumex" to WeedInfo("Dock", "Deep taproot regrows from fragments — removing it often makes it worse"),
        "ambrosia" to WeedInfo("Ragweed", "Causes severe allergies, aggressive annual that self-seeds prolifically"),
        "digitaria" to WeedInfo("Crabgrass", "Invasive annual grass that dominates lawns in summer heat"),
        "cyperus" to WeedInfo("Nutsedge", "Grass-like weed spreading via underground tubers, resistant to most herbicides"),
        "euphorbia" to WeedInfo("Spurge", "Spreads very quickly, produces toxic milky sap harmful to other plants"),
        "chenopodium" to WeedInfo("Fat Hen / Goosefoot", "One plant produces up to 75,000 seeds — prolific annual weed"),
        "reynoutria" to WeedInfo("Japanese Knotweed", "EXTREMELY invasive — legally notifiable in many countries, can damage building foundations"),
        "fallopia" to WeedInfo("Japanese Knotweed", "EXTREMELY invasive — legally notifiable in many countries, can damage building foundations"),
        "polygonum" to WeedInfo("Knotweed", "Fast-spreading invasive, very difficult to control once established"),
        "persicaria" to WeedInfo("Redshank / Knotweed", "Aggressive annual, self-seeds freely in moist areas"),
        "oxalis" to WeedInfo("Wood Sorrel", "Spreads by both seeds and underground bulbils, very hard to fully remove"),
        "veronica" to WeedInfo("Speedwell", "Creeping mat that smothers fine lawn grass, spreads by stems and seeds"),
        "cardamine" to WeedInfo("Hairy Bittercress", "Explosive seed capsules disperse seeds 1m+ away, rapid lifecycle"),
        "lamium" to WeedInfo("Dead Nettle", "Spreads aggressively in garden beds via stems and seeds"),
        "galium" to WeedInfo("Cleavers / Goosegrass", "Clings via tiny hooks, smothers other plants, very prolific seeder"),
        "senecio" to WeedInfo("Groundsel", "Year-round annual weed, seeds freely in all seasons"),
        "sonchus" to WeedInfo("Sowthistle", "Wind-dispersed seeds, fast growing annual taking over bare soil"),
        "urtica" to WeedInfo("Stinging Nettle", "Painful sting, spreads aggressively via underground rhizomes"),
        "capsella" to WeedInfo("Shepherd's Purse", "Common in lawns and beds, germinates very early in spring"),
        "poa" to WeedInfo("Annual Meadow Grass", "Self-seeds prolifically in lawns, dies in summer leaving bare patches"),
        "bromus" to WeedInfo("Brome Grass", "Invasive annual grass that blends in with lawn until it seeds"),
        "elytrigia" to WeedInfo("Couch Grass", "Highly invasive perennial grass, spreads via underground rhizomes"),
        "elymus" to WeedInfo("Couch Grass", "Underground rhizomes are nearly impossible to fully remove"),
        "agrostis" to WeedInfo("Creeping Bent Grass", "Forms dense mats that suppress fine lawn grass"),
        "trifolium" to WeedInfo("Clover", "Spreads aggressively, makes lawn uneven and attracts stinging insects near paths"),
        "medicago" to WeedInfo("Medick", "Annual weed that spreads quickly, competes with lawn grass"),
        "hieracium" to WeedInfo("Hawkweed", "Spreads via runners and wind seeds, forms invasive rosettes"),
        "hypochaeris" to WeedInfo("Cat's Ear", "Rosette weed resembling dandelion, deep taproots and wind seeds"),
        "leontodon" to WeedInfo("Hawkbit", "Dandelion-like rosette suppresses grass, spreads via wind"),
        "achillea" to WeedInfo("Yarrow", "Spreads via rhizomes, can take over lawns and flower beds"),
        "ranunculus" to WeedInfo("Creeping Buttercup", "Spreads via runners (stolons) that invade lawns and borders rapidly"),
        "ficaria" to WeedInfo("Lesser Celandine", "Underground tubers and bulbils carpet entire areas, very hard to eradicate"),
        "geum" to WeedInfo("Wood Avens / Herb Bennet", "Self-seeds prolifically in borders, hooks seeds onto clothing and animals"),
        "lolium" to WeedInfo("Ryegrass (Weed Type)", "Can outcompete fine-leaved lawn grasses when growing as a weed"),
        "bellis" to WeedInfo("Common Daisy", "Forms mats that crowd out lawn grass (can look decorative but spreads aggressively)"),
    )

    fun identify(scientificName: String): WeedInfo? {
        val genus = scientificName.lowercase().trim().split(" ").firstOrNull() ?: return null
        return weeds[genus]
    }
}
