package supa.duap.command.manager

import dev.kord.common.entity.optional.optional
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.user
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.koin.java.KoinJavaComponent.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import supa.duap.command.model.Command.MatchingCommand
import supa.duap.match.MatchMakingManager
import supa.duap.match.model.GameType
import supa.duap.match.model.Grade
import supa.duap.match.model.MatchArgs
import supa.duap.match.model.PlayerProfile

class MatchingCommandManager(kord : Kord) : CommandManager<MatchingCommand>(kord) {
    private val logger : Logger = LoggerFactory.getLogger(this.javaClass)

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
                name = MatchingCommand.MatchRegisterCommand.optionName1,
                description = "자신과 상대방의 등급 차이 허용 한도를 설정해요. 기본값은 1이에요. 설정하지 않으려면 -1을 넣어주세요."
            ).optional()

            integer(
                name = MatchingCommand.MatchRegisterCommand.optionName2,
                description = "매칭 대기시간을 분단위로 설정해요.",
                builder = {
                    this.minValue = 0
                    this.maxValue = 10
                }
            ).optional()
        }

        addCommand(MatchingCommand.RANK_GAME) {
            integer(
                name = MatchingCommand.MatchRegisterCommand.optionName1,
                description = "자신과 상대방의 등급 차이 허용 한도를 설정해요. 기본값은 1이에요. 설정하지 않으려면 -1을 넣어주세요."
            ).optional()

            integer(
                name = MatchingCommand.MatchRegisterCommand.optionName2,
                description = "매칭 대기시간을 분단위로 설정해요.",
                builder = {
                    this.minValue = 0
                    this.maxValue = 10
                }
            ).optional()
        }

        addCommand(MatchingCommand.MATCH_INFO)

        addCommand(MatchingCommand.MATCH_CANCEL)

        addCommand(MatchingCommand.RECORD_GAME_RESULT) {
            integer(
                name = MatchingCommand.RECORD_GAME_RESULT.optionName1,
                description = "P1의 승리 수를 입력해주세요.",
                builder = {
                    minValue = 0
                }
            )

            integer(
                name = MatchingCommand.RECORD_GAME_RESULT.optionName2,
                description = "P2의 승리 수를 입력해주세요.",
                builder = {
                    minValue = 0
                }
            )
        }

        addCommand(MatchingCommand.SHOW_RANKING)
    }

    override suspend fun responseCommand(command : MatchingCommand, interaction : ChatInputCommandInteraction) {
        when (command) {
            MatchingCommand.CREATE_PROFILE -> registerProfile(interaction)
            MatchingCommand.SHOW_PROFILE -> showProfile(interaction)
            MatchingCommand.CASUAL_GAME -> registerGamePool(interaction, GameType.CASUAL)
            MatchingCommand.RANK_GAME -> registerGamePool(interaction, GameType.RANK)
            MatchingCommand.MATCH_INFO -> showMatchInfo(interaction)
            MatchingCommand.MATCH_CANCEL -> unregisterGamePool(interaction)
            MatchingCommand.RECORD_GAME_RESULT -> registerGameResult(interaction)
            MatchingCommand.SHOW_RANKING -> showRanking(interaction)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun registerProfile(interaction : ChatInputCommandInteraction) {
        val author = interaction.user.takeIf { it is Member } as Member? ?: run {
            throw Exception("누가 절 부르신거죠? 부르신 분을 못찾겠어요.")
        }

        flow {
            val list = author.roles.fold(mutableListOf<Role>()) { list, role -> list.apply { add(role) } }
            emit(Grade.getFromRole(list))
        }
            .flatMapConcat { grade ->
                matchMakingManager.createProfile(
                    author.id.value.toLong(),
                    author.mention,
                    grade
                )
            }
            .catch { e ->
                interaction.respondEphemeral { embed { description = e.message ?: "프로필 생성에 실패했어요." } }
            }
            .onEach { player ->
                if (player == null) {
                    interaction.respondEphemeral { embed { description = "프로필 생성에 실패했어요." } }
                    return@onEach
                }

                interaction.respondPublic {
                    embed {
                        author {
                            name = "프로필을 생성했어요."
                        }

                        description = player.print()
                    }
                }
            }
            .take(1)
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
                interaction.respondEphemeral { embed { description = e.message ?: "프로필이 등록되지 않았어요." } }
            }
            .onEach { player ->
                if (player == null) {
                    interaction.respondEphemeral { embed { description = "프로필이 등록되지 않았어요." } }
                    return@onEach
                }

                interaction.respondEphemeral {
                    embed {
                        description = player.print()
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
                matchMakingManager.getPlayer(it.id.value.toLong())
            }
            .flatMapConcat { player ->
                if (player == null) {
                    throw Exception("프로필이 등록되지 않았어요.")
                }

                matchMakingManager.getRunningGame(player.id).map { player to it }
            }
            .onEach { (player, runningGame) ->
                if (matchMakingManager.isRegistered(player)) {
                    throw Exception("이미 대기열에 등록되어있어요.")
                }

                if (runningGame != null) {
                    val other = if (runningGame.player1.id != player.id) {
                        runningGame.player1
                    } else {
                        runningGame.player2
                    }

                    throw Exception("이미 ${other.name}님과 게임을 진행중이에요.")
                }

                val rankAvailableRange =
                    interaction.command.integers[MatchingCommand.MatchRegisterCommand.optionName1]?.toInt() ?: 1
                val awaitTimeMinutes =
                    interaction.command.integers[MatchingCommand.MatchRegisterCommand.optionName2]?.toInt() ?: 0

                logger.debug("player : ${author?.globalName}, rankAvailableRange : $rankAvailableRange, awaitTimeMinutes : $awaitTimeMinutes")

                val matchArgs = MatchArgs(player.id, rankAvailableRange, awaitTimeMinutes)

                matchMakingManager.addOnGameCreateListener(key = player) { game ->
                    if (game == null) {
                        interaction.channel.createMessage {
                            embed {
                                description = "${player.name} 상대방을 찾지 못해 매칭이 취소되었어요."
                            }
                        }
                        return@addOnGameCreateListener
                    }

                    interaction.channel.createMessage {
                        embed {
                            author {
                                name = "매칭됐어요."
                            }
                            description =
                                "1P : ${game.player1.name}\n2P : ${game.player2.name}\n\n 방을 생성한 후 게임을 진행해주세요."
                        }
                    }
                }

                matchMakingManager.enqueue(player, matchArgs, gameType)

                interaction.respondPublic {
                    val gameTypeName = when (gameType) {
                        GameType.CASUAL -> "캐주얼 게임"
                        GameType.RANK -> "랭크 게임"
                    }

                    embed {
                        description = "${player.name}님이 $gameTypeName 대기열에 등록했어요."
                    }
                }
            }
            .catch { e ->
                e.printStackTrace()
                interaction.respondEphemeral { embed { description = e.message ?: "매칭 등록에 실패했어요." } }
            }
            .collect()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun showMatchInfo(interaction : ChatInputCommandInteraction) {
        flow {
            val author = interaction.user.takeIf { it is Member } as Member?

            if (author == null) {
                throw Exception("누가 절 부르신거죠? 부르신 분을 못찾겠어요.")
            }

            emit(author)
        }
            .filterNotNull()
            .flatMapConcat { author ->
                matchMakingManager.getRunningGame(author.id.value.toLong())
            }
            .onEach { runningGame ->
                if (runningGame == null) {
                    throw Exception("진행중인 게임이 없어요.")
                }

                interaction.respondEphemeral {
                    embed {
                        author {
                            name = "${runningGame.gameType.typeName}매치 게임중이에요."
                        }

                        description =
                            "1P : ${runningGame.player1.name}\n2P : ${runningGame.player2.name}"
                    }
                }
            }
            .catch { e ->
                e.printStackTrace()
                interaction.respondEphemeral { embed { description = e.message ?: "매치 정보를 찾지 못했어요." } }
            }
            .collect()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun unregisterGamePool(interaction : ChatInputCommandInteraction) {
        val author = interaction.user.takeIf { it is Member } as Member?

        flow {
            if (author == null) {
                throw Exception("누가 절 부르신거죠? 부르신 분을 못찾겠어요.")
            }

            emit(author)
        }
            .flatMapConcat {
                matchMakingManager.getPlayer(it.id.value.toLong())
            }
            .onEach { player ->
                if (player == null) {
                    interaction.respondEphemeral { embed { description = "프로필이 등록되지 않았어요." } }
                    return@onEach
                }

                if (!matchMakingManager.dequeue(player)) {
                    interaction.respondEphemeral {
                        embed {
                            description = "등록된 대기열이 없어요."
                        }
                    }
                    return@onEach
                }

                interaction.respondPublic {
                    embed {
                        description = "${player.name}님이 매칭을 취소했어요."
                    }
                }
            }
            .catch { e ->
                e.printStackTrace()
                interaction.respondEphemeral { embed { description = e.message ?: "매칭 취소에 실패했어요." } }
            }
            .collect()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun registerGameResult(interaction : ChatInputCommandInteraction) {
        flow {
            val author = interaction.user.takeIf { it is Member } as Member? ?: run {
                throw Exception("누가 절 부르신거죠? 부르신 분을 못찾겠어요.")
            }

            emit(author)
        }
            .flatMapConcat {
                val winCounts = interaction.command.integers

                val p1WinCount =
                    winCounts[MatchingCommand.RECORD_GAME_RESULT.optionName1] ?: throw Exception("매개변수가 잘못되었습니다.")
                val p2WinCount =
                    winCounts[MatchingCommand.RECORD_GAME_RESULT.optionName2] ?: throw Exception("매개변수가 잘못되었습니다.")

                matchMakingManager.registerGameScore(it.id.value.toLong(), p1WinCount.toInt(), p2WinCount.toInt())
            }
            .onEach { gameResult ->
                if (gameResult == null) {
                    throw Exception("게임 정보가 잘못되었어요.")
                }

                interaction.respondPublic {
                    embed {
                        description = buildString {
                            appendLine("게임 결과를 저장했어요.")
                            appendLine()

                            append("${gameResult.player1Name} (${if (gameResult.player1WinCount > gameResult.player2WinCount) { "승" } else { "패" }})")

                            if (gameResult.gameType == GameType.RANK) {
                                append(" 점수 : ${gameResult.player1EloScore} (${gameResult.player1EloScoreChange})")
                            }

                            appendLine()

                            append("${gameResult.player2Name} (${if (gameResult.player2WinCount > gameResult.player1WinCount) { "승" } else { "패" }})")

                            if (gameResult.gameType == GameType.RANK) {
                                append(" 점수 : ${gameResult.player2EloScore} (${gameResult.player2EloScoreChange})")
                            }
                        }
                    }
                }
            }
            .catch { e ->
                interaction.respondEphemeral { embed { description = e.message ?: "알 수 없는 오류가 발생했습니다." } }
            }
            .collect()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun showRanking(interaction : ChatInputCommandInteraction) {
        flow {
            val author = interaction.user.takeIf { it is Member } as Member? ?: run {
                throw Exception("누가 절 부르신거죠? 부르신 분을 못찾겠어요.")
            }

            emit(author)
        }
            .flatMapConcat {
                matchMakingManager.getPlayerRanking(it.id.value.toLong())
            }
            .onEach { rankInfo ->
                if (rankInfo == null) {
                    throw Exception("랭킹 정보 획득에 실패했습니다.")
                }

                interaction.respondEphemeral {
                    embed {
                        description = buildString {
                            rankInfo.top10.sortedByDescending { it.eloScore }.forEachIndexed { index, player ->
                                appendLine("${index + 1}위 : ${player.name} ${player.eloScore}점")
                            }

                            if (rankInfo.rank > 10) {
                                appendLine()
                                append("${rankInfo.rank}위 : ${rankInfo.player.name} ${rankInfo.player.eloScore}점")
                            }
                        }
                    }
                }
            }
            .catch { e ->
                e.printStackTrace()
                interaction.respondEphemeral { embed { description = e.message ?: "알 수 없는 오류가 발생했습니다." } }
            }
            .collect()
    }

    private fun PlayerProfile.print() =
        "$name\n\n" +
                "등급 : ${grade.gradeName}\n" +
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
                "패배 : $casualLoseCount\n"
}
