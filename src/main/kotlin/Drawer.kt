@file:Suppress("UnsafeCastFromDynamic", "unused")

package wonder.drawer

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import kotlin.math.abs
import kotlin.math.min

/**
 * Primary source for The Impossible Desk Drawer.
 * The checked-in runtime.js is the generated browser artifact. All experience
 * state, catalogue content, progression rules, controls, and reactions live
 * here so the project remains genuinely Kotlin-led.
 */
data class Artifact(
    val id: String,
    val title: String,
    val classification: String,
    val image: String,
    val description: String,
    val action: String,
    val observation: String,
    val atmosphere: String,
    val depthBand: DepthBand
)

enum class DepthBand(val label: String, val frequency: Double) {
    SHALLOW("SHALLOW INTERIOR", 150.0),
    WEATHER("BELOW WEATHER", 208.0),
    UNLICENSED("UNLICENSED DEPTH", 264.0)
}

enum class ViewState { LANDING, CABINET, RECORD, BOTTOM, ENDING }

data class DrawerState(
    var view: ViewState = ViewState.LANDING,
    var selected: Int = 0,
    val seen: MutableSet<String> = mutableSetOf(),
    val activated: MutableSet<String> = mutableSetOf(),
    var soundEnabled: Boolean = true,
    var dragStart: Double? = null,
    var dragOrigin: Int = 0
) {
    fun wrap(index: Int, count: Int): Int = (index % count + count) % count
    fun hasSeenEverything(count: Int): Boolean = seen.size == count
    fun hasActivatedEverything(count: Int): Boolean = activated.size == count
}

object Catalogue {
    val artifacts = listOf(
        Artifact(
            id = "weather-letter",
            title = "Weather Letter",
            classification = "CORRESPONDENCE / CLIMATE",
            image = "assets/weather-letter.webp",
            description = "A letter whose contents refused to remain figurative. " +
                "It rains upward when somebody nearby is avoiding a reply.",
            action = "BREAK THE WAX SEAL",
            observation = "The storm recognizes your name and immediately becomes more specific.",
            atmosphere = "#55758d",
            depthBand = DepthBand.SHALLOW
        ),
        Artifact(
            id = "staircase-key",
            title = "Staircase Key",
            classification = "ACCESS / VERTICAL",
            image = "assets/staircase-key.webp",
            description = "A key for a lock located several floors inside itself. " +
                "Every step is smaller than the last, but the landing never gets closer.",
            action = "CLIMB THREE STEPS",
            observation = "You arrive one inch above where you started, carrying a memory of red carpet.",
            atmosphere = "#7c432f",
            depthBand = DepthBand.SHALLOW
        ),
        Artifact(
            id = "moon-teacup",
            title = "Moon in a Teacup",
            classification = "REFRESHMENTS / NIGHT",
            image = "assets/moon-teacup.webp",
            description = "The night shift has been steeping since 11:42 p.m. " +
                "The stars are decorative, but the moon appears fully caffeinated.",
            action = "TAKE ONE SIP",
            observation = "For a moment, every shadow in the room points toward tomorrow.",
            atmosphere = "#27365c",
            depthBand = DepthBand.WEATHER
        ),
        Artifact(
            id = "forest-file",
            title = "The Green File",
            classification = "RECORDS / OVERGROWN",
            image = "assets/forest-file.webp",
            description = "Filed under administrative growth. Each tab opens onto a different path; " +
                "none lead back to the cabinet where it was found.",
            action = "FOLLOW THE SMALLEST PATH",
            observation = "A mushroom stamps your paperwork APPROVED and refuses to elaborate.",
            atmosphere = "#58412e",
            depthBand = DepthBand.WEATHER
        ),
        Artifact(
            id = "koi-drawer",
            title = "Aquatic Drawer",
            classification = "FURNITURE / TIDAL",
            image = "assets/koi-drawer.webp",
            description = "Its waterline remains level even when the drawer is tilted. " +
                "The koi is believed to be responsible for all missing paper clips.",
            action = "ASK THE KOI WHAT IT SAW",
            observation = "The koi circles twice, which is either testimony or excellent legal advice.",
            atmosphere = "#315b65",
            depthBand = DepthBand.UNLICENSED
        ),
        Artifact(
            id = "paper-door",
            title = "Door of Pending Matters",
            classification = "EXIT / UNCONFIRMED",
            image = "assets/paper-door.webp",
            description = "Bound from documents that were never quite finished. " +
                "The stair beyond it rises through every margin somebody left blank.",
            action = "KNOCK ON THE MARGIN",
            observation = "A smaller version of your knock answers from three pages ahead.",
            atmosphere = "#8a5135",
            depthBand = DepthBand.UNLICENSED
        )
    )
}

