package com.greenicephoenix.traceledger.feature.sms.parser

/**
 * Central registry of built-in SMS parsing patterns.
 *
 * DESIGN PHILOSOPHY:
 *  - Rather than per-bank parsers (fragile, needs constant updates),
 *    we use a layered generic approach that works for any bank worldwide.
 *  - Bank-specific sender detection improves description quality (adds "HDFC" prefix)
 *    but is NOT required for the parsing to succeed.
 *  - All regex patterns are case-insensitive (use with RegexOption.IGNORE_CASE).
 *
 * ADDING A NEW BANK:
 *  Simply add a BankSenderInfo entry to KNOWN_SENDERS. No other changes needed.
 */
object BuiltInSmsRules {

    // =========================================================================
    //  AMOUNT PATTERNS
    //  Order matters — try the most specific first.
    //  All amounts use commas as thousand separators (Indian standard).
    // =========================================================================

    val AMOUNT_PATTERNS = listOf(
        // ₹1,000.00 or ₹1000
        Regex("""₹\s*([0-9,]+(?:\.[0-9]{1,2})?)"""),
        // Rs.1,000.00 or Rs 1000 or Rs.1000.00
        Regex("""[Rr][Ss]\.?\s*([0-9,]+(?:\.[0-9]{1,2})?)"""),
        // INR 1,000.00
        Regex("""INR\s+([0-9,]+(?:\.[0-9]{1,2})?)"""),
        // USD/GBP/EUR/AUD/SGD 1,000.00 (international)
        Regex("""(?:USD|GBP|EUR|AUD|SGD|CAD|AED|JPY)\s*([0-9,]+(?:\.[0-9]{1,2})?)"""),
    )

    // =========================================================================
    //  DIRECTION KEYWORDS
    //  We check for these in the full lowercase SMS body.
    //  ORDER IS IMPORTANT: check longer/more specific phrases first to avoid
    //  partial matches (e.g. "credited to" should not be confused with just "credit").
    // =========================================================================

    /** These keywords indicate money LEFT the user's account → EXPENSE */
    val DEBIT_KEYWORDS = listOf(
        "debited from",
        "has been debited",
        "is debited",
        "was debited",
        "debited",
        "spent via",
        "spent at",
        "spent on",
        "payment of",
        "paid to",
        "sent to",
        "transferred to",
        "withdrawn",
        "deducted",
        "purchase",
        "transaction of",        // common in HDFC/ICICI CC
        "charged to",
        " dr ",                   // Indian banking shorthand
        "dr.",
        " dr\n",
    )

    /** These keywords indicate money ENTERED the user's account → INCOME */
    val CREDIT_KEYWORDS = listOf(
        "credited to",
        "has been credited",
        "is credited",
        "was credited",
        "credited",
        "received from",
        "deposited to",
        "deposited in",
        "refund",
        "cashback",
        "salary",
        "neft credit",
        "rtgs credit",
        "imps credit",
        "upi credit",
        " cr ",                   // Indian banking shorthand
        "cr.",
        " cr\n",
    )

    // =========================================================================
    //  OTP / PROMO FILTERS
    //  If any of these appear in the SMS, we discard it immediately.
    //  This is the first gate — runs before amount extraction.
    // =========================================================================

    val OTP_KEYWORDS = listOf(
        "otp",
        "one time password",
        "one-time password",
        "verification code",
        "verify",
        "authentication code",
        "login attempt",
        "login otp",
        "not share",           // "Do not share your OTP"
        "never share",
    )

    val PROMO_KEYWORDS = listOf(
        "offer",
        "discount",
        "cashback offer",
        "click here",
        "visit us",
        "reply stop",
        "unsubscribe",
        "congratulations",
        "you have won",
        "limited time",
        "expires soon",
        "exclusive deal",
        "upgrade now",
    )

    // =========================================================================
    //  ACCOUNT LAST-4 PATTERNS
    //  Different banks format this differently. Try all patterns.
    // =========================================================================

