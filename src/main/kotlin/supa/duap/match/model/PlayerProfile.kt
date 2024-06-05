package supa.duap.match.model

import dev.kord.core.entity.Role


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
    IMMORTAL("이모탈 Immortal"),
    IMMORTAL_SSR("이모탈 SSR Immortal"),
    IMMORTAL_UR("이모탈 UR Immortal"),
    IMMORTAL_SUR("이모탈 SUR Immortal");

    operator fun minus(opGrade : Grade) : Int = this.ordinal - opGrade.ordinal

    companion object {
        fun getGrade(eloScore : Int) =
            when (eloScore) {
                in 0..1530 -> BEGINNER
                in 1531..2300 -> FIGHTER
                in 2301..3070 -> MASTER_PROXY
                in 3071..3840 -> EXPERT
                in 3841..4610 -> KING_OF_FIST
                in 4611..5380 -> EMPEROR_OF_FIST
                in 5381..6150 -> KING_LIKE
                in 6151..6920 -> STAR_FIST
                in 6921..7690-> IMMORTAL
                in 7691..8460 -> IMMORTAL_SSR
                in 8461..9230 -> IMMORTAL_UR
                else -> IMMORTAL_SUR
            }

        fun getFromRole(roles : List<Role>) : Grade {
            roles.forEach { role ->
                val grade = entries.find { role.name.contains(it.gradeName) }

                if (grade != null) {
                    return grade
                }
            }

            return BEGINNER
        }
    }
}
