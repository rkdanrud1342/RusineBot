package supa.duap.modules

import dev.kord.common.annotation.KordVoice
import org.koin.dsl.module
import supa.duap.InteractionManager
import supa.duap.RoleManager
import supa.duap.command.manager.BasicCommandManager
import supa.duap.command.manager.MatchingCommandManager
import supa.duap.match.MatchMakingManager

@KordVoice
val managerModule = module {
    single { BasicCommandManager(get()) }
    single { MatchingCommandManager(get()) }

    single { MatchMakingManager(get(), get()) }

    single { RoleManager(get()) }
    single { InteractionManager(get(), get(), get()) }
}
