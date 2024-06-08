package supa.duap

import dev.kord.core.Kord
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.transform
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import supa.duap.match.model.Grade

class RoleManager(
    private val kord : Kord
) {
    private val logger : Logger = LoggerFactory.getLogger(this.javaClass)

    private val fighterRoles = mutableListOf<Role>()

    suspend fun start() {
        kord.on<GuildCreateEvent> {
            val roleArray = Array<Role?>(Grade.entries.size) { null }

            guild.roles.collect { role ->
                val grade = Grade.entries.find { grade -> role.name.contains(grade.gradeName) } ?: return@collect

                roleArray[grade.ordinal] = role
            }

            roleArray.forEach { role ->
                if (role == null) return@forEach

                fighterRoles.add(role)
            }

            logger.debug(fighterRoles.joinToString { it.name })
        }
    }

    suspend fun getMentionRoles(member : Member, rankAvailableRange : Int) : List<Role> {
        if (rankAvailableRange == -1) {
            return fighterRoles
        }

        return member.roles.transform { role ->
            val roleIndex = fighterRoles.indexOf(role).takeIf { it != -1 } ?: return@transform

            val lowIndex = (roleIndex - rankAvailableRange).takeIf { it >= 0 } ?: 0
            val highIndex = ((roleIndex + rankAvailableRange).takeIf { it <= fighterRoles.lastIndex } ?: fighterRoles.lastIndex) + 1

            emit(fighterRoles.subList(lowIndex, highIndex))
        }
            .single()
    }
}
