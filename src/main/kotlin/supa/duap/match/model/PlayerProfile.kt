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
                in 0..153 -> BEGINNER
                in 154..230 -> FIGHTER
                in 231..307 -> MASTER_PROXY
                in 308..384 -> EXPERT
                in 385..461 -> KING_OF_FIST
                in 462..538 -> EMPEROR_OF_FIST
                in 539..615 -> KING_LIKE
                in 616..692 -> STAR_FIST
                in 693..769-> IMMORTAL
                in 770..846 -> IMMORTAL_SSR
                in 847..923 -> IMMORTAL_UR
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
