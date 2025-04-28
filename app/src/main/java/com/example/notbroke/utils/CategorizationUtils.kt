package com.example.notbroke.utils

object CategorizationUtils {
    //custom category temp storage(will update to database permanet storage)
    private val customIncomeCategories = mutableListOf<String>()
    private val customExpenseCategories = mutableListOf<String>()

    // Basic keyword-to-category mapping. Expand this significantly.
    // Basic keyword-to-category mapping.

    private val categoryRules: Map<String, List<String>> = mapOf(
        "Air Travel" to listOf("flysafair", "kulula", "mango", "south african airways", "saa", "british airways", "emirates", "qatar", "airlink", "cemair", "lift", "airport", "plane ticket", "flight"),
        "Banking" to listOf("fnb", "first national bank", "standard bank", "nedbank", "absa", "capitec", "african bank", "tyme bank", "discovery bank", "bank charges", "atm"),
        "Clothing & Fashion" to listOf("zando", "spree", "poetry", "foschini", "truworths", "edgars", "jet", "mr price", "ackermans", "pep", "miladys", "queenspark", "studio 88", "sportscene", "totalsports", "cape union mart", "old khaki", "country road", "witchery"),
        "Education" to listOf("unisa", "uct", "wits", "stellenbosch university", "ukzn", "uj", "ru", "ufs", "nwu", "vut", "tut", "cput", "sbs", "gibs", "regenesys", "sacap", "damelin", "imm gsm", "boston city campus", "varsity college", "afda", "cityvarsity", "ca connect", "get smarter", "edx", "coursera", "udemy", "school fees", "textbooks", "stationery", "student loan"),
        "Electronics & Appliances" to listOf("hirsch's", "incredible connection", "house & home", "dial-a-bed", "furniture city", "ok furniture", "tevo", "russells", "game", "makro", "builders", "takealot", "wantitall", "cybercellar", "matrix warehouse", "computer mania"),
        "Entertainment" to listOf("ster-kinekor", "nu metro", "theatre", "artscape", "market theatre", "joburg theatre", "playstation", "xbox", "nintendo", "steam", "loot.co.za", "kalahari.com", "computicket", "webtickets", "quicket", "gigsberg", "park acoustics", "rocking the daisies", "oppikoppi", "afrikaburn", "joy of jazz", "cape town international jazz festival", "national arts festival", "dstv", "multichoice", "showmax", "netflix", "amazon prime video", "disney+", "spotify", "apple music", "youtube music", "soundcloud", "event", "concert", "tickets", "museum", "gallery", "zoo", "aquarium", "botanical garden"),
        "Financial Services" to listOf("sanlam", "old mutual", "liberty", "momentum", "discovery life", "investec", "allan gray", "coronation", "ninety one", "sygnia", "outvest", "easy equities", "satrix", "etfsa", "tax", "accountant", "financial advisor"),
        "Food & Beverage" to listOf("woolworths food", "pnp liquor", "checkers liquor", "tops at spar", "norman goodfellows", "makro liquor", "game liquor", "debonairs pizza", "pizza hut", "domino's pizza", "burger king", "wendy's", "ocean basket", "mozambik", "rhapsody's", "news cafe", "vida e caffe", "mugg & bean", "wimpy", "steers", "nando's", "kfc", "mcdonald's", "spur steak ranches", "fishaways", "kauai", "simply asia", "the braai room", "hazel food market", "neighbourgoods market", "oranjezicht city farm market", "wine", "beer", "spirits", "coffee", "tea", "juice", "soft drinks"),
        "Gaming & Gambling" to listOf("tab", "gold reef city casino", "suncoast casino", "grandwest casino", "montecasino", "emerald resort and casino", "tsogo sun gaming", "peermont", "lotto", "powerball", "sportingbet", "hollywoodbets", "supabets", "world sports betting", "playabets"),
        "Groceries & Household" to listOf("pnp", "pick n pay", "checkers", "woolies", "woolworths", "food", "shoprite", "usave", "spar", "grocery", "boxer", " Cambridge Food", "aldi", "lidl", "seven eleven", "forecourts", "corner shop", "household cleaning", "laundry", "toiletries", "pet food", "garden supplies"),
        "Health & Wellness" to listOf("clicks", "dischem", "pharmacy", "doctor", "hospital", "dr.", "clinic", "medical", "pathcare", "lancet laboratories", "ampath", "netcare", "mediclinic", "life healthcare", "intercare", "discovery health", "bonitas", "momentum medical scheme", "medscheme", "gemed", "bankmed", "keyhealth", "fedhealth", "santam medical aid", "denovo health", "optometrist", "dentist", "physiotherapist", "chiropractor", "psychologist", "dietician", "gym", "virgin active", "planet fitness", "zone fitness", "sweat 1000", "yoga", "pilates", "wellness warehouse", "faithful to nature", "takealot health"),
        "Home & Garden" to listOf("builders warehouse", "makro home", "game home", "house & home", "furniture city", "ok furniture", "dial-a-bed", "russells", "tevo", "cape union mart home", "mr price home", "sheet street", "home etc", "coricraft", "bloc & cork", "weylandts", "garden centres", "stodels", "lifestyle home garden", "plantland", "lawnmower", "braai", "outdoor furniture", "pool service"),
        "Insurance" to listOf("outsurance", "discovery insure", "king price", "miway", "virsekur", "auto & general", "budget insurance", "first for women", "hollard", "santam insurance", "old mutual insurance", "sanlam insurance", "momentum short-term insurance", "lion of africa", "axa africa", "guardrisk", "alexander forbes", "aon south africa", "marsh africa", "willis towers watson"),
        "Mobile & Internet" to listOf("vodacom", "mtn", "cell c", "telkom", "rain", "afrihost", "mweb", "rsaweb", "vox telecom", "cool ideas", "fibrehoods", "vumatel", "openserve", "octotel", "supersonic", "frogfoot networks", "mobile data", "airtime", "prepaid", "contract", "router", "modem"),
        "Personal Care & Beauty" to listOf("clicks beauty", "dischem beauty", "foschini for beauty", "truworths beauty", "edgars beauty", "woolworths beauty", "takealot beauty", "superbalist beauty", "bash beauty", "salon", "barber", "haircut", "hair dye", "manicure", "pedicure", "massage", "spa", "cosmetics", "makeup", "skincare", "fragrance", "toiletries", "bath products", "dental care", "hair products"),
        "Property & Accommodation" to listOf("pam golding properties", "remax", "seeff", "rawson properties", "engel & völkers", "sotheby's international realty", "just property", "leadhome", "private property", "property24", "roomsforafrica", "booking.com", "airbnb", "rent", "bond", "mortgage", "accommodation", "body corporate", "estate agent", "property management"),
        "Restaurants & Takeaways" to listOf("restaurant", "takeaway", "coffee", "kfc", "mcdonalds", "nandos", "spur", "wimpy", "steers", "debonairs", "fishaways", "kauai", "vida e", "mug", "bean", "ocean basket", "mozambik", "rhapsody's", "news cafe", "simply asia", "the braai room", "hazel food market", "neighbourgoods market", "oranjezicht city farm market"), // More comprehensive list
        "Shopping & Retail" to listOf("clothing", "shoes", "takealot", "makro", "game", "builders", "amazon", "superbalist", "bash", "zando", "spree", "poetry", "foschini", "truworths", "edgars", "jet", "mr price", "ackermans", "pep", "miladys", "queenspark", "studio 88", "sportscene", "totalsports", "cape union mart", "old khaki", "country road", "witchery", "hirsch's", "incredible connection", "house & home", "dial-a-bed", "furniture city", "ok furniture", "tevo", "russells"), // Combined general shopping with some specifics
        "Sports & Outdoors" to listOf("totalsports", "sportscene", "cape union mart", "old khaki", "first ascent", "outdoor warehouse", "trappers", "campsite", "fishing", "hiking", "cycling", "running", "gym equipment", "sports apparel", "soccer", "rugby", "cricket", "golf", "swimming"),
        "Transport & Automotive" to listOf("fuel", "petrol", "engen", "shell", "sasol", "caltex", "total", "uber", "bolt", "taxi", "gautrain", "parking", "car wash", "tolcon", "n3 toll route", "bakwena platinum toll road", "tracn4", "sanral", "aa", "cartrack", "matrix", "netstar", "vehicle insurance", "car service", "tyres", "battery", "windscreen", "licence disc", "traffic fine", "public transport", "bus", "train", "minibus taxi"),
        "Travel & Tourism" to listOf("south african tourism", "cape town tourism", "gauteng tourism", "kruger national park", "table mountain", "v&a waterfront", "robben island", "garden route", "drakensberg", "st lucia", "pilanesberg", "sun city", "safari", "game reserve", "hotel", "guesthouse", "backpackers", "flights", "car rental", "travel agent", "tour operator", "tourism grading council of south africa", "south african national parks", "heritage south africa"),
        "Utilities & Municipal Services" to listOf("eskom", "city power", "city of cape town electricity", "eThekwini electricity", "electricity", "water", "municipal", "rates", "levy", "refuse removal", "sewerage", "wifi", "internet", "fibre", "adsl", "telkom landline", "tv licence"),
        "Other" to listOf() // Default/fallback
    ).toSortedMap() // Sort categories alphabetically for the dropdown

