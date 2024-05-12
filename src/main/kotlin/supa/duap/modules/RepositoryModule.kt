package supa.duap.modules

import org.koin.dsl.module
import supa.duap.match.MatchMakingRepository

val repositoryModule = module {
    single { MatchMakingRepository(get()) }
}