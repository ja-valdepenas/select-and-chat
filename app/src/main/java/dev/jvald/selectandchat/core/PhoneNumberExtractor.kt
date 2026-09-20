package dev.jvald.selectandchat.core

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.Leniency
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType
import com.google.i18n.phonenumbers.PhoneNumberUtil.ValidationResult
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber

/** One phone number resolved out of a text selection, pre-formatted for display. */
data class PhoneCandidate(
    /** "+34612345678" — what wa.me needs, minus the plus. */
    val e164: String,
    /** "+34 612 34 56 78" */
    val international: String,
    /** "612 34 56 78" */
    val national: String,
    val regionCode: String?,
    /**
     * False when this only came out of the forgiving fallback pass, which matches on
     * little more than digit count and will happily turn an order number into a phone
     * number. Callers must confirm these with the user instead of acting on them.
     */
    val confident: Boolean,
)

/** Everything the caller needs to pick one of the four UI branches. */
data class Extraction(
    val candidates: List<PhoneCandidate>,
    /** Looks like a local number, but no default country is set yet — ask for one. */
    val needsRegion: Boolean,
    /** Digits lifted from the selection, used to prefill manual entry. */
    val digitsHint: String,
)


/** How the digits typed so far measure up against the selected country. */
enum class LengthCheck { EMPTY, TOO_SHORT, OK, TOO_LONG, WRONG_LENGTH, UNKNOWN }

data class NumberFeedback(
    val check: LengthCheck,
    /** Digits in a typical number for the region, for the hint text. Null if unknown. */
    val expectedDigits: Int?,
) {
    /**
     * Whether to tell the user this number will not work. TOO_SHORT is excluded on
     * purpose: every number is too short while it is still being typed.
     */
    val shouldWarn: Boolean
        get() = check == LengthCheck.TOO_LONG || check == LengthCheck.WRONG_LENGTH
}

/**
 * Pulls phone numbers out of arbitrary selected text.
 *
 * All the genuinely hard parts — per-country formats, numbers embedded in prose,
 * extensions — are libphonenumber's job. This only decides how forgiving to be.
 */
object PhoneNumberExtractor {

    private const val UNKNOWN_REGION = "ZZ"
    private const val MIN_DIGITS = 5

    /** Matches a string made up *only* of characters that can appear in a phone number. */
    private val PHONE_ONLY = Regex("""^[+\d\s()\-./]+$""")

    private val util: PhoneNumberUtil get() = PhoneNumberUtil.getInstance()

    fun extract(text: CharSequence?, defaultRegion: String?): Extraction {
        val raw = text?.toString()?.trim().orEmpty()
        val digitsHint = raw.filter(Char::isDigit)
        if (raw.isEmpty()) return Extraction(emptyList(), needsRegion = false, digitsHint = "")

        val region = defaultRegion?.takeIf { it.isNotBlank() } ?: UNKNOWN_REGION
        val found = LinkedHashMap<String, PhoneCandidate>()

        // 1. The whole selection as a single number. The user selected exactly this text,
        // which is a much stronger signal of intent than a match buried in prose — so a
        // merely *possible* number is good enough here.
        wholeSelection(raw, region)?.let { found[it.e164] = it }

        // 2. Numbers embedded in surrounding text. VALID keeps order numbers, dates and
        // prices out of the results.
        matches(raw, region, Leniency.VALID, confident = true)
            .forEach { found.putIfAbsent(it.e164, it) }

        // 3. Only when both strict passes came up empty, retry forgivingly. Running this
        // unconditionally would let junk outrank real matches. These come back marked
        // unconfident: "Order #100045678" resolves here, and acting on it without showing
        // the user first would open a chat with a number they never meant.
        if (found.isEmpty()) {
            matches(raw, region, Leniency.POSSIBLE, confident = false)
                .forEach { found.putIfAbsent(it.e164, it) }
        }

        val needsRegion = found.isEmpty() &&
            region == UNKNOWN_REGION &&
            !raw.startsWith("+") &&
            digitsHint.length >= MIN_DIGITS &&
            PHONE_ONLY.matches(raw)

        return Extraction(found.values.toList(), needsRegion, digitsHint)
    }

