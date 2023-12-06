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

    sealed class MatchingCommand(
        key : String,
        description : String
    ) : Command(key, description) {
        data object CREATE_PROFILE : MatchingCommand(
            key = "프로필생성",
            description = "프로필을 등록합니다."
        )

        data object SHOP_PROFILE : MatchingCommand(
            key = "프로필보기",
            description = "프로필을 봅니다."
        )

        data object RANK_GAME : MatchingCommand(
            key = "랭크매치",
            description = "랭크매치 대기열에 등록합니다."
        )

        data object CASUAL_GAME : MatchingCommand(
            key = "캐주얼매치",
            description = "캐주얼매치 대기열에 등록합니다."
        )

        data object RECORD_SCORE : MatchingCommand(
            key = "점수등록",
            description = "랭크매치 종료 후 점수를 등록합니다."
        )
    }
}
