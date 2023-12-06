package supa.duap

import dev.kord.common.annotation.KordVoice
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import supa.duap.command.manager.BasicCommandManager
import supa.duap.command.model.Command
import supa.duap.command.manager.CommandManager
import supa.duap.command.manager.MatchingCommandManager
import supa.duap.command.manager.MusicCommandManager

@KordVoice
class InteractionManager(
    private val kord : Kord,
    private val basicCommandManager : BasicCommandManager,
    private val musicCommandManager : MusicCommandManager,
    private val matchingCommandManager : MatchingCommandManager
) {

    suspend fun start() {
        basicCommandManager.registerCommand()
        musicCommandManager.registerCommand()
        matchingCommandManager.registerCommand()

        kord.on<GuildChatInputCommandInteractionCreateEvent> {
            val command = CommandManager.commandList.find { it.key == interaction.invokedCommandName}

            when (command) {
                is Command.BasicCommand -> basicCommandManager.responseCommand(command, interaction)
                is Command.MusicCommand -> musicCommandManager.responseCommand(command, interaction)
                is Command.MatchingCommand -> matchingCommandManager.responseCommand(command, interaction)
                else -> responseUnknownCommand(interaction)
            }
        }
    }

    private suspend fun responseUnknownCommand(interaction : ChatInputCommandInteraction) {
        interaction.respondPublic { content = "제가 모르는 명령어에요." }
    }
}
