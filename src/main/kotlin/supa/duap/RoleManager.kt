package supa.duap

import com.kotlindiscord.kord.extensions.utils.hasRole
import dev.kord.core.Kord
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.on
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import supa.duap.match.model.Player

class RoleManager(
    private val kord : Kord
) {
    companion object {
        private const val ADMIN_ROLE_NAME = "관리자"
    }
    private val logger : Logger = LoggerFactory.getLogger(this.javaClass)

    private lateinit var adminRole : Role

    private val _fighterRoles = mutableListOf<Role>()
    val fighterRoles : List<Role>
        get() = _fighterRoles

    suspend fun start() {
        kord.on<GuildCreateEvent> {
            val roleArray = Array<Role?>(Grade.entries.size) { null }

            guild.roles.collect { role ->
                if (role.name.contains(ADMIN_ROLE_NAME)) {
                    adminRole = role
                    return@collect
                }

                val grade = Grade.entries.find { grade -> role.name.contains(grade.gradeName) } ?: return@collect

                roleArray[grade.ordinal] = role
            }

            roleArray.forEach { role ->
                if (role == null) return@forEach

                _fighterRoles.add(role)
            }

            logger.debug(_fighterRoles.joinToString { it.name })
        }
    }

    fun getMentionRoles(player : Player, rankAvailableRange : Int) : List<Role> {
        if (rankAvailableRange == -1) {
            return _fighterRoles
        }

        val index = player.grade.ordinal

        val lowIndex = (index - rankAvailableRange).coerceAtLeast(0)
        val highIndex = (index + rankAvailableRange).coerceAtMost(_fighterRoles.lastIndex)

        return _fighterRoles.slice(lowIndex .. highIndex)
    }

    fun getRoleFromGrade(grade : Grade) : Role = _fighterRoles[grade.ordinal]

    fun hasAdminRole(member: Member) : Boolean {
        if (!::adminRole.isInitialized) {
            return false
        }

        return member.hasRole(adminRole)
    }
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
    IMMORTAL_SR("이모탈 SR Immortal"),
    IMMORTAL_SSR("이모탈 SSR Immortal"),
    IMMORTAL_UR("이모탈 UR Immortal"),
    IMMORTAL_SUR("이모탈 SUR Immortal");

    operator fun minus(opGrade : Grade) : Int = this.ordinal - opGrade.ordinal

    companion object {
        fun getGrade(eloScore : Int) =
            when (eloScore) {
                in Int.MIN_VALUE..3999 -> BEGINNER
                in 4000..4249 -> FIGHTER
                in 4250..4499 -> MASTER_PROXY
                in 4500..4749 -> EXPERT
                in 4750..4999 -> KING_OF_FIST
                in 5000..5249 -> EMPEROR_OF_FIST
                in 5250..5499 -> KING_LIKE
                in 5500..5749 -> STAR_FIST
                in 5750..5999-> IMMORTAL
                in 6000..6249 -> IMMORTAL_SR
                in 6250..6499 -> IMMORTAL_SSR
                in 6500..6749 -> IMMORTAL_UR
                else -> IMMORTAL_SUR
            }

        fun getFromRole(vararg roles : Role) : Grade? {
            roles.forEach { role ->
                entries.find { role.name.contains(it.gradeName) }?.also { return it }
            }

            return null
        }
    }
}