    /** Parses a number the user typed by hand. Deliberately lenient — they meant it. */
    fun parseManual(input: String, region: String?): PhoneCandidate? {
        val trimmed = input.trim()
        if (trimmed.count(Char::isDigit) < MIN_DIGITS) return null
        val number = try {
            util.parse(trimmed, region?.takeIf { it.isNotBlank() } ?: UNKNOWN_REGION)
        } catch (e: NumberParseException) {
            return null
        }
        return if (util.isPossibleNumber(number)) number.toCandidate(confident = true) else null
    }

    private fun wholeSelection(raw: String, region: String): PhoneCandidate? {
        // Guard before parse(): libphonenumber will happily lift a number-ish substring
        // out of prose, which would bypass the stricter VALID pass below.
        if (!PHONE_ONLY.matches(raw)) return null
        if (raw.count(Char::isDigit) < MIN_DIGITS) return null
        val number = try {
            util.parse(raw, region)
        } catch (e: NumberParseException) {
            return null
        }
        // The user selected exactly this text and nothing else, so even a loosely-matching
        // number here is a deliberate choice rather than a lucky digit run.
        return if (util.isPossibleNumber(number)) number.toCandidate(confident = true) else null
    }

    private fun matches(
        raw: String,
        region: String,
        leniency: Leniency,
        confident: Boolean,
    ): List<PhoneCandidate> =
        util.findNumbers(raw, region, leniency, Long.MAX_VALUE)
            .map { it.number().toCandidate(confident) }

    /**
     * Length feedback for what the user has typed so far.
     *
     * The per-country rules come from libphonenumber's own metadata rather than a table
     * we would have to maintain: countries change their numbering plans, and several
     * allow more than one valid length, which a single "max digits" number cannot express.
     */
    fun checkLength(input: String, region: String?): NumberFeedback {
        val expected = expectedDigitsFor(region)
        val digits = input.count(Char::isDigit)
        if (digits == 0) return NumberFeedback(LengthCheck.EMPTY, expected)
        if (region.isNullOrBlank()) return NumberFeedback(LengthCheck.UNKNOWN, expected)

        val number = try {
            util.parse(input, region)
        } catch (e: NumberParseException) {
            // Unparseable so far is almost always a half-typed number.
            return NumberFeedback(LengthCheck.TOO_SHORT, expected)
        }

        val check = when (util.isPossibleNumberWithReason(number)) {
            ValidationResult.IS_POSSIBLE,
            ValidationResult.IS_POSSIBLE_LOCAL_ONLY,
            -> LengthCheck.OK

            ValidationResult.TOO_SHORT -> LengthCheck.TOO_SHORT
            ValidationResult.TOO_LONG -> LengthCheck.TOO_LONG
            ValidationResult.INVALID_LENGTH -> LengthCheck.WRONG_LENGTH
            else -> LengthCheck.UNKNOWN
        }
        return NumberFeedback(check, expected)
    }

    /** Digit count of a typical number for the region, taken from libphonenumber examples. */
    private fun expectedDigitsFor(region: String?): Int? {
        if (region.isNullOrBlank()) return null
        return runCatching {
            val example = util.getExampleNumberForType(region, PhoneNumberType.MOBILE)
                ?: util.getExampleNumber(region)
            example?.nationalNumber?.toString()?.length
        }.getOrNull()
    }

    private fun PhoneNumber.toCandidate(confident: Boolean) = PhoneCandidate(
        e164 = util.format(this, PhoneNumberFormat.E164),
        international = util.format(this, PhoneNumberFormat.INTERNATIONAL),
        national = util.format(this, PhoneNumberFormat.NATIONAL),
        regionCode = util.getRegionCodeForNumber(this),
        confident = confident,
    )

    private fun <K, V> LinkedHashMap<K, V>.putIfAbsent(key: K, value: V) {
        if (!containsKey(key)) put(key, value)
    }
}
