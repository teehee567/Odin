package me.odinmain.features.impl.skyblock

import me.odinmain.features.Module
import me.odinmain.features.settings.Setting.Companion.withDependency
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.features.settings.impl.HudSetting
import me.odinmain.utils.equalsOneOf
import me.odinmain.utils.render.RenderUtils
import me.odinmain.utils.render.mcTextAndWidth
import me.odinmain.utils.skyblock.LocationUtils
import me.odinmain.utils.skyblock.isHolding
import me.odinmain.utils.skyblock.skyblockID
import me.odinmain.utils.toFixed
import me.odinmain.utils.ui.Colors
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import kotlin.math.ceil

object AbilityTimers : Module(
    name = "Ability Timers",
    desc = "Provides timers for Wither Impact, Tactical Insertion, and Enrage."
) {
    private interface AbilityTimer {
        fun handleSoundEffect(packet: S29PacketSoundEffect) {}
        fun update() {}
        fun reset() {}
    }

    init {
        // Core
        onPacket<S29PacketSoundEffect> {
            WitherImpactTimer.handleSoundEffect(it)
            TacticalInsertionTimer.handleSoundEffect(it)
            EnrageTimer.handleSoundEffect(it)
        }

        onPacket<S32PacketConfirmTransaction> {
            WitherImpactTimer.update()
            TacticalInsertionTimer.update()
            EnrageTimer.update()
        }

        onWorldLoad {
            WitherImpactTimer.reset()
            TacticalInsertionTimer.reset()
            EnrageTimer.reset()
        }

        // Other
        onPacket<C08PacketPlayerBlockPlacement> {
            WitherImpactTimer.handleBlockPlacement(it)
        }
    }

    private object WitherImpactTimer : AbilityTimer {
        var ticks: Int = -1

        val hud by HudSetting("Wither Impact Hud", 10f, 10f, 1f, true) {
            if (ticks <= 0 && (hideWhenDone || !LocationUtils.isInSkyblock) && !it) return@HudSetting 0f to 0f
            val width = if (compact) 6f else 65f
            RenderUtils.drawText(text, width / 2f, 0f, 1f, Colors.WHITE, shadow = true, center = true)
            width to 12f
        }

        val compact: Boolean by BooleanSetting("Compact Mode", true, desc = "Compacts the Hud to just one character wide.").withDependency { witherHud.enabled }
        val hideWhenDone: Boolean by BooleanSetting("Hide When Ready", true, desc = "Hides the hud when the cooldown is over.").withDependency { witherHud.enabled }

        inline val text: String get() =
        if (compact) if (ticks <= 0) "§aR" else "${ticks.color(61, 21)}${ceil(ticks / 20f).toInt()}"
        else if (ticks <= 0) "§6Shield: §aReady" else "§6Shield: ${ticks.color(61, 21)}${(ticks / 20f).toFixed()}s"

        override fun handleSoundEffect(packet: S29PacketSoundEffect) {
            if (packet.soundName == "mob.zombie.remedy" && 
                packet.pitch == 0.6984127f && 
                packet.volume == 1f && 
                hud.enabled && 
                ticks != -1
            ) {
                ticks = 100
            }
        }

        override fun update() {
            if (ticks > 0 && hud.enabled) ticks--
        }

        override fun reset() {
            ticks = -1
        }

        fun handleBlockPlacement(packet: C08PacketPlayerBlockPlacement) {
            if (mc.thePlayer?.heldItem?.skyblockID?.equalsOneOf("ASTRAEA", "HYPERION", "VALKYRIE", "SCYLLA", "NECRON_BLADE") == false || ticks != -1) return
            ticks = 0
        }
    }

    private object TacticalInsertionTimer : AbilityTimer {
        var timer: Int = 0

        val hud by HudSetting("Tactical Insertion Hud", 10f, 10f, 1f, true) {
            if (tacTimer == 0 && !it) return@HudSetting 0f to 0f
            mcTextAndWidth("§6Tac: ${timer.color(40, 20)}${(timer / 20f).toFixed()}s", 1f, 1f, 1f, color = Colors.WHITE, center = false) + 2f to 12f
        }

        override fun handleSoundEffect(packet: S29PacketSoundEffect) {
            if (packet.soundName == "fire.ignite" && 
                packet.pitch == 0.74603176f && 
                packet.volume == 1f && 
                isHolding("TACTICAL_INSERTION") && 
                hud.enabled
            ) {
                timer = 60
            }
        }

        override fun update() {
            if (timer > 0 && hud.enabled) timer--
        }

        override fun reset() {
            timer = 0
        }
    }

    private object EnrageTimer : AbilityTimer {
        var timer: Int = 0

        val enrageHud by HudSetting("Enrage Hud", 10f, 10f, 1f, true) {
            if (timer == 0 && !it) return@HudSetting 0f to 0f
            mcTextAndWidth("§4Enrage: ${timer.color(80, 40)}${(timer / 20f).toFixed()}s", 0f, 0f, 1f, Colors.WHITE, center = false) + 2f to 12f
        }

        override fun handleSoundEffect(packet: S29PacketSoundEffect) {
            if (packet.soundName == "mob.zombie.remedy" && 
                packet.pitch == 1.0f && 
                packet.volume == 0.5f && 
                mc.thePlayer?.getCurrentArmor(0)?.skyblockID == "REAPER_BOOTS" &&
                mc.thePlayer?.getCurrentArmor(1)?.skyblockID == "REAPER_LEGGINGS" && 
                mc.thePlayer?.getCurrentArmor(2)?.skyblockID == "REAPER_CHESTPLATE" && 
                hud.enabled
            ) {
                timer = 120
            }
        }

        override fun update() {
            if (timer > 0 && hud.enabled) timer--
        }

        override fun reset() {
            timer = 0
        }
    }

    private fun Int.color(compareFirst: Int, compareSecond: Int): String {
        return when {
            this >= compareFirst-> "§e"
            this >= compareSecond -> "§6"
            else -> "§4"
        }
    }
}
