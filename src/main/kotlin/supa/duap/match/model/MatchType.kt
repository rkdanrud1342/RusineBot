package supa.duap.match.model

enum class MatchType(val typeName : String, val typeCode : Int) {
    CASUAL("캐주얼", 0),
    RANK("랭크", 1);

    companion object {
        fun typeCodeOf(typeCode : Int) : MatchType = entries.find { it.typeCode == typeCode } ?: CASUAL
    }
}