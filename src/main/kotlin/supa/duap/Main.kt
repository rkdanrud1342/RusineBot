package supa.duap

import dev.kord.common.annotation.KordVoice
import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.NON_PRIVILEGED
import dev.kord.gateway.PrivilegedIntent
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject
import supa.duap.modules.*

@KordVoice
suspend fun main() {
    startKoin {
        modules(
            kordModule,
            networkModule,
            apiModule,
            repositoryModule,
            managerModule
        )
    }

    val kord : Kord by inject(Kord::class.java)
    val interactionManager : InteractionManager by inject(InteractionManager::class.java)
    val roleManager : RoleManager by inject(RoleManager::class.java)

    interactionManager.start()
    roleManager.start()

    kord.login {
        @OptIn(PrivilegedIntent::class)
        intents = Intents.NON_PRIVILEGED + Intent.MessageContent
    }
}
