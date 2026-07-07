package com.garden.flowerid

import java.util.Locale

object WeedDatabase {

    enum class Severity { MILD, AGGRESSIVE, NOTIFIABLE }

    data class WeedInfo(
        val commonName: String,
        val reason: String,
        val removal: String,
        val severity: Severity,
        val hazard: String?
    )

    private data class Texts(
        val reason: String,
        val removal: String,
        val hazard: String? = null
    )

    private data class Entry(
        val commonName: String,
        val severity: Severity,
        val en: Texts,
        val sk: Texts
    )

    // Keyed by lowercase genus name
    private val weeds = mapOf(
        "taraxacum" to Entry(
            "Dandelion", Severity.AGGRESSIVE,
            en = Texts(
                "Invasive taproot, spreads rapidly via wind-blown seeds, competes aggressively with lawn grass",
                "Dig out the entire taproot with a narrow weeding knife or dandelion fork, ideally after rain when the soil is soft. Remove flower heads before they turn to seed. For lawns, spot-treat with a selective broadleaf weedkiller (e.g. 2,4-D / dicamba)."
            ),
            sk = Texts(
                "Invázny kolový koreň, rýchlo sa šíri semenami roznášanými vetrom a agresívne konkuruje trávniku",
                "Vyryte celý kolový koreň úzkym vypichovačom buriny, najlepšie po daždi, keď je pôda mäkká. Kvety odstráňte skôr, než sa zmenia na semená. Na trávniku bodovo použite selektívny herbicíd na dvojklíčnolistové buriny (napr. 2,4-D / dicamba)."
            )
        ),
        "cirsium" to Entry(
            "Thistle", Severity.AGGRESSIVE,
            en = Texts(
                "Deep taproots and spiny rosettes crowd out grass, spreads via wind",
                "Wear thick gloves. Dig out the full taproot; repeated cutting at the base slowly starves it. Never let it flower — remove seed heads. Spot-treat regrowth with a selective broadleaf weedkiller.",
                "Sharp spines can prick skin — wear thick gloves"
            ),
            sk = Texts(
                "Hlboké kolové korene a pichľavé ružice vytláčajú trávu, šíri sa vetrom",
                "Noste hrubé rukavice. Vykopte celý kolový koreň; opakované odrezávanie pri zemi rastlinu postupne vyčerpá. Nenechajte ju zakvitnúť — odstraňujte semenné hlávky. Odrastky bodovo ošetrite selektívnym herbicídom.",
                "Ostré tŕne môžu poraniť pokožku — noste hrubé rukavice"
            )
        ),
        "convolvulus" to Entry(
            "Bindweed", Severity.AGGRESSIVE,
            en = Texts(
                "Extremely invasive climber, strangles other plants, nearly impossible to fully eradicate",
                "It won't pull out fully — dig out as much of the white root as you can. Let stems climb a cane, then paint the leaves with a glyphosate-based systemic weedkiller so it travels to the roots. Repeat through the season; mulch to suppress regrowth."
            ),
            sk = Texts(
                "Mimoriadne invázna popínavá rastlina, škrtí ostatné rastliny a takmer sa nedá úplne odstrániť",
                "Úplne sa vytrhnúť nedá — vykopte čo najviac bieleho koreňa. Nechajte stonky vyliezť po paličke a potom listy natrite systémovým herbicídom na báze glyfosátu, aby sa dostal do koreňov. Opakujte počas celej sezóny; mulčujte proti odrastaniu."
            )
        ),
        "calystegia" to Entry(
            "Hedge Bindweed", Severity.AGGRESSIVE,
            en = Texts(
                "Fast-spreading climber, rhizomes spread underground",
                "Trace and dig out the underground rhizomes, or let it climb a cane and treat the foliage with glyphosate. Mulch heavily and pull any regrowth promptly."
            ),
            sk = Texts(
                "Rýchlo sa šíriaca popínavá rastlina, podzemky sa rozrastajú pod zemou",
                "Vystopujte a vykopte podzemné podzemky, alebo ju nechajte vyliezť po paličke a listy ošetrite glyfosátom. Silno mulčujte a odrastky ihneď vytrhnite."
            )
        ),
        "aegopodium" to Entry(
            "Ground Elder", Severity.AGGRESSIVE,
            en = Texts(
                "Extremely invasive, underground rhizomes spread rapidly and suppress all other plants",
                "Dig out every scrap of white rhizome — any fragment left behind regrows. Smother cleared ground with cardboard or black plastic for a full season, or treat the leafy growth with glyphosate. Persistence is essential."
            ),
            sk = Texts(
                "Mimoriadne invázna, podzemné podzemky sa rýchlo šíria a potláčajú všetky ostatné rastliny",
                "Vykopte každý kúsok bieleho podzemku — každý zabudnutý úlomok znova vyrastie. Vyčistenú plochu prikryte kartónom alebo čiernou fóliou na celú sezónu, alebo olistené výhonky ošetrite glyfosátom. Kľúčová je vytrvalosť."
            )
        ),
        "stellaria" to Entry(
            "Chickweed", Severity.MILD,
            en = Texts(
                "Rapid grower that smothers lawn grass, thrives in cool weather",
                "Hoe or hand-pull before it sets seed — the roots are shallow and lift easily. Mulch bare soil to block seedlings. Rarely needs weedkiller."
            ),
            sk = Texts(
                "Rýchlo rastie a dusí trávnik, darí sa jej v chladnom počasí",
                "Okopávajte alebo vytrhávajte skôr, než vysemení — korene sú plytké a ľahko sa vytiahnu. Holú pôdu mulčujte proti semenáčikom. Herbicíd zvyčajne nie je potrebný."
            )
        ),
        "plantago" to Entry(
            "Plantain Weed", Severity.MILD,
            en = Texts(
                "Flat rosettes smother grass, very drought-resistant once established",
                "Lever out the whole rosette and taproot with a hand fork. For lawns, spot-treat with a selective broadleaf weedkiller. Keep the lawn thick to stop it returning."
            ),
            sk = Texts(
                "Ploché ružice dusia trávu, po zakorenení veľmi dobre znáša sucho",
                "Celú ružicu aj s kolovým koreňom vypáčte ručnou vidličkou. Na trávniku bodovo použite selektívny herbicíd. Hustý trávnik zabráni jej návratu."
            )
        ),
        "rumex" to Entry(
            "Dock", Severity.AGGRESSIVE,
            en = Texts(
                "Deep taproot regrows from fragments — removing it often makes it worse",
                "Dig out the entire long taproot — any piece left behind resprouts. Remove seed heads promptly. Spot-treat stubborn regrowth with a systemic (glyphosate) or selective broadleaf weedkiller."
            ),
            sk = Texts(
                "Hlboký kolový koreň znova vyrastá z úlomkov — nesprávne odstránenie situáciu často zhorší",
                "Vykopte celý dlhý kolový koreň — každý ponechaný kúsok znova vyrastie. Semenné hlávky odstraňujte ihneď. Tvrdohlavé odrastky bodovo ošetrite systémovým (glyfosát) alebo selektívnym herbicídom."
            )
        ),
        "ambrosia" to Entry(
            "Ragweed", Severity.AGGRESSIVE,
            en = Texts(
                "Causes severe allergies, aggressive annual that self-seeds prolifically",
                "Pull or hoe young plants (wear gloves and ideally a mask — the pollen is highly allergenic). Remove every plant before it flowers. Mulch bare soil to prevent seedlings.",
                "Pollen triggers severe hay fever and can worsen asthma"
            ),
            sk = Texts(
                "Spôsobuje silné alergie, agresívna jednoročná burina, ktorá sa hojne vysemeňuje",
                "Mladé rastliny vytrhávajte alebo okopávajte (noste rukavice a ideálne aj rúško — peľ je silne alergénny). Každú rastlinu odstráňte pred kvitnutím. Holú pôdu mulčujte proti semenáčikom.",
                "Peľ vyvoláva silnú sennú nádchu a môže zhoršiť astmu"
            )
        ),
        "digitaria" to Entry(
            "Crabgrass", Severity.AGGRESSIVE,
            en = Texts(
                "Invasive annual grass that dominates lawns in summer heat",
                "Pull clumps before they seed. Keep the lawn thick and mow high to shade it out. Apply a pre-emergent crabgrass preventer in early spring before soil warms."
            ),
            sk = Texts(
                "Invázna jednoročná tráva, ktorá v letných horúčavách ovládne trávnik",
                "Trsy vytrhnite skôr, než vysemenia. Udržujte hustý trávnik a koste vyššie, aby ju tráva zatienila. Skoro na jar aplikujte preemergentný prípravok proti prstovke."
            )
        ),
        "cyperus" to Entry(
            "Nutsedge", Severity.AGGRESSIVE,
            en = Texts(
                "Grass-like weed spreading via underground tubers, resistant to most herbicides",
                "Pull young plants weekly before the underground tubers (\"nutlets\") form — persistence starves the roots. It shrugs off most weedkillers; for heavy infestations use a sedge-specific herbicide (e.g. halosulfuron)."
            ),
            sk = Texts(
                "Burina podobná tráve, šíri sa podzemnými hľuzkami a odoláva väčšine herbicídov",
                "Mladé rastliny vytrhávajte každý týždeň, kým sa nevytvoria podzemné hľuzky — vytrvalosť vyčerpá korene. Väčšina herbicídov na ňu nefunguje; pri silnom zamorení použite špeciálny prípravok na šachorovité (napr. halosulfurón)."
            )
        ),
        "euphorbia" to Entry(
            "Spurge", Severity.AGGRESSIVE,
            en = Texts(
                "Spreads very quickly, produces toxic milky sap harmful to other plants",
                "Wear gloves — the milky sap irritates skin and eyes. Hand-pull young plants with the root and hoe seedlings before they seed. Mulch bare soil.",
                "Milky sap is toxic and irritates skin & eyes — keep away from pets & children"
            ),
            sk = Texts(
                "Šíri sa veľmi rýchlo, produkuje jedovatú mliečnu šťavu škodlivú pre ostatné rastliny",
                "Noste rukavice — mliečna šťava dráždi pokožku a oči. Mladé rastliny vytrhnite aj s koreňom a semenáčiky okopte skôr, než vysemenia. Holú pôdu mulčujte.",
                "Mliečna šťava je jedovatá a dráždi pokožku aj oči — držte mimo dosahu detí a zvierat"
            )
        ),
        "chenopodium" to Entry(
            "Fat Hen / Goosefoot", Severity.MILD,
            en = Texts(
                "One plant produces up to 75,000 seeds — prolific annual weed",
                "Hoe or hand-pull before it flowers — stopping the seed is everything. Mulch bare ground. Easy to control if caught early."
            ),
            sk = Texts(
                "Jedna rastlina vyprodukuje až 75 000 semien — mimoriadne plodná jednoročná burina",
                "Okopte alebo vytrhnite pred kvitnutím — najdôležitejšie je zastaviť semenenie. Holú pôdu mulčujte. Pri včasnom zásahu sa ľahko kontroluje."
            )
        ),
        "reynoutria" to Entry(
            "Japanese Knotweed", Severity.NOTIFIABLE,
            en = Texts(
                "EXTREMELY invasive — legally notifiable in many countries, can damage building foundations",
                "Do NOT dig, strim or compost it — fragments spread it and disposal is legally controlled as hazardous waste. This needs professional treatment (repeated glyphosate stem-injection over several years). Check and follow your local reporting rules."
            ),
            sk = Texts(
                "MIMORIADNE invázna — v mnohých krajinách podlieha ohlasovacej povinnosti, dokáže poškodiť základy budov",
                "NEVYKOPÁVAJTE, nekoste ani nekompostujte — úlomky ju šíria a likvidácia je právne regulovaná ako nebezpečný odpad. Vyžaduje profesionálny zásah (opakovaná injektáž glyfosátu do stoniek počas niekoľkých rokov). Overte si a dodržte miestne ohlasovacie pravidlá."
            )
        ),
        "fallopia" to Entry(
            "Japanese Knotweed", Severity.NOTIFIABLE,
            en = Texts(
                "EXTREMELY invasive — legally notifiable in many countries, can damage building foundations",
                "Do NOT dig, strim or compost it — fragments spread it and disposal is legally controlled as hazardous waste. This needs professional treatment (repeated glyphosate stem-injection over several years). Check and follow your local reporting rules."
            ),
            sk = Texts(
                "MIMORIADNE invázna — v mnohých krajinách podlieha ohlasovacej povinnosti, dokáže poškodiť základy budov",
                "NEVYKOPÁVAJTE, nekoste ani nekompostujte — úlomky ju šíria a likvidácia je právne regulovaná ako nebezpečný odpad. Vyžaduje profesionálny zásah (opakovaná injektáž glyfosátu do stoniek počas niekoľkých rokov). Overte si a dodržte miestne ohlasovacie pravidlá."
            )
        ),
        "polygonum" to Entry(
            "Knotweed", Severity.AGGRESSIVE,
            en = Texts(
                "Fast-spreading invasive, very difficult to control once established",
                "Hoe or pull young plants; dig established roots out fully. It's persistent — repeat, and treat any regrowth with a systemic (glyphosate) weedkiller."
            ),
            sk = Texts(
                "Rýchlo sa šíriaca invázna burina, po usadení sa veľmi ťažko kontroluje",
                "Mladé rastliny okopte alebo vytrhnite; staršie korene vykopte celé. Je vytrvalá — opakujte a odrastky ošetrite systémovým (glyfosátovým) herbicídom."
            )
        ),
        "persicaria" to Entry(
            "Redshank / Knotweed", Severity.MILD,
            en = Texts(
                "Aggressive annual, self-seeds freely in moist areas",
                "Hoe or hand-pull before it seeds. It loves damp ground, so improving drainage helps. Mulch bare soil."
            ),
            sk = Texts(
                "Agresívna jednoročná burina, voľne sa vysemeňuje na vlhkých miestach",
                "Okopte alebo vytrhnite skôr, než vysemení. Má rada vlhkú pôdu, preto pomôže zlepšenie odvodnenia. Holú pôdu mulčujte."
            )
        ),
        "oxalis" to Entry(
            "Wood Sorrel", Severity.AGGRESSIVE,
            en = Texts(
                "Spreads by both seeds and underground bulbils, very hard to fully remove",
                "Dig out carefully with every tiny bulbil — sieve the soil, as any left behind regrow. Mulch cleared areas and spot-treat regrowth with glyphosate. Very persistent, so keep at it.",
                "Mildly toxic if eaten in large amounts (high oxalic acid)"
            ),
            sk = Texts(
                "Šíri sa semenami aj podzemnými cibuľkami, veľmi ťažko sa odstraňuje úplne",
                "Opatrne vykopte aj s každou drobnou cibuľkou — pôdu preosejte, lebo každá zabudnutá znova vyrastie. Vyčistené miesta mulčujte a odrastky bodovo ošetrite glyfosátom. Je veľmi vytrvalá, nevzdávajte sa.",
                "Vo väčšom množstve mierne jedovatá pri zjedení (vysoký obsah kyseliny šťaveľovej)"
            )
        ),
        "veronica" to Entry(
            "Speedwell", Severity.MILD,
            en = Texts(
                "Creeping mat that smothers fine lawn grass, spreads by stems and seeds",
                "Rake up the creeping stems and hand-pull; scarify lawns to lift the mats. It resists most selective weedkillers, so feed and thicken the lawn to crowd it out."
            ),
            sk = Texts(
                "Plazivý koberec dusí jemný trávnik, šíri sa stonkami aj semenami",
                "Plazivé stonky vyhrabte a vytrhajte; trávnik vertikutujte, aby sa koberce nadvihli. Odoláva väčšine selektívnych herbicídov, preto trávnik prihnojte a zahustite, aby ju vytlačil."
            )
        ),
        "cardamine" to Entry(
            "Hairy Bittercress", Severity.MILD,
            en = Texts(
                "Explosive seed capsules disperse seeds 1m+ away, rapid lifecycle",
                "Hoe or hand-pull before the slender seed pods form (they fire seeds when touched). Mulch bare soil. Quick and easy to control if caught early."
            ),
            sk = Texts(
                "Výbušné tobolky rozstreľujú semená viac než 1 m ďaleko, veľmi rýchly životný cyklus",
                "Okopte alebo vytrhnite skôr, než sa vytvoria tenké struky (pri dotyku vystreľujú semená). Holú pôdu mulčujte. Pri včasnom zásahu rýchla a ľahká práca."
            )
        ),
        "lamium" to Entry(
            "Dead Nettle", Severity.MILD,
            en = Texts(
                "Spreads aggressively in garden beds via stems and seeds",
                "Hand-pull or hoe — it's shallow-rooted and lifts easily. Mulch beds to suppress seedlings."
            ),
            sk = Texts(
                "Agresívne sa šíri v záhonoch stonkami aj semenami",
                "Vytrhajte alebo okopte — má plytké korene a ľahko sa vytiahne. Záhony mulčujte proti semenáčikom."
            )
        ),
        "galium" to Entry(
            "Cleavers / Goosegrass", Severity.MILD,
            en = Texts(
                "Clings via tiny hooks, smothers other plants, very prolific seeder",
                "Pull whole plants before the sticky seeds form (they cling to clothing and fur and spread everywhere). Mulch bare ground. Easy to remove while young."
            ),
            sk = Texts(
                "Prichytáva sa drobnými háčikmi, dusí ostatné rastliny a hojne sa vysemeňuje",
                "Celé rastliny vytrhnite skôr, než sa vytvoria lepkavé semená (chytajú sa oblečenia aj srsti a šíria sa všade). Holú pôdu mulčujte. Mladé rastliny sa odstraňujú ľahko."
            )
        ),
        "senecio" to Entry(
            "Groundsel", Severity.MILD,
            en = Texts(
                "Year-round annual weed, seeds freely in all seasons",
                "Hoe or hand-pull before the fluffy seed heads open. Mulch bare soil — it seeds almost year-round, so stay on top of it.",
                "Toxic to horses, livestock and pets if eaten"
            ),
            sk = Texts(
                "Celoročná jednoročná burina, semení vo všetkých ročných obdobiach",
                "Okopte alebo vytrhnite skôr, než sa otvoria páperové semenné hlávky. Holú pôdu mulčujte — semení takmer celý rok, preto buďte dôslední.",
                "Jedovatý pre kone, hospodárske zvieratá aj domácich miláčikov"
            )
        ),
        "sonchus" to Entry(
            "Sowthistle", Severity.MILD,
            en = Texts(
                "Wind-dispersed seeds, fast growing annual taking over bare soil",
                "Hoe or pull young plants with the root before they flower. Wear gloves. Mulch bare ground to block seedlings."
            ),
            sk = Texts(
                "Semená roznáša vietor, rýchlo rastúca jednoročná burina obsadzujúca holú pôdu",
                "Mladé rastliny okopte alebo vytrhnite aj s koreňom pred kvitnutím. Noste rukavice. Holú pôdu mulčujte proti semenáčikom."
            )
        ),
        "urtica" to Entry(
            "Stinging Nettle", Severity.AGGRESSIVE,
            en = Texts(
                "Painful sting, spreads aggressively via underground rhizomes",
                "Wear thick gloves and long sleeves. Dig out the yellow underground rhizomes completely; repeated cutting weakens clumps over time. Treat regrowth with glyphosate.",
                "Stings on contact — wear gloves and cover skin"
            ),
            sk = Texts(
                "Bolestivo pŕhli a agresívne sa šíri podzemnými podzemkami",
                "Noste hrubé rukavice a dlhé rukávy. Úplne vykopte žlté podzemné podzemky; opakované kosenie trsy časom oslabí. Odrastky ošetrite glyfosátom.",
                "Pŕhli pri dotyku — noste rukavice a zakryte si pokožku"
            )
        ),
        "capsella" to Entry(
            "Shepherd's Purse", Severity.MILD,
            en = Texts(
                "Common in lawns and beds, germinates very early in spring",
                "Hoe or hand-pull before the heart-shaped seed pods form. Mulch bare soil. Simple to control if tackled early."
            ),
            sk = Texts(
                "Bežná v trávnikoch aj záhonoch, klíči veľmi skoro na jar",
                "Okopte alebo vytrhnite skôr, než sa vytvoria srdcovité struky. Holú pôdu mulčujte. Pri včasnom zásahu jednoduchá kontrola."
            )
        ),
        "poa" to Entry(
            "Annual Meadow Grass", Severity.MILD,
            en = Texts(
                "Self-seeds prolifically in lawns, dies in summer leaving bare patches",
                "Hand-pull tufts from lawns and beds before they seed. It's a grass, so lawn selective weedkillers won't touch it — thicken and feed the lawn to crowd it out instead."
            ),
            sk = Texts(
                "V trávniku sa hojne vysemeňuje a v lete odumiera, po čom ostávajú holé miesta",
                "Trsy vytrhávajte z trávnika aj záhonov skôr, než vysemenia. Je to tráva, takže selektívne trávnikové herbicídy na ňu nefungujú — trávnik radšej prihnojte a zahustite."
            )
        ),
        "bromus" to Entry(
            "Brome Grass", Severity.MILD,
            en = Texts(
                "Invasive annual grass that blends in with lawn until it seeds",
                "Pull or dig clumps before the seed heads ripen. It can't be selectively sprayed within a lawn — in beds and borders spot-treat with glyphosate."
            ),
            sk = Texts(
                "Invázna jednoročná tráva, ktorá v trávniku splýva, kým nevysemení",
                "Trsy vytrhnite alebo vykopte skôr, než dozrejú semenné klasy. V trávniku sa nedá selektívne postrekovať — v záhonoch bodovo použite glyfosát."
            )
        ),
        "elytrigia" to Entry(
            "Couch Grass", Severity.AGGRESSIVE,
            en = Texts(
                "Highly invasive perennial grass, spreads via underground rhizomes",
                "Dig out every white underground rhizome — each fragment regrows. Sieve the soil as you go. Treat regrowth foliage with glyphosate and repeat; mulch cleared ground."
            ),
            sk = Texts(
                "Vysoko invázna trváca tráva, šíri sa podzemnými podzemkami",
                "Vykopte každý biely podzemný podzemok — každý úlomok znova vyrastie. Pôdu pri práci preosievajte. Odrastené listy ošetrite glyfosátom a opakujte; vyčistenú pôdu mulčujte."
            )
        ),
        "elymus" to Entry(
            "Couch Grass", Severity.AGGRESSIVE,
            en = Texts(
                "Underground rhizomes are nearly impossible to fully remove",
                "Dig out every white underground rhizome — each fragment regrows. Sieve the soil as you go. Treat regrowth foliage with glyphosate and repeat; mulch cleared ground."
            ),
            sk = Texts(
                "Podzemné podzemky sa takmer nedajú úplne odstrániť",
                "Vykopte každý biely podzemný podzemok — každý úlomok znova vyrastie. Pôdu pri práci preosievajte. Odrastené listy ošetrite glyfosátom a opakujte; vyčistenú pôdu mulčujte."
            )
        ),
        "agrostis" to Entry(
            "Creeping Bent Grass", Severity.AGGRESSIVE,
            en = Texts(
                "Forms dense mats that suppress fine lawn grass",
                "Rake and scarify hard to lift the mats, then dig out patches. Overseed lawns with your chosen grass to outcompete it. In beds, spot-treat with glyphosate."
            ),
            sk = Texts(
                "Vytvára husté koberce, ktoré potláčajú jemný trávnik",
                "Dôkladne vyhrabte a vertikutujte, aby sa koberce nadvihli, potom vykopte ložiská. Trávnik dosejte želanou trávou, aby ho prerastla. V záhonoch bodovo použite glyfosát."
            )
        ),
        "trifolium" to Entry(
            "Clover", Severity.MILD,
            en = Texts(
                "Spreads aggressively, makes lawn uneven and attracts stinging insects near paths",
                "Rake up the runners and hand-pull. Clover thrives in poor soil, so feed the lawn with a nitrogen-rich fertiliser to suppress it. Spot-treat with a selective clover weedkiller if needed."
            ),
            sk = Texts(
                "Agresívne sa šíri, robí trávnik nerovným a pri chodníkoch priťahuje bodavý hmyz",
                "Vyhrabte poplazy a vytrhajte. Ďateline sa darí v chudobnej pôde, preto trávnik prihnojte dusíkatým hnojivom. V prípade potreby bodovo použite selektívny herbicíd na ďatelinu."
            )
        ),
        "medicago" to Entry(
            "Medick", Severity.MILD,
            en = Texts(
                "Annual weed that spreads quickly, competes with lawn grass",
                "Hoe or hand-pull before it seeds. Feed and thicken the lawn to crowd it out. Mulch beds to block seedlings."
            ),
            sk = Texts(
                "Jednoročná burina, ktorá sa rýchlo šíri a konkuruje trávniku",
                "Okopte alebo vytrhnite skôr, než vysemení. Trávnik prihnojte a zahustite, aby ju vytlačil. Záhony mulčujte proti semenáčikom."
            )
        ),
        "hieracium" to Entry(
            "Hawkweed", Severity.AGGRESSIVE,
            en = Texts(
                "Spreads via runners and wind seeds, forms invasive rosettes",
                "Dig out the rosettes with the root before the runners spread. Remove seed heads before they blow. Spot-treat regrowth with a selective broadleaf weedkiller."
            ),
            sk = Texts(
                "Šíri sa poplazmi aj semenami vo vetre, vytvára invázne ružice",
                "Ružice vykopte aj s koreňom skôr, než sa rozšíria poplazy. Semenné hlávky odstráňte, kým ich nerozfúka vietor. Odrastky bodovo ošetrite selektívnym herbicídom."
            )
        ),
        "hypochaeris" to Entry(
            "Cat's Ear", Severity.MILD,
            en = Texts(
                "Rosette weed resembling dandelion, deep taproots and wind seeds",
                "Lever out the whole taproot and rosette with a hand fork. Remove flower stems before they seed. Spot-treat lawns with a selective broadleaf weedkiller."
            ),
            sk = Texts(
                "Ružicová burina podobná púpave, hlboké kolové korene a semená roznášané vetrom",
                "Celý kolový koreň aj ružicu vypáčte ručnou vidličkou. Kvetné stonky odstráňte pred vysemenením. Na trávniku bodovo použite selektívny herbicíd."
            )
        ),
        "leontodon" to Entry(
            "Hawkbit", Severity.MILD,
            en = Texts(
                "Dandelion-like rosette suppresses grass, spreads via wind",
                "Dig out the taproot and rosette. Remove seed heads promptly. A selective broadleaf weedkiller works well on lawn infestations."
            ),
            sk = Texts(
                "Ružica podobná púpave potláča trávu, šíri sa vetrom",
                "Vykopte kolový koreň aj ružicu. Semenné hlávky odstraňujte ihneď. Na zaburinený trávnik dobre funguje selektívny herbicíd."
            )
        ),
        "achillea" to Entry(
            "Yarrow", Severity.AGGRESSIVE,
            en = Texts(
                "Spreads via rhizomes, can take over lawns and flower beds",
                "Dig out the creeping rhizomes fully. Rake to lift the ferny foliage before mowing. Spot-treat regrowth with a selective broadleaf weedkiller."
            ),
            sk = Texts(
                "Šíri sa podzemkami, dokáže ovládnuť trávnik aj záhony",
                "Plazivé podzemky vykopte celé. Pred kosením papraďovité listy vyhrabte. Odrastky bodovo ošetrite selektívnym herbicídom."
            )
        ),
        "ranunculus" to Entry(
            "Creeping Buttercup", Severity.AGGRESSIVE,
            en = Texts(
                "Spreads via runners (stolons) that invade lawns and borders rapidly",
                "Dig out the plant along with its rooting runners. It loves wet ground, so improving drainage helps a lot. Spot-treat regrowth with a selective broadleaf weedkiller.",
                "Toxic to pets and livestock if eaten; sap can irritate skin"
            ),
            sk = Texts(
                "Šíri sa poplazmi, ktoré rýchlo prenikajú do trávnika aj záhonov",
                "Rastlinu vykopte aj so zakorenenými poplazmi. Má rada mokrú pôdu, takže veľmi pomôže zlepšenie odvodnenia. Odrastky bodovo ošetrite selektívnym herbicídom.",
                "Pri zjedení jedovatý pre domáce aj hospodárske zvieratá; šťava môže dráždiť pokožku"
            )
        ),
        "ficaria" to Entry(
            "Lesser Celandine", Severity.AGGRESSIVE,
            en = Texts(
                "Underground tubers and bulbils carpet entire areas, very hard to eradicate",
                "Dig out carefully with every tiny tuber and bulbil — sieve the soil, as any left behind regrow. Smother cleared ground with thick mulch. Very persistent; treat foliage with glyphosate if needed.",
                "Toxic if eaten raw — keep away from pets & children"
            ),
            sk = Texts(
                "Podzemné hľuzky a cibuľky pokryjú celé plochy, veľmi ťažko sa odstraňuje",
                "Opatrne vykopte s každou drobnou hľuzkou a cibuľkou — pôdu preosejte, lebo každá zabudnutá znova vyrastie. Vyčistenú plochu prikryte hrubým mulčom. Je veľmi vytrvalý; listy prípadne ošetrite glyfosátom.",
                "V surovom stave jedovatý pri zjedení — držte mimo dosahu detí a zvierat"
            )
        ),
        "geum" to Entry(
            "Wood Avens / Herb Bennet", Severity.MILD,
            en = Texts(
                "Self-seeds prolifically in borders, hooks seeds onto clothing and animals",
                "Hand-pull or dig out the root before the hooked seeds form. Mulch beds to block seedlings. Easy to control if caught before it seeds."
            ),
            sk = Texts(
                "V záhonoch sa hojne vysemeňuje, semená s háčikmi sa chytajú oblečenia aj zvierat",
                "Vytrhnite alebo vykopte aj s koreňom skôr, než sa vytvoria háčikovité semená. Záhony mulčujte proti semenáčikom. Pri zásahu pred vysemenením ľahká kontrola."
            )
        ),
        "lolium" to Entry(
            "Ryegrass (Weed Type)", Severity.MILD,
            en = Texts(
                "Can outcompete fine-leaved lawn grasses when growing as a weed",
                "Hand-pull clumps from fine lawns and beds — it's a grass, so lawn selective weedkillers won't touch it. Overseed with your desired grass to blend it out."
            ),
            sk = Texts(
                "Ako burina dokáže vytlačiť jemnolisté trávnikové trávy",
                "Trsy vytrhávajte z jemných trávnikov a záhonov — je to tráva, takže selektívne trávnikové herbicídy nefungujú. Dosejte želanú trávu, aby splynul."
            )
        ),
        "bellis" to Entry(
            "Common Daisy", Severity.MILD,
            en = Texts(
                "Forms mats that crowd out lawn grass (can look decorative but spreads aggressively)",
                "Lever out the rosettes with a hand fork or daisy grubber. Mow regularly and feed the lawn to keep it dense. A selective broadleaf weedkiller controls heavy infestations."
            ),
            sk = Texts(
                "Vytvára koberce, ktoré vytláčajú trávnik (vyzerá pekne, ale šíri sa agresívne)",
                "Ružice vypáčte ručnou vidličkou alebo vypichovačom. Pravidelne koste a trávnik prihnojujte, aby ostal hustý. Silné zamorenie rieši selektívny herbicíd."
            )
        ),
        "heracleum" to Entry(
            "Hogweed / Giant Hogweed", Severity.NOTIFIABLE,
            en = Texts(
                "Giant hogweed is a dangerous invasive — sap causes severe burns; regular hogweed also spreads aggressively",
                "Do NOT touch it or strim it — the sap plus sunlight causes severe skin burns and blistering. For giant hogweed, call a professional. Small common hogweed can be dug out wearing full covering, gloves and eye protection.",
                "Sap causes severe burns and blisters in sunlight — do NOT touch bare-skinned; dangerous to children and pets"
            ),
            sk = Texts(
                "Boľševník obrovský je nebezpečná invázna rastlina — šťava spôsobuje ťažké popáleniny; aj domáce druhy sa šíria agresívne",
                "NEDOTÝKAJTE sa ho a nekoste krovinorezom — šťava spolu so slnečným svetlom spôsobuje ťažké popáleniny a pľuzgiere. Na boľševník obrovský zavolajte odborníka. Menšie domáce druhy možno vykopať v úplne zakrytom oblečení, rukaviciach a s ochranou očí.",
                "Šťava na slnku spôsobuje ťažké popáleniny a pľuzgiere — NEDOTÝKAJTE sa holou pokožkou; nebezpečný pre deti aj zvieratá"
            )
        ),
        "equisetum" to Entry(
            "Horsetail / Marestail", Severity.AGGRESSIVE,
            en = Texts(
                "Ancient deep-rooted weed, rhizomes reach 2m down, extremely persistent",
                "You can't dig it out fully — roots go metres deep. Keep cutting shoots to starve it and improve drainage. Crush the stems then treat with a glyphosate-based weedkiller; expect a multi-year effort.",
                "Toxic to horses and livestock if eaten in quantity"
            ),
            sk = Texts(
                "Prastará burina s koreňmi do hĺbky 2 m, mimoriadne vytrvalá",
                "Úplne sa vykopať nedá — korene siahajú metre hlboko. Výhonky opakovane odstraňujte, aby ste ju vyčerpali, a zlepšite odvodnenie. Stonky pomliaždite a ošetrite glyfosátovým herbicídom; počítajte s viacročným úsilím.",
                "Vo väčšom množstve jedovatá pre kone a hospodárske zvieratá"
            )
        ),
        "glechoma" to Entry(
            "Ground Ivy / Creeping Charlie", Severity.AGGRESSIVE,
            en = Texts(
                "Creeping stems root at every node and quickly carpet lawns and beds",
                "Rake up the runners and hand-pull after rain — every rooted node must come out. Improve lawn drainage and light. Spot-treat with a selective broadleaf weedkiller in autumn."
            ),
            sk = Texts(
                "Plazivé stonky korenia v každom kolienku a rýchlo pokryjú trávnik aj záhony",
                "Poplazy vyhrabte a po daždi vytrhajte — von musí každé zakorenené kolienko. Zlepšite odvodnenie a presvetlenie trávnika. Na jeseň bodovo ošetrite selektívnym herbicídom."
            )
        ),
        "alliaria" to Entry(
            "Garlic Mustard", Severity.AGGRESSIVE,
            en = Texts(
                "Invasive biennial that outcompetes native plants and self-seeds heavily",
                "Hand-pull before it flowers in its second year, taking the S-shaped root. Bag pulled plants — they can still set seed. Recheck the area for two seasons."
            ),
            sk = Texts(
                "Invázna dvojročná rastlina, ktorá vytláča pôvodné druhy a silno sa vysemeňuje",
                "Vytrhnite pred kvitnutím v druhom roku aj s esovitým koreňom. Vytrhnuté rastliny dajte do vreca — semená dokážu dozrieť aj po vytrhnutí. Miesto kontrolujte dve sezóny."
            )
        ),
        "anthriscus" to Entry(
            "Cow Parsley", Severity.MILD,
            en = Texts(
                "Rapid spring grower that swamps borders and self-seeds prolifically",
                "Dig out the taproot of young plants; cut established stands before flowering to stop seeding. Repeated cutting exhausts the root over a couple of seasons.",
                "Easily confused with toxic hemlock — wear gloves and don't eat any part"
            ),
            sk = Texts(
                "Rýchlo rastie na jar, zahltí záhony a hojne sa vysemeňuje",
                "Mladým rastlinám vykopte kolový koreň; staršie porasty pokoste pred kvitnutím, aby nevysemenili. Opakované kosenie koreň za pár sezón vyčerpá.",
                "Ľahko zameniteľná s jedovatým bolehlavom — noste rukavice a nič z nej nejedzte"
            )
        ),
        "epilobium" to Entry(
            "Willowherb", Severity.MILD,
            en = Texts(
                "Wind-blown seeds colonise any bare soil, roots snap when pulled",
                "Pull young plants when the soil is moist so the root comes out whole. Never let it flower — one plant releases tens of thousands of seeds. Mulch beds thickly."
            ),
            sk = Texts(
                "Semená vo vetre obsadia každú holú pôdu, korene sa pri vytrhávaní lámu",
                "Mladé rastliny vytrhávajte z vlhkej pôdy, aby koreň vyšiel celý. Nenechajte ju zakvitnúť — jedna rastlina uvoľní desaťtisíce semien. Záhony hrubo mulčujte."
            )
        ),
        "conyza" to Entry(
            "Fleabane", Severity.MILD,
            en = Texts(
                "Tall annual producing huge numbers of wind-dispersed seeds, herbicide-resistant strains exist",
                "Hand-pull or hoe while it is a small rosette — mature plants resist many weedkillers. Never let it set seed; mulch bare ground."
            ),
            sk = Texts(
                "Vysoká jednoročná burina s obrovským množstvom semien roznášaných vetrom, existujú kmene odolné voči herbicídom",
                "Vytrhnite alebo okopte, kým je malou ružicou — dospelé rastliny odolávajú mnohým herbicídom. Nikdy ju nenechajte vysemeniť; holú pôdu mulčujte."
            )
        ),
        "amaranthus" to Entry(
            "Pigweed", Severity.AGGRESSIVE,
            en = Texts(
                "Fast-growing annual, one plant can shed over 100,000 seeds",
                "Hoe or pull seedlings early — plants outgrow crops within weeks. Remove before flowering and mulch. Persistent seed bank means checking for several seasons."
            ),
            sk = Texts(
                "Rýchlo rastúca jednoročná burina, jedna rastlina uvoľní vyše 100 000 semien",
                "Semenáčiky okopte alebo vytrhnite čo najskôr — rastliny prerastú výsadbu za pár týždňov. Odstráňte pred kvitnutím a mulčujte. Zásoba semien v pôde vydrží, kontrolujte niekoľko sezón."
            )
        ),
        "portulaca" to Entry(
            "Purslane", Severity.MILD,
            en = Texts(
                "Succulent mat-former; stem fragments re-root and seeds survive decades",
                "Hoe on a hot dry day and remove the pieces — fragments left on moist soil re-root. Don't compost it. Mulch to block the long-lived seeds."
            ),
            sk = Texts(
                "Sukulentná kobercová burina; úlomky stoniek znova korenia a semená prežijú desaťročia",
                "Okopte v horúci suchý deň a kúsky pozbierajte — úlomky na vlhkej pôde znova zakorenia. Nekompostujte ju. Mulčujte proti dlhovekým semenám."
            )
        ),
        "setaria" to Entry(
            "Foxtail Grass", Severity.MILD,
            en = Texts(
                "Annual grass whose bristly seed heads dominate thin lawns in summer",
                "Pull or dig clumps before seed heads form. Mow high and overseed to thicken the lawn; a spring pre-emergent herbicide stops the seeds germinating.",
                "Bristly seed heads can lodge in pets' paws, ears and noses"
            ),
            sk = Texts(
                "Jednoročná tráva, ktorej štetinaté klasy v lete ovládnu riedky trávnik",
                "Trsy vytrhnite alebo vykopte pred vytvorením klasov. Koste vyššie a dosievajte, aby trávnik zhustol; jarný preemergentný herbicíd zastaví klíčenie semien.",
                "Štetinaté klasy sa môžu zabodnúť psom a mačkám do labiek, uší či nosa"
            )
        ),
        "echinochloa" to Entry(
            "Barnyard Grass", Severity.MILD,
            en = Texts(
                "Vigorous annual grass of damp, disturbed ground; heavy seeder",
                "Pull or hoe young clumps before they seed, ideally when soil is moist. Improve drainage and keep grass dense — it only invades thin, wet patches."
            ),
            sk = Texts(
                "Bujná jednoročná tráva vlhkej narušenej pôdy; silno semení",
                "Mladé trsy vytrhnite alebo okopte skôr, než vysemenia, najlepšie z vlhkej pôdy. Zlepšite odvodnenie a udržujte hustý porast — preniká len do riedkych vlhkých miest."
            )
        ),
        "mercurialis" to Entry(
            "Dog's Mercury", Severity.MILD,
            en = Texts(
                "Shade-loving carpeting weed that spreads by rhizomes under hedges and trees",
                "Dig out the shallow rhizomes with a fork and repeat as regrowth appears. Wear gloves. Planting dense ground cover shades it out over time.",
                "Poisonous to people and pets if eaten"
            ),
            sk = Texts(
                "Tieňomilná kobercová burina, ktorá sa šíri podzemkami pod živými plotmi a stromami",
                "Plytké podzemky vykopte vidlami a zásah opakujte pri odrastaní. Noste rukavice. Hustá pôdopokryvná výsadba ju časom zatieni.",
                "Jedovatá pre ľudí aj zvieratá pri zjedení"
            )
        ),
    )

    fun identify(scientificName: String): WeedInfo? {
        val genus = scientificName.lowercase().trim().split(" ").firstOrNull() ?: return null
        val entry = weeds[genus] ?: return null
        val texts = if (Locale.getDefault().language == "sk") entry.sk else entry.en
        return WeedInfo(entry.commonName, texts.reason, texts.removal, entry.severity, texts.hazard)
    }
}
