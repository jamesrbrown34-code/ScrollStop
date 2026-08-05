package com.scrollstop.app.core

import com.scrollstop.app.premium.ReminderTone

/**
 * Copy for a scroll reminder, split for good notification layout:
 * [title] is a short headline for the collapsed notification, [summary] is a one-line
 * collapsed text carrying the scroll count, and [message] is the full nudge shown when
 * the notification is expanded.
 */
data class ToneCopy(val title: String, val summary: String, val message: String)

/**
 * Builds the copy for a scroll reminder from the selected tone and awareness tier. Each tier
 * within a tone has its own bank of templates; a random entry is picked, avoiding an immediate
 * repeat of the last message shown.
 */
object ReminderMessaging {
    private data class Template(val title: String, val message: String)

    private val summaries: Map<ReminderTone, String> = mapOf(
        ReminderTone.GENERIC to "You've scrolled {count} times today",
        ReminderTone.MOTIVATOR to "{count} scrolls today — you've got this",
        ReminderTone.DRILL_SERGEANT to "{count} scrolls today!",
        ReminderTone.ACTION_HERO to "{count} scrolls and counting!"
    )

    private val banks: Map<ReminderTone, Map<AwarenessLevel, List<Template>>> = mapOf(
        ReminderTone.GENERIC to mapOf(
            AwarenessLevel.NOTICE to listOf(
                Template("Scroll check", "Is it worth your time?"),
                Template("Scroll check", "Are you still finding this interesting, or just scrolling?"),
                Template("Take a breath", "Do you actually want to keep going?"),
                Template("Scroll check", "What are you looking for right now?")
            ),
            AwarenessLevel.WARNING to listOf(
                Template("Scroll check", "That's a lot of scrolling. Is it adding to your day?"),
                Template("Scroll check", "The feed isn't going anywhere. Is your time?"),
                Template("Take a pause", "You've been scrolling a while — what are you avoiding?"),
                Template("Scroll check", "Could this time be better spent?")
            ),
            AwarenessLevel.CRITICAL to listOf(
                Template("Step back", "Your day is passing while the feed feeds you. Step back."),
                Template("Scroll check", "This is serious scrolling. What is it really costing you?"),
                Template("Scroll check", "Nothing in here is more important than the day you're living.")
            )
        ),
        ReminderTone.MOTIVATOR to mapOf(
            AwarenessLevel.NOTICE to listOf(
                Template("You're in control", "You don't need the feed. You've got this."),
                Template("Time for a win", "Put the phone down and take the small win."),
                Template("Good choice time", "One good choice right now makes a better rest of your day."),
                Template("Do something you love", "Close it and go do something you actually love.")
            ),
            AwarenessLevel.WARNING to listOf(
                Template("Do something for you", "Your future self is rooting for you. Take a break."),
                Template("Choose you", "Every scroll is a choice. Choose you."),
                Template("Still going strong", "You're worth more than this feed. Put it down.")
            ),
            AwarenessLevel.CRITICAL to listOf(
                Template("Change the pattern", "You've got the strength to put it down. Prove it to yourself."),
                Template("Finish proud", "Imagine finishing the day proud, not drained. Start now."),
                Template("You're bigger than it", "Step away and take back your afternoon.")
            )
        ),
        ReminderTone.DRILL_SERGEANT to mapOf(
            AwarenessLevel.NOTICE to listOf(
                Template("AT EASE!", "Is that feed going to pay your bills? No. Put it DOWN."),
                Template("LISTEN UP!", "That screen isn't going anywhere. Your day is. MOVE."),
                Template("DROP IT!", "You have better things to do than feed your thumbs. DROP IT.")
            ),
            AwarenessLevel.WARNING to listOf(
                Template("WASTE OF DAYLIGHT!", "You're burning daylight on junk. DROP IT NOW."),
                Template("ENOUGH!", "Do you want to be someone who scrolls all day? ACT LIKE IT."),
                Template("MOVE OUT!", "That feed isn't marching anywhere. You are. MOVE OUT.")
            ),
            AwarenessLevel.CRITICAL to listOf(
                Template("AT ATTENTION!", "Your phone doesn't own you. Take command. NOW."),
                Template("CRISIS OF ATTENTION!", "Close the app and stand tall. That's an order."),
                Template("DROP THE PHONE!", "Drop the phone. Push away from the desk. NOW.")
            )
        ),
        ReminderTone.ACTION_HERO to mapOf(
            AwarenessLevel.NOTICE to listOf(
                Template("The real fight's ahead", "The feed is the enemy. And you? You're the hero. Hasta la vista, feed!"),
                Template("Back to the mission", "Do it NOW. Close the app and come back stronger."),
                Template("Know when to strike", "A hero knows when to strike. Strike: close the app.")
            ),
            AwarenessLevel.WARNING to listOf(
                Template("Heroes know when to stop", "I'll be back — but right now you need to go. Step away!"),
                Template("Win this one", "A hero chooses their battles. This feed is not your battle. WIN THIS ONE."),
                Template("Finish the fight", "Finish this. Close the app. Be the hero of your day.")
            ),
            AwarenessLevel.CRITICAL to listOf(
                Template("Final boss!", "Your biggest muscle is your willpower. FLEX IT. Put the phone down."),
                Template("END THIS!", "Get up, walk away, and come back the victor. NOW."),
                Template("The big one", "You don't survive the feed by scrolling it. Close it and win.")
            )
        )
    )

    fun pick(tone: ReminderTone, level: AwarenessLevel, count: Int, lastMessage: String?): ToneCopy {
        val tier = banks[tone]
            ?.let { it[level] ?: it[AwarenessLevel.NOTICE] }
            ?: banks[ReminderTone.GENERIC]!![AwarenessLevel.NOTICE]!!
        val pool = if (lastMessage == null) tier else tier.filter { it.message != lastMessage }.ifEmpty { tier }
        val chosen = pool.random()
        val summary = (summaries[tone] ?: summaries[ReminderTone.GENERIC]!!)
            .replace("{count}", count.toString())
        return ToneCopy(
            chosen.title.replace("{count}", count.toString()),
            summary,
            chosen.message.replace("{count}", count.toString())
        )
    }
}
