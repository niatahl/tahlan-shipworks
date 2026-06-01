package org.niatahl.tahlan.utils

object TahlanIDs {
    const val LEGIO = "tahlan_legioinfernalis"
    const val BLACKWATCH = "tahlan_legioelite"
    const val DAEMONS = "tahlan_legiodaemons"
    const val NEURALLINK_COMM = "tahlan_neurallink"
    const val DIGITAL_SOUL = "tahlan_digitalSoul"
    const val ALLMOTHER = "tahlan_allmother"

    const val CORE_DAEMON = "tahlan_daemoncore"
    const val CORE_ARCHDAEMON = "tahlan_archdaemoncore"
    const val DAEMONIC_HEART = "tahlan_daemoncore"
    const val HEL_CARAPACE = "tahlan_daemonarmor"

    const val TAG_DAEMON = "tahlan_daemon"
    const val TAG_DAEMONIZE = "tahlan_daemonize"

    // Illustrious recovery quest
    const val ILLUSTRIOUS_HULL = "tahlan_illustrious"
    // global latch: set once the Illustrious has been salvaged; gates the quest from ever re-offering
    const val ILLUSTRIOUS_RECOVERED = "\$tahlan_illustriousRecovered"
    // global latch: set once Louisa's one-time restoration has been paid for
    const val ILLUSTRIOUS_RESTORED = "\$tahlan_illustriousRestored"
    // member tag stamped on the recovered hull so the restoration step can find it again
    const val ILLUSTRIOUS_MEMBER_TAG = "tahlan_illustriousRecoveredMember"
    // global mission reference / in-progress flag (vanilla HubMission convention)
    const val ILLUSTRIOUS_REF = "\$tahlan_illustrious_ref"
    const val ILLUSTRIOUS_IN_PROGRESS = "\$tahlan_illustrious_inProgress"
    // per-hop cache-salvage flags that advance the clue chain
    const val ILLUSTRIOUS_CLUE1 = "\$tahlan_illustrious_clue1Gained"
    const val ILLUSTRIOUS_CLUE2 = "\$tahlan_illustrious_clue2Gained"
    const val ILLUSTRIOUS_CLUE3 = "\$tahlan_illustrious_clue3Gained"

    // Traumtänzer salvage dilemma (capstone reward fork: ship XOR crew/Henrietta)
    // variant id of the placed derelict (see DerelictsSpawnScript)
    const val TRAUM_VARIANT = "tahlan_schneefall_traum_albtraum"
    // one-time global latch: set once the dilemma has been resolved either way
    const val TRAUM_RESOLVED = "\$tahlan_traumResolved"
    // OPTIONAL flavor-only gate for richer questline-aware text. Confirm the real
    // Rosenritter-questline-complete flag during apply; wrong/absent flag just means
    // the cold text is shown (harmless). Prose is author-rewritten.
    const val TRAUM_QUEST_COMPLETE = "\$tahlan_regaliaQuestComplete"

    // Siege system
    const val SIEGE_CONDITION_ID = "tahlan_siegecondition"
    const val SIEGE_MANAGER_KEY = "\$tahlan_siegeManager"

    // Legio "awoken" / daemonic-incursion flag (set by the natural incursion or by gifting them a planetkiller)
    const val TRIGGERED = "\$tahlan_triggered"

    // Planetkiller handed to the Legio (the "incredibly stupid" path) and its delayed doomsday strike
    const val GAVE_PK_TO_LEGIO = "\$tahlan_gavePKtoLegio"
    const val PK_STRIKE_ARMED = "\$tahlan_pkStrikeArmed"
    const val PK_STRIKE_FIRED = "\$tahlan_pkStrikeFired"
    const val PK_STRIKE_FLEET = "\$tahlan_pkStrikeFleet"
    // Set when the strike resolves by interception — lifts the natural-incursion suppression so the
    // Legio still awakens eventually (a gifted planetkiller only delays the betrayal, never cancels it).
    const val PK_STRIKE_RESOLVED = "\$tahlan_pkStrikeResolved"

    // SOTF references
    const val SOTF_NIGHTINGALE = "sotf_nightingale"
    const val SOTF_SIERRA = "sotf_sierra"
    const val SOTF_CYWAR = "sotf_cyberwarfare"
    const val SOTF_BARROW = "sotf_barrow"
    const val SOTF_SIRIUS = "sotf_sirius"
    const val SOTF_REVERIE = "sotf_reverie"
    const val SOTF_SIRIUS_MIMIC = "sotf_sirius_mimic"
}