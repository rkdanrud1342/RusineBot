package supa.duap.command.manager

import dev.kord.common.Locale
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.message.embed
import io.ktor.util.logging.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.koin.java.KoinJavaComponent.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import supa.duap.Grade
import supa.duap.RoleManager
import supa.duap.command.model.Command.MatchingCommand
import supa.duap.match.MatchMakingManager
import supa.duap.match.model.GameType
import supa.duap.match.model.MatchArgs
import supa.duap.match.model.Player
import supa.duap.match.model.PlayerProfile

class MatchingCommandManager(kord : Kord) : CommandManager<MatchingCommand>(kord) {
    private val logger : Logger = LoggerFactory.getLogger(this.javaClass)

    private val matchMakingManager : MatchMakingManager by inject(MatchMakingManager::class.java)
    private val roleManager : RoleManager by inject(RoleManager::class.java)

    private val matchedMentList = listOf(
        "역사상 유례없는 대결이 곧 시작됩니다!",
        "모든 실력을 시험해보고 전설에 걸맞은 싸움을 보여주십시오!",
        "세계 최고의 격투 축제인 KOF에 오신 것을 환영합니다!",
        "대전 격투의 새로운 시대가 열리는 것을 목도하십시오!",
        "이 선수들은 최고 중의 최고입니다! Burn to Fight!",
        "과연 승리의 여신은 누구에게 미소 지을까요?",
        "꼭 봐야 할 대전이 될 것입니다! 눈도 깜빡하지 마세요!",
        "이 시합은 화제를 불러일으킬 것입니다!",
        "역사에 남을 경기가 될 것입니다!",
        "이제 곧 한바탕 파티가 펼쳐지겠군요!",
        "두 거성간의 위대한 전투를 준비하십시오!",
        "이제 막바지에 이르렀습니다! 승리는 누구의 손에?",
        "쇼의 스타들이 모두 모였습니다! 화끈하게 즐겨보시죠!",
        "지금 아니면 영원히 없을 시합입니다!",
        "여러분이 어떤 사람인지 보여주십시오!",
        "그 무엇도 이 쇼를 막을 수는 없습니다!",
        "Get Ready For The Next Battle!",
        "이 드라마가 어떻게 전개될지 한 번 확인해보시죠!"
    )

