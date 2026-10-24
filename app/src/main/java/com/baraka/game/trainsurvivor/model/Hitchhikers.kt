package com.baraka.game.trainsurvivor.model

import com.baraka.game.trainsurvivor.data.TrainSoundCue

/**
 * How good a hitchhiker's weapon is this time. Rolled when they appear; better grades hit harder and faster, stay
 * longer, tip more and get [bonus] extra bounces / pierce.
 */
enum class Grade(
    val label: String,
    val color: Long,
    val damage: Float,
    val rate: Float,
    val stay: Float,
    val tip: Float,
    val weight: Int,
    val bonus: Int
) {
    JUNK("Junk", 0xFFB0B0B0, 0.6f, 1.25f, 0.8f, 0.5f, 38, 0),
    DECENT("Decent", 0xFF81C784, 1f, 1f, 1f, 1f, 38, 0),
    GREAT("Great", 0xFF4FC3F7, 1.5f, 0.85f, 1.2f, 2f, 18, 1),
    LEGENDARY("Legendary", 0xFFFFC107, 2.4f, 0.7f, 1.5f, 5f, 6, 2)
}

enum class Hair { SHORT, LONG, BUN, PIGTAILS }

/**
 * How a passenger's shot looks in flight. LINE is a plain tracer; every other style is a sprite that flies to the target (the hit
 * lands after [flight] seconds) and then fades over [life]. To give a weapon its own look: add a style here, draw it in
 * `render/TrainRenderer.kt` (drawShot) and set `shot = ...` on the hitchhiker.
 */
enum class ShotStyle(val flight: Float, val life: Float) {
    LINE(0f, 0.1f),
    BIRD(0.2f, 0.55f),
    PAN(0.22f, 0.55f),
    HARPOON(0.16f, 0.5f),
    BOX(0.3f, 0.75f),
    GUST(0.12f, 0.4f),
    SOUP(0.55f, 0.75f),
    SHELL(0.6f, 0.8f),
    CONFETTI(0.25f, 0.6f),
    BOOT(0f, 0.5f);

    /** How long the visual of a bounce or a pierce between two zombies lasts. */
    val hop: Float get() = life * 0.6f
}

/** The kind of burst a weapon leaves where it lands (the engine emits the matching particles). */
enum class Impact { SPARK, CONFETTI, FEATHERS, SOUP, EXPLOSION, AIR, BOX }

/** What a hitchhiker says at each moment. One line is picked at random. */
class HitchLines(val board: Array<String>, val kill: Array<String>, val hurt: Array<String>, val leave: Array<String>, val idle: Array<String>)

/**
 * A random passenger who rides the roof for a while and helps with a silly weapon. Behaviour is all flags and numbers
 * (bounces, pierce, splash, knock-back, slow, burn, train shield, rally), so a new passenger is one entry here plus its
 * prop in `render/HitchhikerArt.kt`. A [damage] of 0 means the weapon only applies its other effects.
 */