    // List of all possible categories (used for the dropdown) - now sorted
    val allCategories: List<String> = categoryRules.keys.toList()

    // Income categories - define separately if needed for the dialog- making changes to now accept custom categories
    val incomeCategories: List<String> = (listOf("Salary", "Investments", "Freelance", "Gift", "Other Income") + customIncomeCategories).sorted()

    //expense categories - define separately if needed for the dialog- making changes to now accept custom categories
    val expenseCategories: List<String> = (categoryRules.keys.toList() + customExpenseCategories).sorted()

    // add custom income categories
    fun addCustomIncomeCategory(category: String) {
        if (!expenseCategories.contains(category)) {
            customIncomeCategories.add(category)
        }
    }
    //remove custom income categories
    fun removeCustomIncomeCategory(category: String) {
        customIncomeCategories.remove(category)
    }

    // add custom expense categories
    fun addCustomExpenseCategory(category: String) {
        if (!expenseCategories.contains(category)) {
            customExpenseCategories.add(category)
        }
    }
    //remove custom expense categories
    fun removeCustomExpenseCategory(category: String) {
        customExpenseCategories.remove(category)
    }


    /**
     * Suggests a category based on keywords in the transaction description.
     * Prioritizes longer keyword matches if applicable (though current logic takes first match).
     *
     * @param description The transaction description text.
     * @return The suggested category name (String) or null if no rule matches.
     */
    fun suggestCategory(description: String?): String? {
        if (description.isNullOrBlank()) {
            return null
        }

        val descLower = description.lowercase() // Case-insensitive matching

        // Consider enhancing: maybe prioritize rules with more specific/longer keywords first
        // Or return multiple suggestions

        for ((category, keywords) in categoryRules) {
            // Sort keywords by length descending to match longer keywords first (optional enhancement)
            // val sortedKeywords = keywords.sortedByDescending { it.length }
            for (keyword in keywords) { // Use 'keywords' or 'sortedKeywords'
                if (descLower.contains(keyword.lowercase())) {
                    return category // Return the first category that matches
                }
            }
        }

        return null // No keyword match found
    }
}