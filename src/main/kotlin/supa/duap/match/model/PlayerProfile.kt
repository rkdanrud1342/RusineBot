package supa.duap.match.model


data class Player(
    val id : Long,
    val name : String,
    val eloScore : Int
) {
    val grade : Grade
        get() = Grade.getGrade(eloScore)
}

data class PlayerProfile(
    val id : Long,
    val name : String,
    val casualWinCount : Int,
    val casualLoseCount : Int,
    val rankWinCount : Int,
    val rankLoseCount : Int,
    val eloScore : Int
) {
    val grade : Grade
        get() = Grade.getGrade(eloScore)
}

enum class Grade(val gradeName : String) {
    BEGINNER("입문자"),
    FIGHTER("격투가"),
    MASTER_PROXY("사범대리"),
    EXPERT("달인"),
    KING_OF_FIST("패왕"),
    EMPEROR_OF_FIST("권황"),
    KING_LIKE("왕자"),
    STAR_FIST("권성"),
    IMMORTAL("불멸자");

    operator fun minus(opGrade : Grade) : Int = this.ordinal - opGrade.ordinal

    companion object {
        fun getGrade(eloScore : Int) =
            when (eloScore) {
                in Int.MIN_VALUE ..< 886 -> BEGINNER
                in 886 ..< 962 -> FIGHTER
                in 962 ..< 1038 -> MASTER_PROXY
                in 1038 ..< 1114 -> EXPERT
                in 1114 ..< 1190 -> KING_OF_FIST
                in 1190 ..< 1266 -> EMPEROR_OF_FIST
                in 1266 ..< 1342 -> KING_LIKE
                in 1342 ..< 1418 -> STAR_FIST
                else -> IMMORTAL
            }
    }
}