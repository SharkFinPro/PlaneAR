package edu.osu.t22.planear.achievements

/**
 * Definition of a single achievement.
 */
data class AchievementDef(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val requirement: String
)

/**
 * All 25 achievements in the app.
 */
val ALL_ACHIEVEMENTS: List<AchievementDef> = listOf(
    AchievementDef(
        id = "first_contact",
        name = "First Contact",
        emoji = "✈️",
        description = "Your AR journey begins — lock onto your first plane.",
        requirement = "Track 1 aircraft"
    ),
    AchievementDef(
        id = "sky_watcher",
        name = "Sky Watcher",
        emoji = "🔭",
        description = "You're getting the hang of scanning the skies.",
        requirement = "Track 50 unique ICAO addresses"
    ),
    AchievementDef(
        id = "century_club",
        name = "Century Club",
        emoji = "🌐",
        description = "You're basically an air traffic controller at this point.",
        requirement = "Track 100 unique ICAO addresses lifetime"
    ),
    AchievementDef(
        id = "speed_demon",
        name = "Speed Demon",
        emoji = "⚡",
        description = "That thing is hauling.",
        requirement = "Spot an aircraft with ground speed over 550 knots"
    ),
    AchievementDef(
        id = "stratosphere_club",
        name = "Stratosphere Club",
        emoji = "🏔️",
        description = "Way up where the air is thin.",
        requirement = "Spot an aircraft above 40,000 ft"
    ),
    AchievementDef(
        id = "tower_of_signals",
        name = "Tower of Signals",
        emoji = "📡",
        description = "So many planes, so little sky.",
        requirement = "Track 10 aircraft simultaneously on screen"
    ),
    AchievementDef(
        id = "night_owl",
        name = "Night Owl",
        emoji = "🌙",
        description = "The skies never sleep, and neither do you.",
        requirement = "Track an aircraft between midnight and 4 AM local time"
    ),
    AchievementDef(
        id = "easy_rider",
        name = "Easy Rider",
        emoji = "🐢",
        description = "Not everyone's in a rush.",
        requirement = "Spot an aircraft flying under 150 knots at altitude"
    ),
    AchievementDef(
        id = "loyal_spotter",
        name = "Loyal Spotter",
        emoji = "📍",
        description = "This is your patch of sky.",
        requirement = "Open the app and track at least 1 aircraft 7 days in a row"
    ),
    AchievementDef(
        id = "climber",
        name = "Climber",
        emoji = "📈",
        description = "Caught a plane punching hard through the sky.",
        requirement = "Spot an aircraft with a vertical rate above 3,000 ft/min"
    ),
    AchievementDef(
        id = "nose_diver",
        name = "Nose Diver",
        emoji = "📉",
        description = "Someone's in a hurry to get down.",
        requirement = "Spot an aircraft descending faster than 3,000 ft/min"
    ),
    AchievementDef(
        id = "low_rider",
        name = "Low Rider",
        emoji = "🌊",
        description = "Flying so low you could almost wave.",
        requirement = "Spot an airborne aircraft below 1,000 ft"
    ),
    AchievementDef(
        id = "dawn_patrol",
        name = "Dawn Patrol",
        emoji = "🌅",
        description = "Up before the sun, watching the early birds.",
        requirement = "Track an aircraft before 5 AM local time"
    ),
    AchievementDef(
        id = "dedicated",
        name = "Dedicated",
        emoji = "🏅",
        description = "Rain or shine, you're out here spotting.",
        requirement = "Track at least 1 aircraft 30 days in a row"
    ),
    AchievementDef(
        id = "ghost_signal",
        name = "Ghost Signal",
        emoji = "👻",
        description = "A plane with no callsign. Who are you?",
        requirement = "Spot an aircraft broadcasting no callsign"
    ),
    AchievementDef(
        id = "rush_hour",
        name = "Rush Hour",
        emoji = "🚦",
        description = "The sky is absolutely packed.",
        requirement = "Track 20 aircraft simultaneously on screen"
    ),
    AchievementDef(
        id = "heading_home",
        name = "Heading Home",
        emoji = "🏠",
        description = "Spotted a plane flying directly overhead.",
        requirement = "Track an aircraft within 5° of directly overhead"
    ),
    AchievementDef(
        id = "marathon_spotter",
        name = "Marathon Spotter",
        emoji = "🏃",
        description = "You've been at this for a while.",
        requirement = "Accumulate 24 total hours of in-app tracking time"
    ),
    AchievementDef(
        id = "frequent_flyer",
        name = "Frequent Flyer",
        emoji = "🎫",
        description = "You keep seeing this one. It's basically your plane now.",
        requirement = "Spot the same ICAO address on 5 separate days"
    ),
    AchievementDef(
        id = "icao_collector",
        name = "ICAO Collector",
        emoji = "🗂️",
        description = "Your logbook is getting seriously impressive.",
        requirement = "Track 500 unique ICAO addresses lifetime"
    ),
    AchievementDef(
        id = "flat_and_fast",
        name = "Flat and Fast",
        emoji = "➡️",
        description = "Cruising perfectly level at high speed — textbook flight.",
        requirement = "Spot an aircraft above 30,000 ft with vertical rate of 0 and speed over 400 knots"
    ),
    AchievementDef(
        id = "the_obsessed",
        name = "The Obsessed",
        emoji = "🛸",
        description = "At this point the sky is just your ceiling.",
        requirement = "Track 1,000 unique ICAO addresses lifetime"
    ),
    AchievementDef(
        id = "sharp_turn",
        name = "Sharp Turn",
        emoji = "↩️",
        description = "Caught a plane banking hard mid-flight.",
        requirement = "Spot an aircraft with a heading change of more than 90° within 30 seconds"
    ),
    AchievementDef(
        id = "holding_pattern",
        name = "Holding Pattern",
        emoji = "🔁",
        description = "Going nowhere fast — someone's waiting for clearance.",
        requirement = "Track an aircraft circling the same position for more than 3 minutes"
    ),
    AchievementDef(
        id = "continental_divide",
        name = "Continental Divide",
        emoji = "🗺️",
        description = "Your app has seen planes from all corners of the ICAO address space.",
        requirement = "Track aircraft whose ICAO addresses span all 5 major continental prefixes"
    )
)
