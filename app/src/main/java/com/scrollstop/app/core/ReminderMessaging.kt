package com.scrollstop.app.core

import com.scrollstop.app.premium.ReminderTone

/**
 * Copy for a scroll reminder, split for good notification layout:
 * [title] is a short count line for the collapsed/heads-up title, [summary] is a one-line
 * nudge for the collapsed text, and [message] is the full message shown when the
 * notification is expanded.
 */
data class ToneCopy(val title: String, val summary: String, val message: String)

/**
 * Builds the copy for a scroll reminder from the selected tone and awareness tier. Each tier
 * within a tone has its own bank of messages; a random entry is picked, avoiding an immediate
 * repeat of the last message shown. The popup nudge is the message's first sentence, so the
 * heads-up never truncates it.
 */
object ReminderMessaging {
    private data class Template(val message: String)

    private val countTitles: Map<ReminderTone, String> = mapOf(
        ReminderTone.GENERIC to "{count} scrolls today",
        ReminderTone.MOTIVATOR to "{count} scrolls in",
        ReminderTone.DRILL_SERGEANT to "{count} scrolls!",
        ReminderTone.ACTION_HERO to "{count} scrolls!",
        ReminderTone.FRIEND to "{count} scrolls today",
        ReminderTone.ZEN to "{count} scrolls"
    )

    private val banks: Map<ReminderTone, Map<AwarenessLevel, List<Template>>> = mapOf(
        ReminderTone.GENERIC to mapOf(
            AwarenessLevel.NOTICE to listOf(
                Template("Is it worth your time?"),
                Template("Are you still finding this interesting, or just scrolling?"),
                Template("Take a breath — do you actually want to keep going?"),
                Template("What are you looking for right now?")
            ),
            AwarenessLevel.WARNING to listOf(
                Template("That's a lot of scrolling. Is it adding to your day?"),
                Template("The feed isn't going anywhere. Is your time?"),
                Template("You've been scrolling a while — what are you avoiding?"),
                Template("Could this time be better spent?")
            ),
            AwarenessLevel.CRITICAL to listOf(
                Template("Your day is passing while the feed feeds you. Step back."),
                Template("This is serious scrolling. What is it really costing you?"),
                Template("Nothing in here is more important than the day you're living.")
            )
        ),
        ReminderTone.MOTIVATOR to mapOf(
            AwarenessLevel.NOTICE to listOf(
                Template("You don't need the feed. You've got this."),
                Template("Put the phone down and take the small win."),
                Template("One good choice right now makes a better rest of your day."),
                Template("Close it and go do something you actually love.")
            ),
            AwarenessLevel.WARNING to listOf(
                Template("Your future self is rooting for you. Take a break."),
                Template("Every scroll is a choice. Choose you."),
                Template("You're worth more than this feed. Put it down.")
            ),
            AwarenessLevel.CRITICAL to listOf(
                Template("You've got the strength to put it down. Prove it to yourself."),
                Template("Imagine finishing the day proud, not drained. Start now."),
                Template("Step away and take back your afternoon.")
            )
        ),
        ReminderTone.DRILL_SERGEANT to mapOf(
            AwarenessLevel.NOTICE to listOf(
                Template("Is that feed going to pay your bills? No. Put it DOWN."),
                Template("That screen isn't going anywhere. Your day is. MOVE."),
                Template("You have better things to do than feed your thumbs. DROP IT.")
            ),
            AwarenessLevel.WARNING to listOf(
                Template("You're burning daylight on junk. DROP IT NOW."),
                Template("Do you want to be someone who scrolls all day? ACT LIKE IT."),
                Template("That feed isn't marching anywhere. You are. MOVE OUT.")
            ),
            AwarenessLevel.CRITICAL to listOf(
                Template("Your phone doesn't own you. Take command. NOW."),
                Template("Close the app and stand tall. That's an order."),
                Template("Drop the phone. Push away from the desk. NOW.")
            )
        ),
        ReminderTone.ACTION_HERO to mapOf(
            AwarenessLevel.NOTICE to listOf(
                Template("The feed is the enemy. And you? You're the hero. Hasta la vista, feed!"),
                Template("Do it NOW. Close the app and come back stronger."),
                Template("A hero knows when to strike. Strike: close the app.")
            ),
            AwarenessLevel.WARNING to listOf(
                Template("I'll be back — but right now you need to go. Step away!"),
                Template("A hero chooses their battles. This feed is not your battle. WIN THIS ONE."),
                Template("Finish this. Close the app. Be the hero of your day.")
            ),
            AwarenessLevel.CRITICAL to listOf(
                Template("Your biggest muscle is your willpower. FLEX IT. Put the phone down."),
                Template("Get up, walk away, and come back the victor. NOW."),
                Template("You don't survive the feed by scrolling it. Close it and win.")
            )
        ),
        ReminderTone.FRIEND to mapOf(
            AwarenessLevel.NOTICE to listOf(
                Template("You've been scrolling a while. Want to take a break together?"),
                Template("Just noticed the time — how's your day going?"),
                Template("You're doing great. Maybe time to step away for a bit?")
            ),
            AwarenessLevel.WARNING to listOf(
                Template("That feed can wait. You've got better things to do."),
                Template("You've been at it a while. I'm not judging — I'm just here."),
                Template("Your time matters. Let's close this and go do something you enjoy.")
            ),
            AwarenessLevel.CRITICAL to listOf(
                Template("You've been scrolling a lot today. Let's take a proper break — you're worth it."),
                Template("This feed isn't going anywhere. Put it down and breathe with me."),
                Template("You've had a big scroll day. One break could change the whole evening.")
            )
        ),
        ReminderTone.ZEN to mapOf(
            AwarenessLevel.NOTICE to listOf(
                Template("The feed will still be here. So will you."),
                Template("Pause. Feel the weight of your thumb. What's next?"),
                Template("Scrolling is easy. Stepping away is a choice. Make it.")
            ),
            AwarenessLevel.WARNING to listOf(
                Template("One breath. Then another. The feed can wait."),
                Template("You're still scrolling. Notice that. Then choose again."),
                Template("Too much of anything dims it. Come back to now.")
            ),
            AwarenessLevel.CRITICAL to listOf(
                Template("Your whole day is ahead. The feed is a fraction of it."),
                Template("Nothing in here needs you right now. You need you."),
                Template("You've scrolled far today. Step back to the still point inside.")
            )
        )
    )

