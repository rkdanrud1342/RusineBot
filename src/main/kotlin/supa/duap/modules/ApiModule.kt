package supa.duap.modules

import org.koin.dsl.module
import supa.duap.match.MatchMakingApi
import supa.duap.match.MatchMakingApiImpl

val apiModule = module {
    single<MatchMakingApi> { MatchMakingApiImpl(get()) }
}