private fun query(selector: String): HTMLElement =
    document.querySelector(selector) as? HTMLElement
        ?: error("Missing required drawer element: $selector")

private fun HTMLElement.show(displayClass: String = "visible") {
    classList.add(displayClass)
    setAttribute("aria-hidden", "false")
}

private fun HTMLElement.hide(displayClass: String = "visible") {
    classList.remove(displayClass)
    setAttribute("aria-hidden", "true")
}

class SoundEngine {
    private var context: dynamic = null
    var enabled: Boolean = true

    fun cue(frequency: Double, duration: Double = .12, waveform: String = "sine") {
        if (!enabled) return
        if (context == null) {
            val ctor = js("window.AudioContext || window.webkitAudioContext")
            context = js("new ctor()")
        }
        val oscillator = context.createOscillator()
        val gain = context.createGain()
        oscillator.type = waveform
        oscillator.frequency.value = frequency
        gain.gain.setValueAtTime(.035, context.currentTime)
        gain.gain.exponentialRampToValueAtTime(.001, context.currentTime + duration)
        oscillator.connect(gain)
        gain.connect(context.destination)
        oscillator.start()
        oscillator.stop(context.currentTime + duration)
    }

    fun arrival(depth: DepthBand) = cue(depth.frequency, .18, "triangle")
    fun confirmation() = cue(440.0, .5, "sine")
    fun threshold() = cue(70.0, 1.6, "sine")
}

class CabinetRenderer(
    private val state: DrawerState,
    private val catalogue: List<Artifact>,
    private val sound: SoundEngine
) {
    private val landing = query("#landing")
    private val cabinet = query("#cabinet")
    private val rail = query("#rail")
    private val record = query("#record")
    private val bottom = query("#bottom")
    private val ending = query("#ending")

    fun buildCards(onOpen: (Int) -> Unit) {
        rail.innerHTML = catalogue.mapIndexed { index, artifact ->
            """
            <button class="artifact" data-index="$index" type="button"
                    aria-label="Open ${artifact.title}">
              <img src="${artifact.image}" alt="">
              <span class="artifact-meta">
                <span>OBJECT ${(index + 1).toString().padStart(2, '0')}
                  <h3>${artifact.title}</h3>
                </span>
                <b>↗</b>
              </span>
            </button>
            """.trimIndent()
        }.joinToString("")

        rail.querySelectorAll(".artifact").asList().forEach { node ->
            val card = node as HTMLElement
            card.addEventListener("click", {
                onOpen(card.dataset["index"]?.toIntOrNull() ?: 0)
            })
        }
    }

    fun enterCabinet() {
        sound.cue(100.0, .6, "triangle")
        landing.asDynamic().animate(
            arrayOf(
                js("({ opacity: 1 })"),
                js("({ opacity: 0, transform: 'scale(.97)' })")
            ),
            js("({ duration: 600, fill: 'forwards' })")
        )
        window.setTimeout({
            landing.style.display = "none"
            cabinet.show()
            state.view = ViewState.CABINET
            select(0)
        }, 580)
    }

    fun select(requested: Int) {
        state.selected = state.wrap(requested, catalogue.size)
        val width = min(window.innerWidth * .46, 570.0) + 28.0
        rail.style.transform = "translateX(${-state.selected * width}px)"
        query("#progress").style.width =
            "${((state.selected + 1).toDouble() / catalogue.size) * 100}%"
        refreshStatus()
        sound.cue(150.0 + state.selected * 28.0, .06, "triangle")
    }

    fun refreshStatus() {
        val selectedArtifact = catalogue[state.selected]
        query("#count").textContent =
            "${state.seen.size.toString().padStart(2, '0')} / ${catalogue.size.toString().padStart(2, '0')}"
        query("#depth").textContent = selectedArtifact.depthBand.label
    }

    fun openRecord(index: Int) {
        state.selected = state.wrap(index, catalogue.size)
        val artifact = catalogue[state.selected]
        state.seen += artifact.id

        (query("#recordImage") as HTMLImageElement).apply {
            src = artifact.image
            alt = artifact.title
        }
        query("#recordNumber").textContent =
            "OBJECT ${(state.selected + 1).toString().padStart(2, '0')} / ${catalogue.size}"
        query("#recordType").textContent = artifact.classification
        query("#recordTitle").textContent = artifact.title
        query("#recordDescription").textContent = artifact.description
        query("#observation").textContent = if (artifact.id in state.activated) {
            artifact.observation
        } else {
            "Observation pending."
        }

        (query("#activate") as HTMLButtonElement).apply {
            textContent = if (artifact.id in state.activated) "OBSERVATION RECORDED" else artifact.action
            disabled = artifact.id in state.activated
        }
        query(".record-visual").style.background =
            "radial-gradient(circle, ${artifact.atmosphere}, #171426 70%)"
        record.show("open")
        document.body?.style?.overflow = "hidden"
        state.view = ViewState.RECORD
        refreshStatus()
        sound.arrival(artifact.depthBand)
    }

    fun activateCurrent() {
        val artifact = catalogue[state.selected]
        state.activated += artifact.id
        query("#observation").textContent = artifact.observation
        (query("#activate") as HTMLButtonElement).apply {
            textContent = "OBSERVATION RECORDED"
            disabled = true
        }
        sound.confirmation()

        if (state.hasActivatedEverything(catalogue.size)) {
            query("#bottomMessage").textContent =
                "All six objects have testified. The paper door has stopped pretending to be locked."
            (query("#openDoor") as HTMLButtonElement).disabled = false
        }
    }

    fun closeRecord() {
        record.hide("open")
        document.body?.style?.overflow = ""
        state.view = ViewState.CABINET
        select(state.selected)

        if (state.hasSeenEverything(catalogue.size)) {
            bottom.show()
            window.setTimeout({
                bottom.asDynamic().scrollIntoView(js("({ behavior: 'smooth' })"))
            }, 300)
        }
    }

    fun revealEnding() {
        ending.show()
        document.body?.style?.overflow = "hidden"
        state.view = ViewState.ENDING
        sound.threshold()
    }

    fun dragStart(clientX: Double) {
        state.dragStart = clientX
        state.dragOrigin = state.selected
    }

    fun dragMove(clientX: Double) {
        val start = state.dragStart ?: return
        val delta = clientX - start
        val width = min(window.innerWidth * .46, 570.0) + 28.0
        rail.style.transform = "translateX(${-(state.dragOrigin * width) + delta}px)"
    }

    fun dragEnd(clientX: Double) {
        val start = state.dragStart ?: return
        val delta = clientX - start
        when {
            abs(delta) <= 70 -> select(state.dragOrigin)
            delta < 0 -> select(state.dragOrigin + 1)
            else -> select(state.dragOrigin - 1)
        }
        state.dragStart = null
    }
}

