package supa.duap.command.manager

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import supa.duap.command.model.Command.BasicCommand

class BasicCommandManager(kord : Kord) : CommandManager<BasicCommand>(kord) {
    override suspend fun registerCommand() {
        addCommand(BasicCommand.PING)
    }

    override suspend fun responseCommand(command : BasicCommand, interaction : ChatInputCommandInteraction) =
        when (command) {
            BasicCommand.PING -> responsePingCommand(interaction)
        }

    private suspend fun responsePingCommand(interaction : ChatInputCommandInteraction) {
        interaction.respondPublic { content = "퐁!"}
    }
}
