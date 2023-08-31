package supa.duap.command.manager

import dev.kord.core.Kord
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import supa.duap.command.model.Command

abstract class CommandManager<T : Command>(protected val kord : Kord) {
    abstract suspend fun registerCommand()
    abstract suspend fun responseCommand(command : T, interaction : ChatInputCommandInteraction)

    companion object {
        val commandList : MutableList<Command> = mutableListOf()
    }

    suspend fun addCommand(
        command : Command,
        builder : GlobalChatInputCreateBuilder.() -> Unit = {}
    ) {
        commandList.add(command)
        kord.createGlobalChatInputCommand(
            name = command.key,
            description = command.description,
            builder = builder
        )
    }
}
