package com.example.utils

data class Quote(val text: String, val author: String)

object MotivationalQuotes {
    val quotes = listOf(
        Quote("Dream, dream, dream. Dreams transform into thoughts and thoughts result in action.", "Dr. A.P.J. Abdul Kalam"),
        Quote("You have to dream before your dreams can come true.", "Dr. A.P.J. Abdul Kalam"),
        Quote("If you want to shine like a sun, first burn like a sun.", "Dr. A.P.J. Abdul Kalam"),
        Quote("All of us do not have equal talent. But, all of us have an equal opportunity to develop our talents.", "Dr. A.P.J. Abdul Kalam"),
        Quote("Excellence happens not by accident. It is a process.", "Dr. A.P.J. Abdul Kalam"),
        Quote("Arise, awake, and stop not till the goal is reached.", "Swami Vivekananda"),
        Quote("Take up one idea. Make that one idea your life; dream of it; think of it; live on that idea.", "Swami Vivekananda"),
        Quote("You cannot believe in God until you believe in yourself.", "Swami Vivekananda"),
        Quote("The greatest sin is to think yourself weak.", "Swami Vivekananda"),
        Quote("Strength is Life, Weakness is Death. Expansion is Life, Contraction is Death.", "Swami Vivekananda"),
        Quote("In the middle of difficulty lies opportunity.", "Albert Einstein"),
        Quote("Genius is 1% talent and 99% hard work.", "Albert Einstein"),
        Quote("It's not that I'm so smart, it's just that I stay with problems longer.", "Albert Einstein"),
        Quote("Education is not the learning of facts, but the training of the mind to think.", "Albert Einstein"),
        Quote("Nothing in life is to be feared, it is only to be understood. Now is the time to understand more, so that we may fear less.", "Marie Curie"),
        Quote("Be less curious about people and more curious about ideas.", "Marie Curie"),
        Quote("One never notices what has been done; one can only see what remains to be done.", "Marie Curie"),
        Quote("It always seems impossible until it is done.", "Nelson Mandela"),
        Quote("There is no passion to be found playing small in settling for a life that is less than the one you are capable of living.", "Nelson Mandela"),
        Quote("The greatest glory in living lies not in never falling, but in rising every time we fall.", "Nelson Mandela"),
        Quote("Your time is limited, so don't waste it living someone else's life.", "Steve Jobs"),
        Quote("The only way to do great work is to love what you do. If you haven't found it yet, keep looking. Don't settle.", "Steve Jobs"),
        Quote("Stay hungry, stay foolish.", "Steve Jobs"),
        Quote("Cultivation of mind should be the ultimate aim of human existence.", "Dr. B.R. Ambedkar"),
        Quote("Life should be great rather than long.", "Dr. B.R. Ambedkar"),
        Quote("Educate, Agitate, Organise. Have faith in yourselves.", "Dr. B.R. Ambedkar"),
        Quote("You cannot cross the sea merely by standing and staring at the water.", "Rabindranath Tagore"),
        Quote("Everything comes to us that belongs to us if we create the capacity to receive it.", "Rabindranath Tagore"),
        Quote("Faith is the bird that feels the light when the dawn is still dark.", "Rabindranath Tagore"),
        Quote("I have not failed. I've just found 10,000 ways that won't work.", "Thomas Edison"),
        Quote("Our greatest weakness lies in giving up. The most certain way to succeed is always to try just one more time.", "Thomas Edison"),
        Quote("Success is the sum of small efforts, repeated day in and day out.", "Robert Collier"),
        Quote("The expert in anything was once a beginner. Step by step, focus and discipline will get you there.", "Helen Hayes"),
        Quote("The best way to predict your future is to create it.", "Abraham Lincoln"),
        Quote("Give me six hours to chop down a tree and I will spend the first four sharpening the axe.", "Abraham Lincoln"),
        Quote("You don't have to be great to start, but you have to start to be great.", "Zig Ziglar"),
        Quote("Believe you can and you're halfway there.", "Theodore Roosevelt"),
        Quote("Do what you can, with what you have, where you are.", "Theodore Roosevelt"),
        Quote("The future belongs to those who believe in the beauty of their dreams.", "Eleanor Roosevelt"),
        Quote("With the new day comes new strength and new thoughts.", "Eleanor Roosevelt"),
        Quote("Learning never exhausts the mind.", "Leonardo da Vinci"),
        Quote("Simplicity is the ultimate sophistication.", "Leonardo da Vinci"),
        Quote("It had long since come to my attention that people of accomplishment rarely sat back and let things happen to them.", "Leonardo da Vinci"),
        Quote("Live as if you were to die tomorrow. Learn as if you were to live forever.", "Mahatma Gandhi"),
        Quote("The future depends on what you do today.", "Mahatma Gandhi"),
        Quote("Strength does not come from physical capacity. It comes from an indomitable will.", "Mahatma Gandhi"),
        Quote("You must be the change you wish to see in the world.", "Mahatma Gandhi"),
        Quote("What you do makes a difference, and you have to decide what kind of difference you want to make.", "Jane Goodall"),
        Quote("Only when we are brave enough to explore the darkness will we discover the infinite power of our light.", "Brené Brown"),
        Quote("Perseverance is not a long race; it is many short races one after the other.", "Walter Bagehot"),
        Quote("Action is the foundational key to all success.", "Pablo Picasso"),
        Quote("Success usually comes to those who are too busy to be looking for it.", "Henry David Thoreau"),
        Quote("Don't watch the clock; do what it does. Keep going.", "Sam Levenson"),
        Quote("The secret of getting ahead is getting started.", "Mark Twain"),
        Quote("Continuous effort—not strength or intelligence—is the key to unlocking our potential.", "Winston Churchill"),
        Quote("Success is not final, failure is not fatal: it is the courage to continue that counts.", "Winston Churchill"),
        Quote("To improve is to change; to be perfect is to change often.", "Winston Churchill"),
        Quote("If you are going through hell, keep going.", "Winston Churchill"),
        Quote("We are what we repeatedly do. Excellence, then, is not an act, but a habit.", "Aristotle"),
        Quote("Quality is not an act, it is a habit.", "Aristotle"),
        Quote("The roots of education are bitter, but the fruit is sweet.", "Aristotle"),
        Quote("Difficulties strengthen the mind, as labor does the body.", "Seneca"),
        Quote("Luck is what happens when preparation meets opportunity.", "Seneca"),
        Quote("It is not because things are difficult that we do not dare; it is because we do not dare that they are difficult.", "Seneca"),
        Quote("You have power over your mind - not outside events. Realize this, and you will find strength.", "Marcus Aurelius"),
        Quote("The happiness of your life depends upon the quality of your thoughts.", "Marcus Aurelius"),
        Quote("Waste no more time arguing about what a good man should be. Be one.", "Marcus Aurelius"),
        Quote("When you arise in the morning think of what a privilege it is to be alive, to think, to enjoy, to love.", "Marcus Aurelius"),
        Quote("He who has a why to live can bear almost any how.", "Friedrich Nietzsche"),
        Quote("That which does not kill us makes us stronger.", "Friedrich Nietzsche"),
        Quote("Self-belief and hard work will always earn you success.", "Virat Kohli"),
        Quote("Whatever you do, do it with passion and give it 100%.", "Sachin Tendulkar"),
        Quote("Discipline is choosing between what you want now and what you want most.", "Abraham Lincoln"),
        Quote("Setting goals is the first step in turning the invisible into the visible.", "Tony Robbins"),
        Quote("The only impossible journey is the one you never begin.", "Tony Robbins"),
        Quote("Small daily improvements over time lead to stunning results.", "Robin Sharma"),
        Quote("Clarity precedes mastery. The clearer you are on your goals, the more effective your efforts.", "Robin Sharma"),
        Quote("Focus on the process, not the outcome. The score takes care of itself.", "Bill Walsh"),
        Quote("An investment in knowledge pays the best interest.", "Benjamin Franklin"),
        Quote("Energy and persistence conquer all things.", "Benjamin Franklin"),
        Quote("By failing to prepare, you are preparing to fail.", "Benjamin Franklin"),
        Quote("Either write something worth reading or do something worth writing.", "Benjamin Franklin"),
        Quote("Start where you are. Use what you have. Do what you can.", "Arthur Ashe"),
        Quote("One important key to success is self-confidence. An important key to self-confidence is preparation.", "Arthur Ashe"),
        Quote("I find that the harder I work, the more luck I seem to have.", "Thomas Jefferson"),
        Quote("Do you want to know who you are? Don't ask. Act! Action will delineate and define you.", "Thomas Jefferson"),
        Quote("Great things are done by a series of small things brought together.", "Vincent van Gogh"),
        Quote("If you hear a voice within you say 'you cannot paint,' then by all means paint, and that voice will be silenced.", "Vincent van Gogh"),
        Quote("The power of concentrated attention is the sole key to the treasure-house of knowledge.", "Swami Vivekananda"),
        Quote("Never stop learning, because life never stops teaching.", "Lin Piao"),
        Quote("The capacity to learn is a gift; the ability to learn is a skill; the willingness to learn is a choice.", "Brian Herbert"),
        Quote("Champions keep playing until they get it right.", "Billie Jean King"),
        Quote("Success isn't about greatness. It's about consistency. Consistent hard work leads to success.", "Dwayne Johnson"),
        Quote("If you don't sacrifice for what you want, what you want becomes the sacrifice.", "Radhanath Swami"),
        Quote("Knowledge is power. Information is liberating. Education is the premise of progress in every society.", "Kofi Annan"),
        Quote("Patience and perseverance have a magical effect before which difficulties disappear and obstacles vanish.", "John Quincy Adams"),
        Quote("Hard work beats talent when talent fails to work hard.", "Kevin Durant"),
        Quote("The mind is everything. What you think you become.", "Buddha"),
        Quote("Purity, patience, and perseverance are the three essentials to success and, above all, love.", "Swami Vivekananda"),
        Quote("Where there is righteousness in the heart, there is beauty in the character.", "Dr. A.P.J. Abdul Kalam")
    )

    fun getQuoteOfTheDay(seenQuotes: Set<String> = emptySet()): Quote {
        if (seenQuotes.isEmpty()) {
            val calendar = java.util.Calendar.getInstance()
            val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
            val index = dayOfYear % quotes.size
            return quotes[index]
        }

        // Filter out quotes that have already been seen
        val unseen = quotes.filter { q ->
            val key = q.text.lowercase().trim()
            !seenQuotes.any { it.lowercase().trim() == key || it.contains(key.take(20)) }
        }

        return if (unseen.isNotEmpty()) {
            unseen.random()
        } else {
            // If all have been seen, pick any random
            quotes.random()
        }
    }
}

