package supa.duap.command.model

import dev.kord.common.Locale
import dev.kord.common.entity.optional.optional
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user
import supa.duap.Grade

sealed class Command(
    val key : String,
    val description : String,
    val builder : GlobalChatInputCreateBuilder.() -> Unit = {}
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
        description : String,
        builder : GlobalChatInputCreateBuilder.() -> Unit = {}
    ) : Command(key, description, builder) {
        companion object {
            const val SHOW_PROFILE_OPTION1_NAME = "플레이어"

            const val MATCH_REGISTER_COMMAND_OPTION1_NAME = "대기시간"
            const val MATCH_REGISTER_COMMAND_OPTION2_NAME = "최대등급차"
            const val MATCH_REGISTER_COMMAND_OPTION3_NAME = "멘션여부"

            const val RECORD_GAME_RESULT_OPTION1_NAME = "p1승리수"
            const val RECORD_GAME_RESULT_OPTION2_NAME = "p2승리수"
        }
        data object CREATE_PROFILE : MatchingCommand(
            key = "프로필생성",
            description = "시합에 사용할 프로필을 생성합니다!",
            builder = {
                name(Locale.ENGLISH_UNITED_STATES, "create_profile")
                description(Locale.ENGLISH_UNITED_STATES, "Create a profile to use for competition!")

                name(Locale.JAPANESE, "プロフィール作成")
                description(Locale.JAPANESE, "試合に使用するプロファイルを作成します！")

                name(Locale.CHINESE_TAIWAN, "創建配置文件")
                description(Locale.CHINESE_TAIWAN, "建立個人資料用於比賽！")
            }
        )

        data object SHOW_PROFILE : MatchingCommand(
            key = "프로필보기",
            description = "프로필을 봅니다.",
            builder = {
                name(Locale.ENGLISH_UNITED_STATES, "view_profile")
                description(Locale.ENGLISH_UNITED_STATES, "Show a profile")

                name(Locale.JAPANESE, "プロフィール閲覧")
                description(Locale.JAPANESE, "プロファイルを表示します。")

                name(Locale.CHINESE_TAIWAN, "查看配置文件")
                description(Locale.CHINESE_TAIWAN, "查看簡歷。")

                user(
                    name = SHOW_PROFILE_OPTION1_NAME,
                    description = "해당 플레이어의 프로필을 보여드립니다!"
                ) {
                    name(Locale.ENGLISH_UNITED_STATES, "player")
                    description(Locale.ENGLISH_UNITED_STATES, "I'll show you the player's profile!")

                    name(Locale.JAPANESE, "プレイヤー")
                    description(Locale.JAPANESE, "そのプレイヤーのプロフィールを表示します！")

                    name(Locale.CHINESE_TAIWAN, "選手")
                    description(Locale.CHINESE_TAIWAN, "我們將向您展示玩家的個人資料！")
                }
            }
        )

        sealed interface MatchRegisterCommand {
            companion object {
                val builder : GlobalChatInputCreateBuilder.() -> Unit = {
                    integer(
                        name = MATCH_REGISTER_COMMAND_OPTION1_NAME,
                        description = "매칭 대기시간을 분단위로 설정합니다. 설정하지 않으려면 0을 입력하세요.",
                        builder = {
                            name(Locale.ENGLISH_UNITED_STATES, "waiting_time")
                            description(Locale.ENGLISH_UNITED_STATES, "Set the matching wait time in minutes. Enter 0 to unset.")

                            name(Locale.JAPANESE, "待ち時間")
                            description(Locale.JAPANESE, "マッチング待ち時間を分単位で設定します。設定しない場合は0を入力してください。")

                            name(Locale.CHINESE_TAIWAN, "等待的時間")
                            description(Locale.CHINESE_TAIWAN, "設定匹配的等待時間（以分鐘為單位）。如果不想設置，請輸入0。")

                            minValue = 0
                            maxValue = 10
                        }
                    )

                    integer(
                        name = MATCH_REGISTER_COMMAND_OPTION2_NAME,
                        description = "자신과 상대의 최대 등급 차이를 설정합니다. 기본값은 1입니다. 설정하지 않으려면 -1을 입력하세요.",
                        builder = {
                            name(Locale.ENGLISH_UNITED_STATES, "max_grade_difference")
                            description(Locale.ENGLISH_UNITED_STATES, "Set max grade difference with your opponent. Default 1. Enter -1 to unset.")

                            name(Locale.JAPANESE, "最大等級差")
                            description(Locale.JAPANESE, "自分と相手の間に最大の違いを設定します。 デフォルトは 1 です。 設定しない場合は -1 を入力します")

                            name(Locale.CHINESE_TAIWAN, "最大等級差")
                            description(Locale.CHINESE_TAIWAN, "設置自己和對手之間的最大等級差異。 默認值爲 1。 輸入-1，如果你不想設置它。")

                            minValue = -1
                            maxValue = Grade.entries.size.toLong()
                        }
                    ).optional()

                    string(
                        name = MATCH_REGISTER_COMMAND_OPTION3_NAME,
                        description = "최대등급차 내의 계급을 멘션합니다.",
                        builder = {
                            name(Locale.ENGLISH_UNITED_STATES, "mention_others")
                            description(Locale.ENGLISH_UNITED_STATES, "Mentions rank roles within the 'max_grade_difference'. No Effect when max_grade_difference is -1.")

                            name(Locale.JAPANESE, "呼び出すかどうか")
                            description(Locale.JAPANESE, "'最大等級差' オプション内のランク ロールについて説明します。 '最大等級差' が -1 の場合、効果はありません。")

                            name(Locale.CHINESE_TAIWAN, "通話狀態")
                            description(Locale.CHINESE_TAIWAN, "調用與'最大等級差'選項對應的等級。")

                            choice(name = "Y", value = "Y")
                            choice(name = "N", value = "N")
                        }
                    ).optional()
                }
            }
        }

        data object RANK_GAME : MatchingCommand(
            key = "랭크매치",
            description = "랭크매치 대기열에 등록합니다.",
            builder = {
                name(Locale.ENGLISH_UNITED_STATES, "rank_match")
                description(Locale.ENGLISH_UNITED_STATES, "Register for Rank match queue.")

                name(Locale.JAPANESE, "ランクマッチ")
                description(Locale.JAPANESE, "ランクマッチのキューに登録します。")

                name(Locale.CHINESE_TAIWAN, "排名賽")
                description(Locale.CHINESE_TAIWAN, "註冊排名匹配隊列。")

                MatchRegisterCommand.builder.invoke(this)
            }
        ), MatchRegisterCommand

        data object CASUAL_GAME : MatchingCommand(
            key = "캐주얼매치",
            description = "캐주얼매치 대기열에 등록합니다.",
            builder = {
                name(Locale.ENGLISH_UNITED_STATES, "casual_match")
                description(Locale.ENGLISH_UNITED_STATES, "Register for Casual match queue.")

                name(Locale.JAPANESE, "カジュアルマッチ")
                description(Locale.JAPANESE, "カジュアルマッチのキューに登録します。")

                name(Locale.CHINESE_TAIWAN, "休閒比賽")
                description(Locale.CHINESE_TAIWAN, "註冊休閒比賽排隊。")

                MatchRegisterCommand.builder.invoke(this)
            }
        ), MatchRegisterCommand

        data object MATCH_INFO : MatchingCommand(
            key = "게임정보",
            description = "현재 진행중인 게임 정보를 표시합니다.",
            builder = {
                name(Locale.ENGLISH_UNITED_STATES, "game_info")
                description(Locale.ENGLISH_UNITED_STATES, "Displays information about matched game.")

                name(Locale.JAPANESE, "ゲーム情報")
                description(Locale.JAPANESE, "現在進行中のゲーム情報を表示します。")

                name(Locale.CHINESE_TAIWAN, "遊戲信息")
                description(Locale.CHINESE_TAIWAN, "顯示當前進行中的遊戲資訊。")
            }
        )

        data object MATCH_CANCEL : MatchingCommand(
            key = "매칭취소",
            description = "매치 대기열 등록를 취소합니다.",
            builder = {
                name(Locale.ENGLISH_UNITED_STATES, "match_cancel")
                description(Locale.ENGLISH_UNITED_STATES, "Unregister match queues.")

                name(Locale.JAPANESE, "マッチングキャンセル")
                description(Locale.JAPANESE, "マッチキューの登録をキャンセルします。")

                name(Locale.CHINESE_TAIWAN, "取消比賽")
                description(Locale.CHINESE_TAIWAN, "取消註冊匹配隊列。")
            }
        )

        data object GAME_CANCEL : MatchingCommand(
            key = "게임취소",
            description = "현재 진행중인 게임을 취소합니다.",
            builder = {
                name(Locale.ENGLISH_UNITED_STATES, "game_cancel")
                description(Locale.ENGLISH_UNITED_STATES, "Cancel game that is currently in progress.")

                name(Locale.JAPANESE, "ゲームキャンセル")
                description(Locale.JAPANESE, "現在進行中のゲームをキャンセルします。")

                name(Locale.CHINESE_TAIWAN, "取消遊戲")
                description(Locale.CHINESE_TAIWAN, "取消目前正在進行的遊戲。")
            }
        )

        data object RECORD_GAME_RESULT : MatchingCommand(
            key = "점수등록",
            description = "게임 점수를 등록합니다.",
            builder = {
                name(Locale.ENGLISH_UNITED_STATES, "register_score")
                description(Locale.ENGLISH_UNITED_STATES, "Register the game score.")

                name(Locale.JAPANESE, "点数登録")
                description(Locale.JAPANESE, "ゲームスコアを登録します。")

                name(Locale.CHINESE_TAIWAN, "成績登記")
                description(Locale.CHINESE_TAIWAN, "登記遊戲分數。")

                integer(
                    name = RECORD_GAME_RESULT_OPTION1_NAME,
                    description = "P1의 승리 수를 입력해주세요.",
                    builder = {
                        name(Locale.ENGLISH_UNITED_STATES, "p1_score")
                        description(Locale.ENGLISH_UNITED_STATES, "Input P1's score.")

                        name(Locale.JAPANESE, "p1スコア")
                        description(Locale.JAPANESE, "P1の点数の入力をお願いします。")

                        name(Locale.CHINESE_TAIWAN, "p1評分")
                        description(Locale.CHINESE_TAIWAN, "請輸入P1的分數。")

                        minValue = 0
                        maxValue = 5
                    }
                )

                integer(
                    name = RECORD_GAME_RESULT_OPTION2_NAME,
                    description = "P2의 승리 수를 입력해주세요.",
                    builder = {
                        name(Locale.ENGLISH_UNITED_STATES, "p2_score")
                        description(Locale.ENGLISH_UNITED_STATES, "Input P2's score.")

                        name(Locale.JAPANESE, "p2スコア")
                        description(Locale.JAPANESE, "P2の点数の入力をお願いします。")

                        name(Locale.CHINESE_TAIWAN, "p2評分")
                        description(Locale.CHINESE_TAIWAN, "請輸入P2的分數。")

                        minValue = 0
                        maxValue = 5
                    }
                )
            }
        )

        data object SHOW_RANKING : MatchingCommand(
            key = "순위표",
            description = "순위표 및 본인의 순위를 봅니다.",
            builder = {
                name(Locale.ENGLISH_UNITED_STATES, "ranking_table")
                description(Locale.ENGLISH_UNITED_STATES, "Show ranking table and your ranking.")

                name(Locale.JAPANESE, "順位表")
                description(Locale.JAPANESE, "順位表および本人の順位を見ます。")

                name(Locale.CHINESE_TAIWAN, "名次表")
                description(Locale.CHINESE_TAIWAN, "顯示排名表及本人的排名。")
            }
        )
    }
}
