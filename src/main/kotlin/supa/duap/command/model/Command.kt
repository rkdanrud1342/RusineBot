package supa.duap.command.model

import dev.kord.common.Locale
import dev.kord.common.entity.optional.optional
import dev.kord.rest.builder.interaction.*
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

    sealed class MatchingCommand(
        key : String,
        description : String,
        builder : GlobalChatInputCreateBuilder.() -> Unit = {}
    ) : Command(key, description, builder) {
        companion object {
            const val OPTION_NAME_PLAYER = "플레이어"

            const val OPTION_NAME_AWAIT_TIME = "대기시간"
            const val OPTION_NAME_MAX_GRADE_DIFF = "최대등급차"
            const val OPTION_NAME_MENTION_YN = "멘션여부"

            const val OPTION_NAME_P1_WIN_COUNT = "p1승리수"
            const val OPTION_NAME_P2_WIN_COUNT = "p2승리수"

            const val OPTION_NAME_USER = "사용자"
            const val OPTION_NAME_GRADE = "등급"
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
                    name = OPTION_NAME_PLAYER,
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

        data object DELETE_PROFILE : MatchingCommand(
            key = "프로필삭제",
            description = "사용자의 프로필을 삭제합니다.",
            builder = {
                user(
                    name = OPTION_NAME_USER,
                    description = "프로필을 삭제할 사용자"
                )
            }
        )

        sealed interface MatchRegisterCommand {
            companion object {
                val builder : GlobalChatInputCreateBuilder.() -> Unit = {
                    integer(
                        name = OPTION_NAME_AWAIT_TIME,
                        description = "매칭 대기시간을 분단위로 설정합니다. 기본 : 10분, 최소 1분, 최대 60분.",
                        builder = {
                            name(Locale.ENGLISH_UNITED_STATES, "waiting_time")
                            description(Locale.ENGLISH_UNITED_STATES, "Set the matching wait time in minutes. Default : 10min, Min : 1min, Max : 60min.")

                            name(Locale.JAPANESE, "待ち時間")
                            description(Locale.JAPANESE, "マッチング待ち時間を分単位で設定します。基本 : 10分、最小 : 1分、最大 : 60分。")

                            name(Locale.CHINESE_TAIWAN, "等待的時間")
                            description(Locale.CHINESE_TAIWAN, "設定匹配的等待時間（以分鐘為單位）。基本 : 10分鐘，最少 : 1分鐘，最多 : 60分鐘。")

                            minValue = 1
                            maxValue = 60
                        }
                    )

                    integer(
                        name = OPTION_NAME_MAX_GRADE_DIFF,
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
                        name = OPTION_NAME_MENTION_YN,
                        description = "최대등급차 내의 계급을 멘션합니다.",
                        builder = {
                            name(Locale.ENGLISH_UNITED_STATES, "mention_others")
                            description(Locale.ENGLISH_UNITED_STATES, "Mentions rank roles within the 'max_grade_difference'. No Effect when max_grade_difference is -1.")

                            name(Locale.JAPANESE, "呼び出すかどうか")
                            description(Locale.JAPANESE, "'最大等級差' オプション内のランク ロールについて説明します。 '最大等級差' が -1 の場合、効果はありません。")

                            name(Locale.CHINESE_TAIWAN, "是否通知")
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
                name(Locale.ENGLISH_UNITED_STATES, "ranking_match")
                description(Locale.ENGLISH_UNITED_STATES, "Register for Ranking match queue.")

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
                    name = OPTION_NAME_P1_WIN_COUNT,
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
                    name = OPTION_NAME_P2_WIN_COUNT,
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

        data object SET_GRADE : MatchingCommand(
            key = "등급설정",
            description = "유저의 등급을 설정합니다. [!!경고!!] 기존에 기록된 점수가 설정 등급의 기본 점수로 변경되니 유의하세요.",
            builder = {
                user(
                    name = OPTION_NAME_USER,
                    description = "등급을 변경할 사용자"
                )

                role(
                    name = OPTION_NAME_GRADE,
                    description = "사용자에게 설정할 등급"
                )
            }
        )
    }
}
