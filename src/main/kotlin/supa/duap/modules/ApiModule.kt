package supa.duap.modules

import org.koin.dsl.module
import retrofit2.Retrofit
import supa.duap.match.MatchMakingApi

val apiModule = module {
    single { get<Retrofit>().create(MatchMakingApi::class.java) }
}