    val ACCOUNT_LAST4_PATTERNS = listOf(
        // **1234 or XX1234 or XXXX1234
        Regex("""(?:\*{2,4}|[Xx]{2,4})([0-9]{4})"""),
        // a/c 1234 or A/C no. XXXX1234
        Regex("""[Aa]/[Cc]\.?\s*(?:[Nn][Oo]\.?\s*)?(?:[Xx*]{0,6})([0-9]{4})"""),
        // Acct XX1234
        Regex("""[Aa]cct?\.?\s+(?:[Xx*]{0,4})([0-9]{4})"""),
        // Account ending 1234 or Account no. 1234
        Regex("""[Aa]ccount\s+(?:ending\s+|[Nn]o\.?\s+)?(?:[Xx*]{0,6})([0-9]{4})"""),
        // Card XX1234
        Regex("""[Cc]ard\s+(?:no\.?\s+)?(?:[Xx*]{0,6})([0-9]{4})"""),
        // ...1234 (common in some banks)
        Regex("""\.{3}([0-9]{4})"""),
    )

    // =========================================================================
    //  MERCHANT / DESCRIPTION EXTRACTION
    //  We look for these keywords and extract what comes AFTER them.
    //  The extracted text is cleaned before use.
    // =========================================================================

    /**
     * After these keywords, the merchant/description text follows.
     * Ordered from most reliable to least reliable.
     */
    val MERCHANT_AFTER_KEYWORDS = listOf(
        "info:",
        "info -",
        "merchant:",
        "merchant name:",
        "at merchant ",
        "txn at ",
        "spent at ",
        "paid to ",
        "sent to ",
        "trf to ",
        "transfer to ",
        "towards ",
        "to vpa ",          // UPI VPA
        "upi-",             // "UPI-MERCHANT@bank"
        "upi/",             // "UPI/MERCHANT"
        "ref: ",
        "ref no ",
        "remarks: ",
    )

    /**
     * Text that signals the merchant name has ENDED — stop extracting here.
     * These are common suffix patterns in bank SMSes.
     */
    val MERCHANT_END_DELIMITERS = listOf(
        "avail",        // "Available balance" / "Avail bal"
        "bal",          // "Balance:"
        "available",
        "limit",        // "Available credit limit"
        "ref no",
        "txn id",
        "transaction id",
        "auth code",
        " on ",         // "spent at MERCHANT on 01/01/24"
        ". on ",
    )

    // =========================================================================
    //  DATE PATTERNS
    //  Indian formats + ISO format + short formats
    // =========================================================================

    val DATE_PATTERNS = listOf(
        // DD-MM-YYYY or DD/MM/YYYY
        Regex("""(\d{1,2})[/-](\d{1,2})[/-](\d{4})"""),
        // DD-MM-YY or DD/MM/YY
        Regex("""(\d{1,2})[/-](\d{1,2})[/-](\d{2})"""),
        // DD-Mon-YYYY (e.g. 01-Jan-2024)
        Regex("""(\d{1,2})[- ](Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*[- ](\d{2,4})""",
            RegexOption.IGNORE_CASE),
        // DD Mon YYYY (e.g. 01 January 2024)
        Regex("""(\d{1,2})\s+(January|February|March|April|May|June|July|August|September|October|November|December)\s+(\d{4})""",
            RegexOption.IGNORE_CASE),
    )

    // =========================================================================
    //  KNOWN BANK SENDERS
    //  Used to:
    //    1. Quickly confirm this SMS is from a financial institution (primary gate)
    //    2. Add bank name to the parsed description for better user context
    //    3. Mark as creditCard so we can suggest the right TraceLedger account type
    // =========================================================================

    data class BankSenderInfo(
        val bankName: String,
        /** The sender ID must CONTAIN any of these strings (case-insensitive) */
        val senderContains: List<String>,
        val isWallet: Boolean = false,
        val isCreditCard: Boolean = false,
    )