    fun pick(tone: ReminderTone, level: AwarenessLevel, count: Int, lastMessage: String?): ToneCopy {
        val tier = banks[tone]
            ?.let { it[level] ?: it[AwarenessLevel.NOTICE] }
            ?: banks[ReminderTone.GENERIC]!![AwarenessLevel.NOTICE]!!
        val pool = if (lastMessage == null) tier else tier.filter { it.message != lastMessage }.ifEmpty { tier }
        val chosen = pool.random()
        val message = chosen.message.replace("{count}", count.toString())
        val title = (countTitles[tone] ?: countTitles[ReminderTone.GENERIC]!!)
            .replace("{count}", count.toString())
        return ToneCopy(
            title = title,
            summary = firstSentence(message),
            message = message
        )
    }

    /** A standalone tone quote for the dashboard: a count-free message, avoiding an immediate repeat. */
    fun pickQuote(tone: ReminderTone, lastShown: String?): String {
        val toneBanks = banks[tone] ?: banks[ReminderTone.GENERIC]!!
        val pool = toneBanks.values.flatten().map { it.message }.filter { "{count}" !in it }
        val filtered = if (lastShown == null) pool else pool.filter { it != lastShown }.ifEmpty { pool }
        return filtered.random()
    }

    private fun firstSentence(text: String): String {
        val trimmed = text.trim()
        val index = trimmed.indexOfFirst { it == '.' || it == '?' || it == '!' }
        return if (index == -1) trimmed else trimmed.substring(0, index + 1).trim()
    }
}
