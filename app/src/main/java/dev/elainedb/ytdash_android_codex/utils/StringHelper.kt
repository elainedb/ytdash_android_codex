package dev.elainedb.ytdash_android_codex.utils

object StringHelper {

    fun isPalindrome(input: String): Boolean {
        val cleaned = input.lowercase().filter { it.isLetterOrDigit() }
        return cleaned == cleaned.reversed()
    }

    fun countWords(input: String): Int {
        if (input.isBlank()) return 0
        return input.trim().split("\\s+".toRegex()).size
    }

    fun wordCount(text: String): Int = countWords(text)

    fun reverseWords(input: String): String = input.trim().split("\\s+".toRegex()).reversed().joinToString(" ")

    fun capitalizeWords(input: String): String {
        return input.trim().split("\\s+".toRegex()).joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
    }

    fun removeVowels(input: String): String = input.replace(Regex("[AEIOUaeiou]"), "")

    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return emailRegex.matches(email)
    }
}
