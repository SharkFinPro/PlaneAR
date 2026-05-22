package edu.osu.t22.planear.scenes.pages

import edu.osu.t22.planear.AppColors
import edu.osu.t22.planear.achievements.ALL_ACHIEVEMENTS
import edu.osu.t22.planear.achievements.AchievementStore
import edu.osu.t22.planear.graphicsEngine.EllipseMode
import edu.osu.t22.planear.graphicsEngine.GraphicsEngineWrapper
import edu.osu.t22.planear.graphicsEngine.RectMode
import edu.osu.t22.planear.graphicsEngine.TextAlignH
import edu.osu.t22.planear.graphicsEngine.TextAlignV
import edu.osu.t22.planear.scenes.SceneInfo
import edu.osu.t22.planear.scenes.SceneSwitcher

/**
 * Achievements page — 2-column scrollable grid of achievement cards.
 *
 * Layout inspired by the "My Honor" achievement screen:
 * - Header with title, streak counter, and progress
 * - 2-column grid of cards, each with name, large emoji, and status badge
 * - Vertical scrolling via drag gesture
 */
class AchievementsPage : Page {
    override val sceneId = SceneId.Achievements

    private var scrollOffset = 0f
    // testing variable for flight data sheet
    private var sheetShownOnStart = false

    override fun render(sceneInfo: SceneInfo, sceneSwitcher: SceneSwitcher) {
        val screenW  = sceneInfo.screenWidth
        val screenH  = sceneInfo.screenHeight - navHeight
        val gestures = sceneInfo.gestures
        val c        = AppColors.current

        val inputBlocked = false

        val margin    = screenW * 0.05f
        val gridW     = screenW - 2f * margin
        val gap       = 20f
        val cardW     = (gridW - gap) / 2f
        val cardH     = cardW * 1.15f
        val cornerR   = 20f
        val cols      = 2

        val headerH   = 320f    // space for the header section
        val rows      = (ALL_ACHIEVEMENTS.size + cols - 1) / cols
        val totalGridH = rows * (cardH + gap) - gap
        val totalContentH = headerH + totalGridH + 60f
        val maxScroll    = (totalContentH - screenH).coerceAtLeast(0f)

        // Handle scrolling
        if (!inputBlocked && gestures.isScrolling) {
            val pos = gestures.scrollPosition
            if (pos != null && pos.second < screenH) {
                scrollOffset += gestures.scrollDelta.second
            }
        }
        scrollOffset = scrollOffset.coerceIn(0f, maxScroll)

        val unlocked = AchievementStore.getUnlockedCount()
        val total    = ALL_ACHIEVEMENTS.size
        val streak   = AchievementStore.getCurrentStreak()

        if (!sheetShownOnStart) {
            FlightDetailSheet.open(
                Aircraft(
                    id             = "a1b2c3",
                    callsign       = "UAL1234",
                    type           = "Boeing 737-800",
                    registration   = "N12345",
                    altitudeSeaLevel = 35000.0,
                    groundSpeed    = 487.0,
                    headingDegrees = 270.0
                )
            )
            sheetShownOnStart = true
        }

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            rectMode(RectMode.CORNER)

            // Page background
            fill(c.background)
            rect(0, 0, screenW, screenH)

            // =====================================================================
            // Header area (scrolls with content)
            // =====================================================================
            val headerY = -scrollOffset

            // Title
            fill(c.textPrimary)
            textFont("roboto", 22)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text("Achievements", margin, headerY + 100f)

            // Progress badge: "X / 25"
            fill(c.accent)
            textFont("roboto", 13)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text("🏆  $unlocked / $total", margin, headerY + 145f)

            // Streak display (right side of header)
            if (streak > 0) {
                fill(c.textPrimary)
                textFont("roboto", 13)
                textAlign(TextAlignH.RIGHT, TextAlignV.BASELINE)
                text("🔥 $streak day streak", screenW - margin, headerY + 145f)
            }

            // Accent line under header
            fill(c.accent)
            rect(margin, headerY + 170f, gridW, 3f, 1.5f)

            // Tracking stats row
            val statsY = headerY + 210f
            fill(c.textSecondary)
            textFont("roboto", 11)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)

            val icaoCount = AchievementStore.uniqueIcaosSeen.size
            val hoursTracked = AchievementStore.totalTrackingMs / 3_600_000.0
            text("$icaoCount ICAOs tracked", margin, statsY)
            textAlign(TextAlignH.RIGHT, TextAlignV.BASELINE)
            text("${"%.1f".format(hoursTracked)}h total time", screenW - margin, statsY)