    val KNOWN_SENDERS: List<BankSenderInfo> = listOf(
        // --- Indian Banks ---
        BankSenderInfo("HDFC Bank",      listOf("HDFCBK", "HDFCBN", "HDFC-B", "VK-HDFCBK", "BP-HDFCBK")),
        BankSenderInfo("ICICI Bank",     listOf("ICICIB", "ICICI-B", "VK-ICICI", "BP-ICICIB")),
        BankSenderInfo("SBI",            listOf("SBIINB", "SBIPSG", "SBISMS", "SBI-IN", "SBI-SM")),
        BankSenderInfo("Axis Bank",      listOf("AXISBK", "UTIBSB", "AXIS-B", "AXIBNK")),
        BankSenderInfo("Kotak Bank",     listOf("KOTAKB", "KOTAK-B", "KOTAK-M", "KOTAKM")),
        BankSenderInfo("Yes Bank",       listOf("YESBK", "YES-BK", "YESBNK", "YESBKG")),
        BankSenderInfo("PNB",            listOf("PNBSMS", "PNB-SM", "PNBALR")),
        BankSenderInfo("Bank of India",  listOf("BOISMS", "BOI-SM", "BOISML")),
        BankSenderInfo("Canara Bank",    listOf("CANBKM", "CANBNK", "CANARA")),
        BankSenderInfo("Federal Bank",   listOf("FEDBNK", "FEDRAL", "FEDBKM")),
        BankSenderInfo("IndusInd Bank",  listOf("INDUSL", "INDUSB", "INDUSIN")),
        BankSenderInfo("Bank of Baroda", listOf("BOBSMS", "BARODA", "BOBALR")),
        BankSenderInfo("Union Bank",     listOf("UNIONB", "UBISMS", "UNBISM")),
        BankSenderInfo("UCO Bank",       listOf("UCOBSM", "UCOBKM")),
        BankSenderInfo("IDFC First",     listOf("IDFCBK", "IDFCFI", "IDFC-F")),
        BankSenderInfo("RBL Bank",       listOf("RBLBNK", "RBL-BK", "RBLBKM")),
        BankSenderInfo("Karnataka Bank", listOf("KTKBNK", "KARBKM")),
        BankSenderInfo("South Indian Bank", listOf("SIBSMS", "SIB-SM")),
        BankSenderInfo("IDBI Bank",      listOf("IDBISM", "IDBIBK")),

        // --- Wallets & UPI ---
        BankSenderInfo("PhonePe",        listOf("PHONEPE", "PHPE"),      isWallet = true),
        BankSenderInfo("Google Pay",     listOf("GPAY", "GPAYIN"),       isWallet = true),
        BankSenderInfo("Paytm",          listOf("PAYTMB", "PYTM", "PAYTM"), isWallet = true),
        BankSenderInfo("Amazon Pay",     listOf("AMAZON", "AMZNPAY"),    isWallet = true),
        BankSenderInfo("CRED",           listOf("CREDPE", "CRED-U"),     isWallet = true),
        BankSenderInfo("Mobikwik",       listOf("MOBIKW", "MBKWIK"),     isWallet = true),
        BankSenderInfo("Freecharge",     listOf("FREECHAR", "FREECHG"),  isWallet = true),

        // --- Credit Cards ---
        BankSenderInfo("HDFC Credit Card",  listOf("HDFCCC", "HDFC-CC", "HDFCCR"), isCreditCard = true),
        BankSenderInfo("ICICI Credit Card", listOf("ICICIC", "ICICICR", "ICICCC"), isCreditCard = true),
        BankSenderInfo("SBI Card",          listOf("SBICRD", "SBICC", "SBICRD"),   isCreditCard = true),
        BankSenderInfo("Axis Credit Card",  listOf("AXISCC", "AXISCR"),            isCreditCard = true),
        BankSenderInfo("Kotak Credit Card", listOf("KOTAKC", "KOTAKCC"),           isCreditCard = true),
        BankSenderInfo("Citi Card",         listOf("CITIBK", "CITICC"),            isCreditCard = true),
        BankSenderInfo("AMEX",              listOf("AMEXIN", "AMEX"),              isCreditCard = true),

        // --- International (generic patterns) ---
        BankSenderInfo("Chase",          listOf("CHASE", "JPMORGN")),
        BankSenderInfo("HSBC",           listOf("HSBCSM", "HSBC-S")),
        BankSenderInfo("Citi",           listOf("CITIBK", "CITI-B")),
        BankSenderInfo("Barclays",       listOf("BARCLY", "BARCSM")),
        BankSenderInfo("Lloyds",         listOf("LLOYDS", "LLBKSM")),
    )

