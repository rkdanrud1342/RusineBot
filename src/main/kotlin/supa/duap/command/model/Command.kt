package supa.duap.command.model

sealed class Command(
    val key : String,
    val description : String
) {
    sealed class BasicCommand(
        key : String,
        description : String
    ) : Command(key, description) {
        data object PING : BasicCommand("핑", "퐁해줘요.")
    }

    sealed class MusicCommand(
        key : String,
        description : String
    ) : Command(key, description) {
        data object PLAY : MusicCommand(
            key = "틀어",
            description = "음원을 재생해요. 다른 음원이 재생중이라면 재생목록에 추가해요."
        )

        data object SKIP : MusicCommand(
            key = "스킵",
            description = "재생중인 음원을 건너뛰어요."
        )

        data object LIST : MusicCommand(
            key = "재생목록",
            description = "재생목록을 표시해요."
        )

        data object REMOVE : MusicCommand(
            key = "지워",
            description = "재생목록에서 음원을 삭제해요."
        )

        data object DROP : MusicCommand(
            key = "다지워",
            description = "재생목록에서 모든 음원을 삭제해요."
        )
    }
}
