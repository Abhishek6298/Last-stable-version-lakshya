package com.example.data

data class StudyPortal(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val tag: String = "Study Portal",
    val description: String = "",
    val iconEmoji: String = "🌐",
    val isDefault: Boolean = false,
    val isVerified: Boolean = true,
    val aiVerificationReason: String = ""
)

data class StudyWebVerificationResult(
    val isStudyWeb: Boolean,
    val reason: String,
    val suggestedTitle: String = "",
    val suggestedTag: String = "Study Portal",
    val suggestedEmoji: String = "🌐"
)

object StudyPortalGuard {
    private val BLOCKED_DOMAINS = listOf(
        "youtube.com", "m.youtube.com", "youtu.be", "youtube-nocookie.com", "ytimg.com", "music.youtube.com",
        "dailymotion.com", "dai.ly",
        "vimeo.com",
        "twitch.tv",
        "tiktok.com",
        "instagram.com", "ig.me",
        "facebook.com", "fb.com", "fb.watch", "messenger.com",
        "twitter.com", "x.com",
        "snapchat.com",
        "threads.net",
        "reddit.com",
        "netflix.com",
        "primevideo.com",
        "hotstar.com", "disneyplus.com",
        "sonyliv.com",
        "zee5.com",
        "jiocinema.com",
        "mxplayer.in",
        "hulu.com",
        "hbomax.com",
        "crunchyroll.com",
        "rumble.com",
        "bitchute.com",
        "metacafe.com",
        "veoh.com",
        "steamcommunity.com",
        "roblox.com",
        "dream11.com",
        "stake.com"
    )

    data class BlockResult(
        val isBlocked: Boolean,
        val platformName: String = "",
        val blockReason: String = ""
    )

    fun checkUrl(url: String): BlockResult {
        if (url.isBlank()) return BlockResult(false)
        val lower = url.lowercase().trim()

        for (domain in BLOCKED_DOMAINS) {
            if (lower.contains(domain)) {
                val name = when {
                    domain.contains("youtube") || domain.contains("youtu.be") || domain.contains("ytimg") -> "YouTube"
                    domain.contains("dailymotion") || domain.contains("dai.ly") -> "Dailymotion"
                    domain.contains("vimeo") -> "Vimeo"
                    domain.contains("twitch") -> "Twitch"
                    domain.contains("tiktok") -> "TikTok"
                    domain.contains("instagram") -> "Instagram"
                    domain.contains("facebook") || domain.contains("fb.") -> "Facebook"
                    domain.contains("twitter") || lower.contains("x.com") -> "X (Twitter)"
                    domain.contains("snapchat") -> "Snapchat"
                    domain.contains("reddit") -> "Reddit"
                    domain.contains("netflix") -> "Netflix"
                    domain.contains("hotstar") || domain.contains("disney") -> "Disney+ Hotstar"
                    domain.contains("primevideo") -> "Amazon Prime Video"
                    domain.contains("jiocinema") -> "JioCinema"
                    domain.contains("sonyliv") -> "Sony LIV"
                    domain.contains("zee5") -> "Zee5"
                    domain.contains("crunchyroll") -> "Crunchyroll"
                    domain.contains("rumble") -> "Rumble"
                    domain.contains("bitchute") -> "BitChute"
                    domain.contains("steam") || domain.contains("roblox") || domain.contains("dream11") -> "Gaming / Betting"
                    else -> "Entertainment & Video Platform"
                }
                return BlockResult(
                    isBlocked = true,
                    platformName = name,
                    blockReason = "🚫 $name is strictly blocked on study portals to eliminate distractions and maintain 100% focused preparation."
                )
            }
        }
        return BlockResult(false)
    }

    /**
     * Verifies that a given URL uses a safe web scheme (http or https) and is not blocked.
     */
    fun isAllowedSchemeAndUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase().trim()
        val isHttpOrHttps = lower.startsWith("http://") || lower.startsWith("https://")
        if (!isHttpOrHttps) return false
        return !checkUrl(lower).isBlocked
    }
}