enum class Hitchhiker(
    val displayName: String,
    val title: String,
    val female: Boolean,
    val hair: Hair,
    val look: AvatarLook,
    val weapon: String,
    val legend: String,
    val damage: Float,
    val interval: Float,
    val range: Float,
    val tracerColor: Long,
    val tracerWidth: Float,
    val cue: TrainSoundCue,
    val lines: HitchLines,
    val kind: DamageKind = DamageKind.PHYSICAL,
    val bounces: Int = 0,
    val pierce: Int = 0,
    val splash: Float = 0f,
    val knock: Float = 0f,
    /** Speed multiplier (1 = none) and how long it lasts on zombies this weapon touches. */
    val slow: Float = 1f,
    val slowTime: Float = 0f,
    /** Burn damage per second, for three seconds. */
    val burn: Float = 0f,
    val flyerBonus: Float = 1f,
    val prefersFlyers: Boolean = false,
    val groundOnly: Boolean = false,
    /** Share of damage to the train this passenger soaks up (before grade). */
    val shield: Float = 0f,
    /** Fire-rate bonus for every gunner while aboard (before grade). */
    val rally: Float = 0f,
    /** Chance a shot is a gag that does nothing (the fisherman's boot). */
    val misfire: Float = 0f,
    /** How the shot looks in flight (and, for anything but a line, when the hit lands). */
    val shot: ShotStyle = ShotStyle.LINE,
    /**
     * A lingering puddle where the shot lands: its radius and seconds, the extra damage zombies standing in it take (0.3 = +30%),
     * the speed multiplier while they slip, and its colour. 0 radius = no puddle.
     */
    val puddle: Float = 0f,
    val puddleTime: Float = 0f,
    val puddleVuln: Float = 0f,
    val puddleSlow: Float = 1f,
    val puddleColor: Long = 0xFFFFB74D,
    val impact: Impact = Impact.SPARK
) {
    GERTRUDE(
        "Grandma Gertrude", "Retired baker", true, Hair.BUN,
        AvatarLook(shirt = 0xFF8E24AA, pants = 0xFF6D4C41, skin = 0xFFE8B98A, hair = 0xFFE0E0E0, width = 0.92f, height = 0.9f, accent = 0xFFFFFFFF),
        weapon = "Frying Pan", legend = "Manhole-Cover Pan",
        damage = 14f, interval = 1.3f, range = 330f, tracerColor = 0xFFB0BEC5, tracerWidth = 4f, cue = TrainSoundCue.TRAIN_HIT,
        lines = HitchLines(
            board = arrayOf("Back in my day we had PANS!", "Is this train going anywhere nice?"),
            kill = arrayOf("BONK!", "That's for your mother!", "Eat your greens!"),
            hurt = arrayOf("Oh, my knees!", "Mind the hairdo!"),
            leave = arrayOf("Call your grandmother!", "There's cake in the cupboard!"),
            idle = arrayOf("Anyone hungry?", "Those zombies need a haircut.")
        ),
        bounces = 2, shot = ShotStyle.PAN
    ),
    DAVE(
        "Dave the Fisherman", "Weekend angler", false, Hair.SHORT,
        AvatarLook(shirt = 0xFF0277BD, pants = 0xFF4E342E, hair = 0xFF5D4037, hat = HatStyle.CAP, hatColor = 0xFFFFC107, accent = 0xFFFFFFFF),
        weapon = "Harpoon Gun", legend = "Whale Harpoon",
        damage = 22f, interval = 1.6f, range = 520f, tracerColor = 0xFFE0E0E0, tracerWidth = 3f, cue = TrainSoundCue.RIFLE,
        lines = HitchLines(
            board = arrayOf("Nice train! Got any bait?", "I'm just here for the fishing."),
            kill = arrayOf("GOT A BIG ONE!", "Dinner's served!", "Fresh catch!"),
            hurt = arrayOf("Splash! I mean, ouch.", "That one bit back!"),
            leave = arrayOf("See ya! ...or a trout.", "The big one got away."),
            idle = arrayOf("You should see the one that got away.", "Fish don't have teeth like that.")
        ),
        pierce = 2, misfire = 0.1f, shot = ShotStyle.HARPOON
    ),
    MARCEL(
        "Marcel the Mime", "Street performer", false, Hair.SHORT,
        AvatarLook(shirt = 0xFFFAFAFA, pants = 0xFF212121, skin = 0xFFF5F5F5, hair = 0xFF212121, hat = HatStyle.CAP, hatColor = 0xFF212121, accent = 0xFF212121),
        weapon = "Invisible Wall", legend = "Invisible Fortress",
        damage = 0f, interval = 3.4f, range = 280f, tracerColor = 0xFF90CAF9, tracerWidth = 2f, cue = TrainSoundCue.CRATE,
        lines = HitchLines(
            board = arrayOf("*waves silently*", "*bows*"),
            kill = arrayOf("*takes a bow*", "*blows a kiss*"),
            hurt = arrayOf("*dramatic fall*", "*clutches chest*"),
            leave = arrayOf("*opens an invisible door*", "*climbs an invisible ladder*"),
            idle = arrayOf("*is trapped in a box*", "*pulls an invisible rope*")
        ),
        slow = 0.25f, slowTime = 2.5f, shield = 0.22f, impact = Impact.BOX, shot = ShotStyle.BOX
    ),
    SPARKY(
        "Sparky", "Kid with a leaf blower", true, Hair.PIGTAILS,
        AvatarLook(shirt = 0xFFFF7043, pants = 0xFF3949AB, skin = 0xFFD9A066, hair = 0xFF6D4C41, hat = HatStyle.BANDANA, hatColor = 0xFFFFEB3B, width = 0.78f, height = 0.8f, accent = 0xFFFFFFFF),
        weapon = "Leaf Blower", legend = "Hurricane Blower",
        damage = 4f, interval = 0.5f, range = 260f, tracerColor = 0xFFB3E5FC, tracerWidth = 8f, cue = TrainSoundCue.LASER,
        lines = HitchLines(
            board = arrayOf("Can I pull the whistle?", "Whoa, a real train!"),
            kill = arrayOf("WHOOSH!", "Blown away!", "Again! Again!"),
            hurt = arrayOf("I'm telling mom!", "Hey, that's not fair!"),
            leave = arrayOf("Best. Day. EVER.", "Bye! I'm keeping the whistle!"),
            idle = arrayOf("Are we there yet?", "Can I have a snack?")
        ),
        knock = 26f, impact = Impact.AIR, shot = ShotStyle.GUST
    ),
    PIERRE(
        "Chef Pierre", "Chef without a kitchen", false, Hair.SHORT,
        AvatarLook(shirt = 0xFFFFFFFF, pants = 0xFF37474F, hair = 0xFF212121, accent = 0xFFE53935),
        weapon = "Soup Cannon", legend = "Volcano Ladle",
        damage = 10f, interval = 1.5f, range = 330f, tracerColor = 0xFFFF8A65, tracerWidth = 5f, cue = TrainSoundCue.MORTAR,
        lines = HitchLines(
            board = arrayOf("Zis train needs more SALT!", "Where is zee kitchen?"),
            kill = arrayOf("Magnifique!", "Bon appetit!", "Too much pepper, no?"),
            hurt = arrayOf("Sacre bleu!", "My soufflé!"),
            leave = arrayOf("Bon appetit, mes amis!", "I leave you zee recipe!"),
            idle = arrayOf("Zis soup needs thyme.", "Who ordered zee zombie?")
        ),
        kind = DamageKind.FIRE, splash = 70f, burn = 6f, groundOnly = true, impact = Impact.SOUP,
        puddle = 90f, puddleTime = 6f, puddleVuln = 0.3f, puddleSlow = 0.5f, puddleColor = 0xFFFF8A65, shot = ShotStyle.SOUP
    ),
    BEA(
        "Pigeon Lady Bea", "Park bench regular", true, Hair.BUN,
        AvatarLook(shirt = 0xFF558B2F, pants = 0xFF4E342E, skin = 0xFFE8B98A, hair = 0xFFBDBDBD, hat = HatStyle.COWBOY, hatColor = 0xFF8D6E63, width = 0.95f, height = 0.92f, accent = 0xFF9E9E9E),
        weapon = "Pigeon Cage", legend = "Pigeon Squadron",
        damage = 8f, interval = 0.75f, range = 420f, tracerColor = 0xFFECEFF1, tracerWidth = 2f, cue = TrainSoundCue.HONK,
        lines = HitchLines(
            board = arrayOf("Coo coo, my darlings!", "Room for my birds?"),
            kill = arrayOf("Fly, my beauties!", "Good birdie!", "Coo!"),
            hurt = arrayOf("Not the hat!", "Leave my pigeons alone!"),
            leave = arrayOf("Pigeons, we're leaving!", "Coo-ee, everybody!"),
            idle = arrayOf("Bernard is being difficult.", "Who wants breadcrumbs?")
        ),
        bounces = 1, flyerBonus = 1.8f, prefersFlyers = true, impact = Impact.FEATHERS, shot = ShotStyle.BIRD
    ),
    CONFETTI(
        "Mr. Confetti", "Party planner", false, Hair.SHORT,
        AvatarLook(shirt = 0xFFE91E63, pants = 0xFF1565C0, hair = 0xFFFF9800, hat = HatStyle.BEANIE, hatColor = 0xFF7E57C2, width = 1.05f, accent = 0xFFFFEB3B),
        weapon = "Party Cannon", legend = "Parade Float",
        damage = 4f, interval = 0.9f, range = 380f, tracerColor = 0xFFFF4081, tracerWidth = 5f, cue = TrainSoundCue.HONK,
        lines = HitchLines(
            board = arrayOf("Is this the party train?!", "Did somebody say PARTY?"),
            kill = arrayOf("SURPRISE!", "Happy un-birthday!", "Pop!"),
            hurt = arrayOf("Not the balloons!", "My cake!"),
            leave = arrayOf("Party's over! ...Kidding!", "Don't forget the cake!"),
            idle = arrayOf("Who wants a balloon?", "Streamers make everything better.")
        ),
        splash = 90f, slow = 0.4f, slowTime = 2.5f, impact = Impact.CONFETTI, shot = ShotStyle.CONFETTI
    ),
    BRENDA(
        "General Brenda", "Retired general", true, Hair.BUN,
        AvatarLook(shirt = 0xFF33691E, pants = 0xFF3E2723, skin = 0xFFC68642, hair = 0xFF212121, hat = HatStyle.HELMET, hatColor = 0xFF558B2F, accent = 0xFFFFC107),
        weapon = "Mortar & Megaphone", legend = "Orbital Megaphone",
        damage = 30f, interval = 3.2f, range = 480f, tracerColor = 0xFFFFCC80, tracerWidth = 5f, cue = TrainSoundCue.MORTAR,
        lines = HitchLines(
            board = arrayOf("Soldiers! ...and train people!", "Eyes up, people!"),
            kill = arrayOf("Outstanding!", "Medals for everyone!", "That's how it's done!"),
            hurt = arrayOf("I've had worse Tuesdays.", "Is that all you've got?"),
            leave = arrayOf("Carry on, soldiers!", "At ease. I'm out."),
            idle = arrayOf("Stand up straight, wagons!", "Drop and give me twenty!")
        ),
        kind = DamageKind.EARTH, splash = 100f, groundOnly = true, rally = 0.22f, impact = Impact.EXPLOSION, shot = ShotStyle.SHELL
    );

    /** Name of the weapon at [grade]; legendary ones get their own grand name. */
    fun weaponName(grade: Grade): String = if (grade == Grade.LEGENDARY) legend else "${grade.label} $weapon"
}
