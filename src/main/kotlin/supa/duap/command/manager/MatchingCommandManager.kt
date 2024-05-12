package supa.duap.command.manager

import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.optional
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.Member
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.user
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.koin.java.KoinJavaComponent.inject
import supa.duap.command.model.Command.MatchingCommand
import supa.duap.match.MatchMakingManager
import supa.duap.match.model.GameType
import supa.duap.match.model.MatchArgs
import supa.duap.match.model.Player

class MatchingCommandManager(kord : Kord) : CommandManager<MatchingCommand>(kord) {
    private val matchMakingManager : MatchMakingManager by inject(MatchMakingManager::class.java)

    override suspend fun registerCommand() {
        addCommand(MatchingCommand.CREATE_PROFILE)

        addCommand(MatchingCommand.SHOW_PROFILE) {
            user(
                name = MatchingCommand.SHOW_PROFILE.optionName1,
                description = "해당 사용자의 프로필을 출력해요."
            ).optional()
        }

        addCommand(MatchingCommand.CASUAL_GAME) {
            integer(
                name = MatchingCommand.CASUAL_GAME.optionName1,
                description = "자신과 상대방의 등급 차이 허용 한도를 설정해요. 기본값은 1이에요. 설정하지 않으려면 -1을 넣어주세요."
            ).optional()
        }
        addCommand(MatchingCommand.RANK_GAME) {
            integer(
                name = MatchingCommand.RANK_GAME.optionName1,
                description = "자신과 상대방의 등급 차이 허용 한도를 설정해요. 기본값은 1이에요. 설정하지 않으려면 -1을 넣어주세요."
            ).optional()
        }
        addCommand(MatchingCommand.RECORD_SCORE)
    }

    override suspend fun responseCommand(command : MatchingCommand, interaction : ChatInputCommandInteraction) {
        when (command) {
            MatchingCommand.CREATE_PROFILE -> registerProfile(interaction)
            MatchingCommand.SHOW_PROFILE -> showProfile(interaction)
            MatchingCommand.CASUAL_GAME -> registerGamePool(interaction, GameType.CASUAL)
            MatchingCommand.RANK_GAME -> registerGamePool(interaction, GameType.RANK)
            MatchingCommand.RECORD_SCORE -> registerScore(interaction)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun registerProfile(interaction : ChatInputCommandInteraction) {
        flow {
            val author = interaction.user.takeIf { it is Member } as Member? ?: run {
                throw Exception("누가 절 부르신거죠? 부르신 분을 못찾겠어요.")
            }

            emit(author)
        }
            .flatMapConcat { author ->
                matchMakingManager.createProfile(
                    author.id.value.toLong(),
                    author.mention
                )
            }
            .catch { e ->
                interaction.respondPublic { embed { description = e.message ?: "프로필 생성에 실패했어요." } }
            }
            .take(1)
            .onEach { player ->
                if (player == null) {
                    interaction.respondPublic { embed { description = "프로필 생성에 실패했어요." } }
                    return@onEach
                }

                interaction.respondPublic {
                    embed {
                        author {
                            name = "프로필을 생성했어요."
                        }

                        description = player.getPlayerInfo()
                    }
                }
            }
            .collect()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun showProfile(interaction : ChatInputCommandInteraction) {
        flow {
            val author = interaction.user.takeIf { it is Member } as Member? ?: run {
                throw Exception("누가 절 부르신거죠? 부르신 분을 못찾겠어요.")
            }

            emit(author)
        }
            .flatMapConcat { author ->
                val user = interaction.command.users[MatchingCommand.SHOW_PROFILE.optionName1] ?: author

                matchMakingManager.getProfile(user.id.value.toLong())
            }
            .catch { e ->
                interaction.respondPublic { embed { description = e.message ?: "프로필이 등록되지 않았어요." } }
            }
            .onEach { player ->
                if (player == null) {
                    interaction.respondPublic { embed { description = "프로필이 등록되지 않았어요." } }
                    return@onEach
                }

                interaction.respondPublic {
                    embed {
                        description = player.getPlayerInfo()
                    }
                }
            }
            .collect()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun registerGamePool(interaction : ChatInputCommandInteraction, gameType : GameType) {
        val author = interaction.user.takeIf { it is Member } as Member?
        flow {
            if (author == null) {
                throw Exception("누가 절 부르신거죠? 부르신 분을 못찾겠어요.")
            }

            emit(author)
        }
            .flatMapConcat {
                matchMakingManager.getProfile(it.id.value.toLong())
            }
            .onEach { player ->
                if (player == null) {
                    interaction.respondPublic { embed { description = "프로필이 등록되지 않았어요." } }
                    return@onEach
                }

                val rankAvailableRange = interaction.command.integers["등급 허용 한도"]?.toInt() ?: 1
                val matchArgs = MatchArgs(player.id, rankAvailableRange)

                matchMakingManager.addOnGameCreateListener(key = player) { game ->
                    if (game == null) {
                        interaction.channel.createMessage {
                            embed {
                                description = "${player.name} 상대방을 찾지 못해 매칭이 취소되었어요."
                            }
                        }
                        return@addOnGameCreateListener
                    }

                    val p1Mention = author?.guild?.getMemberOrNull(Snowflake(game.player1Id))?.mention
                    val p2Mention = author?.guild?.getMemberOrNull(Snowflake(game.player2Id))?.mention

                    interaction.channel.createMessage {
                        embed {
                            author {
                                name = "매칭됐어요."
                            }
                            description = "1P : $p1Mention\n2P : $p2Mention\n\n 방을 생성한 후 게임을 진행해주세요."
                        }
                    }
                }

                if (!matchMakingManager.addQueue(player, matchArgs, gameType)) {
                    throw Exception("이미 매칭에 등록되어 있어요.")
                }

                interaction.respondPublic {
                    val gameTypeName = when (gameType) {
                        GameType.CASUAL -> "캐주얼 게임"
                        GameType.RANK -> "랭크 게임"
                    }

                    embed {
                        description = "${author?.mention}님이 $gameTypeName 매칭에 등록했어요."
                    }
                }
            }
            .catch { e ->
                interaction.respondPublic { embed { description = e.message ?: "매칭 등록에 실패했어요." } }
            }
            .collect()
    }

    private suspend fun registerScore(interaction : ChatInputCommandInteraction) {
        interaction.respondPublic {
            embed {
                description = "개발중이라고 애송이"
            }
        }
    }

    private fun Player.getPlayerInfo() =
            "$name\n\n" +
            "등급 : $grade\n" +
            "점수 : $eloScore\n" +
            "\n" +
            "랭크 경기\n" +
            "경기수 : ${rankWinCount + rankLoseCount}\n" +
            "승리 : $rankWinCount\n" +
            "패배 : $rankLoseCount\n" +
            "\n" +
            "캐주얼 경기\n" +
            "경기수 : ${casualWinCount + casualLoseCount}\n" +
            "승리 : $casualWinCount\n" +
            "패배 : $casualLoseCount\n" +
            "\n" +
            "AFK : $afkCount"
}
