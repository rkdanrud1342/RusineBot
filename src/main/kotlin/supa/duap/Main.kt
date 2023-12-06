package supa.duap

import dev.kord.common.annotation.KordVoice
import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject
import supa.duap.modules.kordModule
import supa.duap.modules.managerModule
import supa.duap.modules.repositoryModule

@KordVoice
suspend fun main() {
    startKoin {
        modules(
            kordModule,
            repositoryModule,
            managerModule
        )
    }

    val kord : Kord by inject(Kord::class.java)
    val interactionManager : InteractionManager by inject(InteractionManager::class.java)

    interactionManager.start()

    kord.login {
        @OptIn(PrivilegedIntent::class)
        intents = Intents.nonPrivileged + Intent.MessageContent
    }
}