    // =========================================================================
    //  AUTO-CATEGORISATION KEYWORDS
    //  Maps description keywords → category names (must match user's category names).
    //  The SmsRuleEngine walks this list and returns the FIRST match.
    // =========================================================================

    /**
     * Returns suggested category name for a given SMS description.
     * Null if no confident match.
     *
     * Category names here match the defaults seeded in TraceLedger on first run.
     * If the user renamed a category, the name won't match — that's fine, we fall
     * back to null and the auto-categorizer on the repository level handles it.
     */
    fun suggestCategory(description: String): String? {
        val lower = description.lowercase()
        return when {
            // Food & Dining
            lower.containsAny("zomato", "swiggy", "domino", "mcdonald", "kfc", "pizza",
                "burger", "restaurant", "cafe", "coffee", "starbucks", "chai",
                "dunkin", "subway", "barbeque", "bbq", "dineout", "eatsure") -> "Food & Dining"

            // Groceries
            lower.containsAny("bigbasket", "grofers", "blinkit", "zepto", "dunzo",
                "dmart", "reliance fresh", "more supermarket", "hypercity",
                "grocery", "supermarket", "vegetables", "kirana", "instamart") -> "Groceries"

            // Transport
            lower.containsAny("uber", "ola", "rapido", "meru", "blablacar",
                "metro", "irctc", "train", "bus", "petrol", "fuel", "hp petrol",
                "indian oil", "bpcl", "fasttag", "toll", "parking") -> "Transport"

            // Shopping
            lower.containsAny("amazon", "flipkart", "myntra", "ajio", "nykaa",
                "meesho", "snapdeal", "tata cliq", "shopping", "mall",
                "retail", "purchase") -> "Shopping"

            // Entertainment
            lower.containsAny("netflix", "hotstar", "prime video", "zee5",
                "sonyliv", "jiocinema", "spotify", "gaana", "wynk",
                "book my show", "bookmyshow", "pvr", "inox", "movie") -> "Entertainment"

            // Health & Medical
            lower.containsAny("apollo", "1mg", "pharmeasy", "medplus",
                "netmeds", "hospital", "clinic", "pharmacy", "medicine",
                "doctor", "dental", "lab test", "diagnostic") -> "Health & Medical"

            // Utilities & Bills
            lower.containsAny("bescom", "tpddl", "msedcl", "electricity",
                "water bill", "gas bill", "piped gas", "adani gas",
                "mahanagar gas", "indane", "bharat gas", "hp gas") -> "Utilities"

            // Mobile & Internet
            lower.containsAny("airtel", "jio", "vi ", "vodafone",
                "bsnl", "recharge", "mobile bill", "postpaid",
                "broadband", "internet bill") -> "Phone & Internet"

            // Education
            lower.containsAny("byju", "unacademy", "vedantu", "courseera",
                "udemy", "school fee", "college fee", "tuition",
                "education", "coaching") -> "Education"

            // Travel
            lower.containsAny("makemytrip", "goibibo", "yatra", "cleartrip",
                "booking.com", "airbnb", "oyo", "hotel", "flight",
                "indigo", "airindia", "spicejet") -> "Travel"

            // Insurance
            lower.containsAny("lic", "policybazaar", "insuranc",
                "premium", "life insurance", "health insurance") -> "Insurance"

            // Investments
            lower.containsAny("zerodha", "groww", "upstox", "paytm money",
                "mutual fund", "sip", "nps", "ppf", "fd maturity",
                "dividend", "interest credit") -> "Investments"

            // Income patterns
            lower.containsAny("salary", "payroll", "wages") -> "Salary"
            lower.containsAny("freelance", "consulting fee") -> "Freelance"

            else -> null
        }
    }

    /** Helper extension to check if a string contains any of the given substrings */
    private fun String.containsAny(vararg substrings: String): Boolean =
        substrings.any { this.contains(it, ignoreCase = true) }
}