            // Section subtitle
            fill(c.textHint)
            textFont("roboto", 12)
            textAlign(TextAlignH.LEFT, TextAlignV.BASELINE)
            text("Tap to track your progress", margin, statsY + 50f)

            // =====================================================================
            // 2-column grid of achievement cards
            // =====================================================================
            val gridStartY = headerY + headerH

            for (i in ALL_ACHIEVEMENTS.indices) {
                val row = i / cols
                val col = i % cols
                val cardX = margin + col * (cardW + gap)
                val cardY = gridStartY + row * (cardH + gap)

                // Skip cards that are off-screen
                if (cardY + cardH < 0f || cardY > screenH) continue

                val ach   = ALL_ACHIEVEMENTS[i]
                val isUnlocked = AchievementStore.isUnlocked(ach.id)

                drawAchievementCard(
                    sceneInfo, cardX, cardY, cardW, cardH, cornerR,
                    ach.emoji, ach.name, ach.requirement, isUnlocked
                )
            }

            // =====================================================================
            // Scroll indicator (subtle fade at bottom when more content below)
            // =====================================================================
            if (scrollOffset < maxScroll - 10f) {
                val fadeH = 60f
                val fadeY = screenH - fadeH
                for (step in 0 until 10) {
                    val alpha = (step * 25).coerceAtMost(255)
                    fill(c.background, alpha)
                    rect(0f, fadeY + step * (fadeH / 10f), screenW, fadeH / 10f)
                }
            }
        }

        postRender(sceneInfo, sceneSwitcher)
    }

    /**
     * Draw a single achievement card in the grid.
     *
     * Layout per card:
     *   ┌──────────────────┐
     *   │  Achievement Name│
     *   │                  │
     *   │       😀         │   ← large emoji centered
     *   │                  │
     *   │   [✅ Completed] │   ← or [🔒 Locked]
     *   └──────────────────┘
     */
    private fun drawAchievementCard(
        sceneInfo: SceneInfo,
        x: Float, y: Float, w: Float, h: Float, r: Float,
        emoji: String, name: String, requirement: String,
        unlocked: Boolean
    ) {
        val c = AppColors.current

        with(GraphicsEngineWrapper(sceneInfo.enginePtr).getRenderer2D()) {
            rectMode(RectMode.CORNER)

            // Card background — slightly lighter for unlocked, darker for locked
            if (unlocked) {
                // Subtle accent-tinted card
                fill(c.backgroundCard)
            } else {
                // Dimmed card
                fill(c.backgroundRow)
            }
            rect(x, y, w, h, r)

            // Accent border on unlocked cards (thin top line)
            if (unlocked) {
                fill(c.accent)
                rect(x + r, y, w - 2f * r, 3f)
            }

            // Achievement name (top of card)
            if (unlocked) fill(c.textPrimary) else fill(c.textHint)
            textFont("roboto", 12)
            textAlign(TextAlignH.CENTER, TextAlignV.TOP)
            text(name, x + w / 2f, y + 20f)

            // Large emoji (centered in card)
            val emojiY = y + h * 0.46f
            textFont("emoji", if (unlocked) 40 else 32)
            textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
            if (!unlocked) fill(c.textHint) // dim emoji for locked
            text(emoji, x + w / 2f, emojiY)

            // Status badge at bottom
            val badgeW = w * 0.65f
            val badgeH = 38f
            val badgeX = x + (w - badgeW) / 2f
            val badgeY = y + h - badgeH - 18f
            val badgeR = badgeH / 2f

            if (unlocked) {
                // Green "Completed" badge
                fill(c.accent)
                rect(badgeX, badgeY, badgeW, badgeH, badgeR)
                fill(c.textOnAccent)
                textFont("roboto", 11)
                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                text("✅ Completed", badgeX + badgeW / 2f, badgeY + badgeH / 2f)
            } else {
                // Dark "Locked" badge
                fill(c.divider)
                rect(badgeX, badgeY, badgeW, badgeH, badgeR)
                fill(c.textHint)
                textFont("roboto", 11)
                textAlign(TextAlignH.CENTER, TextAlignV.CENTER)
                text("🔒 Locked", badgeX + badgeW / 2f, badgeY + badgeH / 2f)
            }
        }
    }
}