    override suspend fun registerCommand() {
        addCommand(MatchingCommand.CREATE_PROFILE)

        addCommand(MatchingCommand.SHOW_PROFILE)

        addCommand(MatchingCommand.CASUAL_GAME)

        addCommand(MatchingCommand.RANK_GAME)

        addCommand(MatchingCommand.MATCH_INFO)

        addCommand(MatchingCommand.MATCH_CANCEL)

        addCommand(MatchingCommand.GAME_CANCEL)

        addCommand(MatchingCommand.RECORD_GAME_RESULT)

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
            MatchingCommand.GAME_CANCEL -> gameCancel(interaction)
            MatchingCommand.RECORD_GAME_RESULT -> registerGameResult(interaction)
            MatchingCommand.SHOW_RANKING -> showRanking(interaction)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun registerProfile(interaction : ChatInputCommandInteraction) {
        val locale = interaction.locale

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
                interaction.respondEphemeral { embed { description = e.message ?: "프로필 생성에 실패했습니다." } }
            }
            .onEach { player ->
                if (player == null) {
                    interaction.respondEphemeral { embed { description = "프로필 생성에 실패했습니다." } }
                    return@onEach
                }

                interaction.respondEphemeral {
                    embed {
                        author {
                            name = when (locale) {
                                Locale.ENGLISH_UNITED_STATES -> "Profile has been Created!"
                                Locale.JAPANESE -> "プロフィールが作られました！"
                                Locale.CHINESE_TAIWAN -> "配置文件已創建！"
                                else -> "선수 프로필이 만들어졌습니다!"
                            }
                        }

                        description = player.format(interaction.locale)
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
                val user = interaction.command.users[MatchingCommand.SHOW_PROFILE_OPTION1_NAME] ?: author

                matchMakingManager.getProfile(user.id.value.toLong())
            }
            .catch { e ->
                interaction.respondEphemeral { embed { description = e.message ?: "프로필 검색에 실패했습니다." } }
            }
            .onEach { player ->
                if (player == null) {
                    interaction.respondEphemeral { embed { description = "프로필 검색에 실패했습니다." } }
                    return@onEach
                }

                interaction.respondEphemeral {
                    embed {
                        description = player.format(interaction.locale)
                    }
                }
            }
            .collect()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun registerGamePool(interaction : ChatInputCommandInteraction, gameType : GameType) {
        val author = interaction.user.takeIf { it is Member } as Member?

        if (author == null) {
            throw Exception("누가 절 부르신거죠? 부르신 분을 못찾겠어요.")
        }

        matchMakingManager.getPlayer(author.id.value.toLong())
            .flatMapConcat { player ->
                if (player == null) {
                    throw Exception("선수 검색에 실패했습니다.")
                }

                if (matchMakingManager.isRegistered(player)) {
                    throw Exception("이미 대기열에 등록되어 있는 상태입니다.\nYou have already registered for the queue.")
                }

                matchMakingManager.getRunningGame(player.id).map { player to it }
            }
            .onEach { (player, runningGame) ->
                if (runningGame != null) {
                    val other = if (runningGame.player1.id != player.id) {
                        runningGame.player1
                    } else {
                        runningGame.player2
                    }

                    throw Exception("이미 ${other.name} 선수와 대전을 진행중입니다.\nYou are playing game with ${other.name}.")
                }

                val awaitTimeMinutes =
                    interaction.command.integers[MatchingCommand.MATCH_REGISTER_COMMAND_OPTION1_NAME]?.toInt() ?: 0
                val rankAvailableRange =
                    interaction.command.integers[MatchingCommand.MATCH_REGISTER_COMMAND_OPTION2_NAME]?.toInt() ?: 1
                val needToMention =
                    interaction.command.strings[MatchingCommand.MATCH_REGISTER_COMMAND_OPTION3_NAME] ?: "Y"

                val matchArgs = MatchArgs(player.id, rankAvailableRange, awaitTimeMinutes)

                matchMakingManager.addOnGameCreateListener(key = player) { game ->
                    if (game == null) {
                        interaction.channel.createMessage {
                            content = player.name

                            embed {
                                description = buildString {
                                    appendLine("대전 상대를 찾지 못해 매칭대기열 등록을 취소합니다.")
                                    append("Unregister match queues because no other players were found.")
                                }
                            }
                        }
                        return@addOnGameCreateListener
                    }

                    interaction.channel.createMessage {
                        embed {
                            author {
                                name = "Here comes a new challenger! 대전 상대가 결정되었습니다!"

                            }

                            description = buildString {
                                appendLine(matchedMentList.random())
                                appendLine()
                                appendLine("1P : ${game.player1.name}")
                                appendLine("2P : ${game.player2.name}")
                                appendLine()
                                appendLine("방을 생성하여 대전을 진행해주시기 바랍니다!")
                                append("Please create a room and proceed with the Game!")
                            }
                        }
                    }

                    try {
                        val member1 = author.getGuild().getMember(Snowflake(game.player1.id))
                        val member2 = author.getGuild().getMember(Snowflake(game.player2.id))

                        (interaction.channel.asChannelOf<TextChannel>()).startPublicThread(name = "${member1.effectiveName} VS ${member2.effectiveName}")
                            .apply {
                                addUser(member1.id)
                                addUser(member2.id)
                            }
                    } catch (e : Exception) {
                        logger.error(e)
                    }
                }

                matchMakingManager.enqueue(player, matchArgs, gameType)

                interaction.respondPublic {
                    val gameTypeName = when (gameType) {
                        GameType.CASUAL -> "캐주얼 매치" to "casual match"
                        GameType.RANK -> "랭크 매치" to "rank match"
                    }

                    embed {
                        description = buildString {
                            appendLine("${player.name} 선수가 ${gameTypeName.first} 대기열에 합류했습니다!")
                            append("Player ${player.name} has joined ${gameTypeName.second} queue!")
                        }
                    }
                }

                if (needToMention == "Y" && rankAvailableRange != -1) {
                    interaction.channel.createMessage {
                        content = roleManager.getMentionRoles(player, rankAvailableRange)
                            .joinToString(separator = " ") { it.mention }

                        embed {
                            description = buildString {
                                appendLine("${player.name} 선수가 상대를 찾고있습니다! 이 선수를 상대할 선수는 과연 누가 될 것인가!")
                                append("Player ${player.name} is looking for an opponent! Who will face this player!")
                            }
                        }
                    }
                }
            }
            .catch { e ->
                e.printStackTrace()
                interaction.respondEphemeral { embed { description = e.message ?: "매칭 등록에 실패했습니다." } }
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
                    throw Exception("현재 진행중인 대전이 없습니다.")
                }

                interaction.respondEphemeral {
                    embed {
                        author {
                            name = "${runningGame.gameType.typeName}매치 대전이 진행중입니다."
                        }

                        description = buildString {
                            appendLine("1P : ${runningGame.player1.name}")
                            append("2P : ${runningGame.player2.name}")
                        }
                    }
                }
            }
            .catch { e ->
                e.printStackTrace()
                interaction.respondEphemeral { embed { description = e.message ?: "대전 정보를 찾지 못했습니다." } }
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
                    interaction.respondEphemeral { embed { description = "선수 검색에 실패했습니다." } }
                    return@onEach
                }

                if (!matchMakingManager.dequeue(player)) {
                    interaction.respondEphemeral {
                        embed {
                            description = "현재 등록된 대기열이 없습니다."
                        }
                    }
                    return@onEach
                }

                interaction.respondPublic {
                    embed {
                        description = "${player.name} 선수가 매칭 대기를 취소했습니다."
                    }
                }
            }
            .catch { e ->
                e.printStackTrace()
                interaction.respondEphemeral { embed { description = e.message ?: "매칭 취소에 실패했습니다." } }
            }
            .collect()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun gameCancel(interaction : ChatInputCommandInteraction) {
        flow {
            val author = interaction.user.takeIf { it is Member } as Member? ?: run {
                throw Exception("누가 절 부르신거죠? 부르신 분을 못찾겠어요.")
            }

            emit(author)
        }
            .flatMapConcat {
                matchMakingManager.cancelRunningGame(it.id.value.toLong())
            }
            .onEach { gameResult ->
                if (gameResult == null) {
                    throw Exception("게임 정보가 잘못되었습니다.")
                }

                interaction.respondPublic {
                    embed { description = "이런! 대전이 취소되었습니다." }
                }
            }
            .catch { e ->
                e.printStackTrace()
                interaction.respondEphemeral { embed { description = e.message ?: "알 수 없는 오류가 발생했습니다." } }
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

                val errorMessage = when (interaction.locale) {
                    Locale.ENGLISH_UNITED_STATES -> "Wrong score has been entered."
                    Locale.JAPANESE -> "スコアが間違って入力されました。"
                    Locale.CHINESE_TAIWAN -> "分數輸入錯誤。"
                    else -> "점수가 잘못 입력되었습니다."
                }

                val p1WinCount = winCounts[MatchingCommand.RECORD_GAME_RESULT_OPTION1_NAME] ?: throw Exception(errorMessage)
                val p2WinCount = winCounts[MatchingCommand.RECORD_GAME_RESULT_OPTION2_NAME] ?: throw Exception(errorMessage)

                matchMakingManager.registerGameScore(it.id.value.toLong(), p1WinCount.toInt(), p2WinCount.toInt())
            }
            .onEach { gameResult ->
                if (gameResult == null) {
                    throw Exception("게임 정보가 잘못되었어요.")
                }

                /*if (gameResult.gameType == GameType.RANK) {
                    (interaction.user as Member).guild.run {
                        val player1Member = getMember(Snowflake(gameResult.player1Id))

                        val player2Member = getMember(Snowflake(gameResult.player2Id))

                        val player1Role = roleManager.getRoleFromGrade(Grade.getGrade(gameResult.player1EloScore.roundToInt()))
                        val player2Role = roleManager.getRoleFromGrade(Grade.getGrade(gameResult.player2EloScore.roundToInt()))

                        if (!player1Member.hasRole(player1Role)) {
                            roleManager.fighterRoles.forEach { role ->
                                if (player1Member.hasRole(role)) {
                                    player1Member.removeRole(role.id, "등급 변경")
                                }
                            }

                            player1Member.addRole(player1Role.id, "등급 변경")
                        }

                        if (!player2Member.hasRole(player2Role)) {
                            roleManager.fighterRoles.forEach { role ->
                                if (player2Member.hasRole(role)) {
                                    player2Member.removeRole(role.id, "등급 변경")
                                }
                            }

                            player2Member.addRole(player2Role.id, "등급 변경")
                        }
                    }
                }*/

                interaction.respondPublic {
                    embed {
                        author {
                            name = "대전 결과가 성공적으로 저장되었습니다!"
                        }
                        description = buildString {
                            appendLine("${gameResult.player1Name} ${gameResult.player1WinCount} : ${gameResult.player2WinCount} ${gameResult.player2Name}")

                            appendLine()

                            append(
                                "${gameResult.player1Name} (${
                                    if (gameResult.player1WinCount > gameResult.player2WinCount) {
                                        "Win"
                                    } else {
                                        "Lose"
                                    }
                                })"
                            )

                            if (gameResult.gameType == GameType.RANK) {
                                append(" Score : ${gameResult.player1EloScore} (${gameResult.player1EloScoreChange})")
                            }

                            appendLine()

                            append(
                                "${gameResult.player2Name} (${
                                    if (gameResult.player2WinCount > gameResult.player1WinCount) {
                                        "Win"
                                    } else {
                                        "Lose"
                                    }
                                })"
                            )

                            if (gameResult.gameType == GameType.RANK) {
                                append(" Score : ${gameResult.player2EloScore} (${gameResult.player2EloScoreChange})")
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
                                appendLine(player.getRankFormat(index + 1, interaction.locale))
                                appendLine()
                            }

                            if (rankInfo.rank > 10) {
                                appendLine()
                                append(rankInfo.player.getRankFormat(rankInfo.rank, interaction.locale))
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

    private fun PlayerProfile.format(locale : Locale?) : String {
        val gradeLabel : String
        val scoreLabel : String
        val rankGameLabel : String
        val casualGameLabel : String
        val gamePlayCountLabel : String
        val winCountLabel : String
        val loseCountLabel : String

        when (locale) {
            Locale.ENGLISH_UNITED_STATES -> {
                gradeLabel = "Grade"
                scoreLabel = "Score"
                rankGameLabel = "Rank Match"
                casualGameLabel = "Casual Match"
                gamePlayCountLabel = "Played"
                winCountLabel = "Win"
                loseCountLabel = "Lose"
            }

            Locale.JAPANESE -> {
                gradeLabel = "等級"
                scoreLabel = "点数"
                rankGameLabel = "ランクマッチ"
                casualGameLabel = "カジュアルマッチ"
                gamePlayCountLabel = "ゲームの回数"
                winCountLabel = "勝利"
                loseCountLabel = "敗北"
            }

            Locale.CHINESE_TAIWAN -> {
                gradeLabel = "檔次"
                scoreLabel = "分數"
                rankGameLabel = "排名賽"
                casualGameLabel = "休閒比賽"
                gamePlayCountLabel = "遊戲次數"
                winCountLabel = "勝利"
                loseCountLabel = "敗北"
            }

            else -> {
                gradeLabel = "등급"
                scoreLabel = "점수"
                rankGameLabel = "랭크 매치"
                casualGameLabel = "캐주얼 매치"
                gamePlayCountLabel = "게임 횟수"
                winCountLabel = "승리"
                loseCountLabel = "패배"
            }
        }

        return "$name\n\n" +
                "$gradeLabel : ${grade.gradeName}\n" +
                "$scoreLabel : $eloScore\n" +
                "\n" +
                "$rankGameLabel\n" +
                "$gamePlayCountLabel : ${rankWinCount + rankLoseCount}\n" +
                "$winCountLabel : $rankWinCount\n" +
                "$loseCountLabel : $rankLoseCount\n" +
                "\n" +
                "$casualGameLabel\n" +
                "$gamePlayCountLabel : ${casualWinCount + casualLoseCount}\n" +
                "$winCountLabel : $casualWinCount\n" +
                "$loseCountLabel : $casualLoseCount\n"
    }

    private fun Player.getRankFormat(rank : Int, locale : Locale?) : String =
        when (locale) {
            Locale.ENGLISH_UNITED_STATES -> {
                val rankSuffix = when (rank) {
                    1 -> "st"
                    2 -> "nd"
                    else -> "th"
                }

                "${rank}${rankSuffix} : $name, score : $eloScore"
            }

            Locale.JAPANESE -> "${rank}位 : $name ${eloScore}点"

            Locale.CHINESE_TAIWAN -> "${rank}位 : $name ${eloScore}分"

            else -> "${rank}위 : $name ${eloScore}점"
        }
}