class DrawerController {
    private val state = DrawerState()
    private val sound = SoundEngine()
    private val renderer = CabinetRenderer(state, Catalogue.artifacts, sound)

    fun start() {
        renderer.buildCards(renderer::openRecord)

        query("#enter").addEventListener("click", { renderer.enterCabinet() })
        query("#heroObject").addEventListener("click", { renderer.enterCabinet() })
        query("#previous").addEventListener("click", { renderer.select(state.selected - 1) })
        query("#next").addEventListener("click", { renderer.select(state.selected + 1) })
        query("#recordClose").addEventListener("click", { renderer.closeRecord() })
        query("#activate").addEventListener("click", { renderer.activateCurrent() })
        query("#openDoor").addEventListener("click", { renderer.revealEnding() })
        query("#again").addEventListener("click", { window.location.reload() })

        query("#sound").addEventListener("click", { event -> toggleSound(event) })
        query(".rail-shell").addEventListener("pointerdown", { event ->
            renderer.dragStart((event as MouseEvent).clientX.toDouble())
        })
        window.addEventListener("pointermove", { event ->
            val mouse = event as MouseEvent
            renderer.dragMove(mouse.clientX.toDouble())
            updateAtmosphere(mouse)
        })
        window.addEventListener("pointerup", { event ->
            renderer.dragEnd((event as MouseEvent).clientX.toDouble())
        })
        window.addEventListener("keydown", { event -> keyboard(event as KeyboardEvent) })
    }

    private fun toggleSound(event: Event) {
        state.soundEnabled = !state.soundEnabled
        sound.enabled = state.soundEnabled
        val button = event.currentTarget as HTMLButtonElement
        button.textContent = "SOUND / ${if (state.soundEnabled) "ON" else "OFF"}"
        button.setAttribute("aria-pressed", state.soundEnabled.toString())
        if (state.soundEnabled) sound.cue(300.0, .1)
    }

    private fun keyboard(event: KeyboardEvent) {
        when {
            state.view == ViewState.RECORD && event.key == "Escape" -> renderer.closeRecord()
            state.view == ViewState.CABINET && event.key == "ArrowRight" -> renderer.select(state.selected + 1)
            state.view == ViewState.CABINET && event.key == "ArrowLeft" -> renderer.select(state.selected - 1)
        }
    }

    private fun updateAtmosphere(event: MouseEvent) {
        val x = event.clientX.toDouble() / window.innerWidth * 100
        val y = event.clientY.toDouble() / window.innerHeight * 100
        document.documentElement?.asDynamic()?.style?.setProperty("--mx", "$x%")
        document.documentElement?.asDynamic()?.style?.setProperty("--my", "$y%")
    }
}

fun main() {
    DrawerController().start()
}
