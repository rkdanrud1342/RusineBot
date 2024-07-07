package supa.duap.command.manager

import com.kotlindiscord.kord.extensions.utils.hasRole
import dev.kord.common.Locale
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.DeferredEphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.interaction.updatePublicMessage
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.rest.builder.component.ButtonBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koin.java.KoinJavaComponent.inject
import supa.duap.BaseCoroutine
import supa.duap.Grade
import supa.duap.RoleManager
import supa.duap.command.model.Command.MatchingCommand
import supa.duap.match.MatchMakingManager
import supa.duap.match.model.*
import java.util.*
import kotlin.math.roundToInt

class MatchingCommandManager(kord : Kord) : CommandManager<MatchingCommand>(kord) {
    private val coroutineScope = CoroutineScope(BaseCoroutine.default)

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

    private val deferredMessageMap = mutableMapOf<Player, DeferredEphemeralMessageInteractionResponseBehavior>()

    private val scoreCheckMap : MutableMap<Long, Boolean> = mutableMapOf()

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

        addCommand(MatchingCommand.SET_GRADE)
    }

    override suspend fun responseCommand(command : MatchingCommand, interaction : ChatInputCommandInteraction) {
        when (command) {
            MatchingCommand.CREATE_PROFILE -> registerProfile(interaction)
            MatchingCommand.SHOW_PROFILE -> showProfile(interaction)
            MatchingCommand.CASUAL_GAME -> registerMatchQueue(interaction, MatchType.CASUAL)
            MatchingCommand.RANK_GAME -> registerMatchQueue(interaction, MatchType.RANK)
            MatchingCommand.MATCH_INFO -> showMatchInfo(interaction)
            MatchingCommand.MATCH_CANCEL -> unregisterMatchPool(interaction)
            MatchingCommand.GAME_CANCEL -> matchCancel(interaction)
            MatchingCommand.RECORD_GAME_RESULT -> registerMatchScore(interaction)
            MatchingCommand.SHOW_RANKING -> showRanking(interaction)
            MatchingCommand.SET_GRADE -> setGrade(interaction)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun registerProfile(interaction : ChatInputCommandInteraction) {
        interaction.author.flatMapConcat { author ->
            val list = author.roles.fold(mutableListOf<Role>()) { list, role -> list.apply { add(role) } }
            val grade = Grade.getFromRole(*list.toTypedArray()) ?: throw Exception("격투 역할이 없군요. 역할 배정을 먼저 받아주세요!")

            matchMakingManager.createProfile(
                author.id.value.toLong(),
                author.mention,
                grade
            )
        }
            .onEach { player ->
                if (player == null) {
                    interaction.respondEphemeral { embed { description = "프로필 생성에 실패했습니다." } }
                    return@onEach
                }

                interaction.respondEphemeral {
                    embed {
                        author {
                            name = when (interaction.locale) {
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
            .catch { e ->
                interaction.respondEphemeral { embed { description = e.message ?: "프로필 생성에 실패했습니다." } }
            }
            .collect()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun showProfile(interaction : ChatInputCommandInteraction) {
        interaction.author
            .flatMapConcat { author ->
                val user = interaction.command.users[MatchingCommand.SHOW_PROFILE_OPTION1_NAME] ?: author

                matchMakingManager.getProfile(user.id.value.toLong())
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
            .catch { e ->
                interaction.respondEphemeral { embed { description = e.message ?: "프로필 검색에 실패했습니다." } }
            }
            .collect()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun registerMatchQueue(interaction : ChatInputCommandInteraction, matchType : MatchType) {
        interaction.author
            .flatMapConcat { author ->
                matchMakingManager.getPlayer(author.id.value.toLong())
            }
            .flatMapConcat { player ->
                if (player == null) {
                    throw Exception("선수 검색에 실패했습니다.")
                }

                if (matchMakingManager.isRegistered(player)) {
                    throw Exception("이미 대기열에 등록되어 있는 상태입니다.\nYou have already registered for the queue.")
                }

                when (matchType) {
                    MatchType.CASUAL -> flow { emit(player to null) }
                    MatchType.RANK -> matchMakingManager.getRunningMatch(player.id).map { player to it }
                }
            }
            .onEach { (player, runningMatch) ->
                if (runningMatch != null) {
                    val other = if (runningMatch.player1.id != player.id) {
                        runningMatch.player1
                    } else {
                        runningMatch.player2
                    }

                    throw Exception("이미 ${other.name} 선수와 대전을 진행중입니다.\nYou are playing match with ${other.name}.")
                }

                val awaitTimeMinutes =
                    interaction.command.integers[MatchingCommand.MATCH_REGISTER_COMMAND_OPTION1_NAME]?.toInt() ?: 10
                val rankAvailableRange =
                    interaction.command.integers[MatchingCommand.MATCH_REGISTER_COMMAND_OPTION2_NAME]?.toInt() ?: 1
                val needToMention =
                    interaction.command.strings[MatchingCommand.MATCH_REGISTER_COMMAND_OPTION3_NAME] ?: "Y"

                val matchArgs = MatchArgs(player.id, rankAvailableRange, awaitTimeMinutes)

                deferredMessageMap[player] = interaction.deferEphemeralResponse()

                matchMakingManager.addOnMatchCreateListener(key = player) { match, needToMakeThread ->
                    val deferredMessageBehavior = deferredMessageMap.remove(player) ?: return@addOnMatchCreateListener

                    if (match == null) {
                        deferredMessageBehavior.respond {
                            embed {
                                description = buildString {
                                    appendLine("대전 상대를 찾지 못해 대기열 등록을 취소합니다.")
                                    append("Unregister match queues because no other players were found.")
                                }
                            }
                        }

                        return@addOnMatchCreateListener
                    }

                    deferredMessageBehavior.respond {
                        embed {
                            author {
                                name = "Here Comes A New Challenger!"
                            }
                        }
                    }

                    if (!needToMakeThread) {
                        return@addOnMatchCreateListener
                    }

                    val (member1, member2) = (interaction.user as Member).getGuild()
                        .run { getMember(Snowflake(match.player1.id)) to getMember(Snowflake(match.player2.id)) }

                    (interaction.channel.asChannelOf<TextChannel>()).startPublicThread(name = "P1 ${member1.effectiveName} VS P2 ${member2.effectiveName}")
                        .apply {
                            addUser(member1.id)
                            addUser(member2.id)

                            createMessage {
                                content = "${member1.mention} VS ${member2.mention}"
                                embed {
                                    author {
                                        name = "Here comes a new challenger! 대전 상대가 결정되었습니다!"
                                    }

                                    description = buildString {
                                        appendLine(matchedMentList.random())
                                        appendLine()
                                        appendLine("1P : ${match.player1.name}")
                                        appendLine("2P : ${match.player2.name}")
                                        appendLine()
                                        appendLine("방을 생성하여 대전을 진행해주시기 바랍니다!")
                                        append("Please create a room and play Match!")
                                    }
                                }
                            }
                        }
                }

                val matchTypeName = when (matchType) {
                    MatchType.CASUAL -> "캐주얼 매치" to "casual match"
                    MatchType.RANK -> "랭크 매치" to "rank match"
                }

                if (needToMention == "Y" && rankAvailableRange != -1) {
                    matchMakingManager.addOnNotMatchedAtOnceListener(key = player) {
                        interaction.channel.createMessage {
                            content = roleManager.getMentionRoles(player, rankAvailableRange)
                                .joinToString(separator = " ") { it.mention }

                            embed {
                                description = buildString {
                                    appendLine("누군가가 ${matchTypeName.first}에서 겨룰 상대를 찾고 있습니다!")
                                    append("Someone is looking for an opponent in a ${matchTypeName.second}!")
                                }
                            }
                        }
                    }
                }

                matchMakingManager.enqueue(player, matchArgs, MatchType.RANK)
            }
            .catch { e ->
                e.printStackTrace()
                interaction.respondEphemeral { embed { description = e.message ?: "매칭 등록에 실패했습니다." } }
            }
            .collect()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun showMatchInfo(interaction : ChatInputCommandInteraction) {
        interaction.author
            .flatMapConcat { author ->
                matchMakingManager.getRunningMatch(author.id.value.toLong())
            }
            .onEach { runningMatch ->
                if (runningMatch == null) {
                    throw Exception("현재 진행중인 대전이 없습니다.")
                }

                interaction.respondEphemeral {
                    embed {
                        author {
                            name = "${runningMatch.matchType.typeName}매치 대전이 진행중입니다."
                        }

                        description = buildString {
                            appendLine("1P : ${runningMatch.player1.name}")
                            append("2P : ${runningMatch.player2.name}")
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
    private suspend fun unregisterMatchPool(interaction : ChatInputCommandInteraction) {
        interaction.author
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
                            description = buildString {
                                appendLine("현재 등록된 대기열이 없습니다.")
                                append("You are not registered at queue.")
                            }
                        }
                    }
                    return@onEach
                }

                interaction.respondEphemeral {
                    embed {
                        description = buildString {
                            appendLine("매칭 대기를 취소했습니다.")
                            append("You have been unregistered at queue.")
                        }
                    }
                }

                deferredMessageMap.remove(player)?.respond { embed { description = "매칭 대기를 취소했습니다." } }?.delete()
            }
            .catch { e ->
                e.printStackTrace()
                interaction.respondEphemeral {
                    embed {
                        description = e.message ?: buildString {
                            appendLine("매칭 취소에 실패했습니다.")
                            append("Failed to unregister.")
                        }
                    }
                }
            }
            .collect()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun matchCancel(interaction : ChatInputCommandInteraction) {
        interaction.author
            .flatMapConcat {
                matchMakingManager.cancelRunningMatch(it.id.value.toLong())
            }
            .onEach { matchResult ->
                if (matchResult == null) {
                    throw Exception("진행중인 게임이 없습니다.")
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
    private suspend fun registerMatchScore(interaction : ChatInputCommandInteraction) {
        val winCounts = interaction.command.integers
        val p1WinCount = winCounts[MatchingCommand.RECORD_GAME_RESULT_OPTION1_NAME]
        val p2WinCount = winCounts[MatchingCommand.RECORD_GAME_RESULT_OPTION2_NAME]

        interaction.author
            .flatMapConcat { author ->
                if (p1WinCount == null || p2WinCount == null) {
                    throw Exception(
                        when (interaction.locale) {
                            Locale.ENGLISH_UNITED_STATES -> "Wrong score has been entered."
                            Locale.JAPANESE -> "スコアが間違って入力されました。"
                            Locale.CHINESE_TAIWAN -> "分數輸入錯誤。"
                            else -> "점수가 잘못 입력되었습니다."
                        }
                    )
                }

                val isFt5 =
                    (p1WinCount == 5L && p1WinCount > p2WinCount) || (p2WinCount == 5L && p2WinCount > p1WinCount)

                if (!isFt5) {
                    throw Exception(
                        when (interaction.locale) {
                            Locale.ENGLISH_UNITED_STATES -> "Ranked match should be played with ft5."
                            Locale.JAPANESE -> "ランクマッチは5先勝で行う必要があります。"
                            Locale.CHINESE_TAIWAN -> "排名賽必須以5先勝進行。"
                            else -> "랭크게임은 5선승으로 진행되어야 합니다."
                        }
                    )
                }

                if (scoreCheckMap[author.id.value.toLong()] != null) {
                    throw Exception(
                        when (interaction.locale) {
                            Locale.ENGLISH_UNITED_STATES -> "The score registration is already in progress."
                            Locale.JAPANESE -> "すでにスコア登録が進行中です。"
                            Locale.CHINESE_TAIWAN -> "分數登記已經在進行中。"
                            else -> "이미 점수 등록이 진행중입니다."
                        }
                    )
                }

                matchMakingManager.getRunningMatch(author.id.value.toLong())
            }
            .onEach { runningMatch ->
                if (runningMatch == null) {
                    throw Exception("현재 진행중인 대전이 없습니다.")
                }

                if (scoreCheckMap[runningMatch.player1.id] != null || scoreCheckMap[runningMatch.player2.id] != null) {
                    throw Exception(
                        when (interaction.locale) {
                            Locale.ENGLISH_UNITED_STATES -> "The score registration is already in progress."
                            Locale.JAPANESE -> "すでにスコア登録が進行中です。"
                            Locale.CHINESE_TAIWAN -> "分數登記已經在進行中。"
                            else -> "이미 점수 등록이 진행중입니다."
                        }
                    )
                }

                scoreCheckMap[runningMatch.player1.id] = false
                scoreCheckMap[runningMatch.player2.id] = false

                val okButtonId = UUID.randomUUID().toString()
                val cancelButtonId = UUID.randomUUID().toString()

                val okButtonBuilder = ButtonBuilder.InteractionButtonBuilder(
                    style = ButtonStyle.Primary,
                    customId = okButtonId
                ).apply {
                    label = "확인(Confirm)"
                }

                val cancelButtonBuilder = ButtonBuilder.InteractionButtonBuilder(
                    style = ButtonStyle.Primary,
                    customId = cancelButtonId
                ).apply {
                    label = "취소(Cancel)"
                }

                lateinit var job : Job

                suspend fun registerScore(runningMatch : RunningMatch, authorId : Long, isDelayed : Boolean) {
                    scoreCheckMap.remove(runningMatch.player1.id)
                    scoreCheckMap.remove(runningMatch.player2.id)

                    matchMakingManager.registerMatchScore(
                        playerId = authorId,
                        p1Score = p1WinCount!!.toInt(),
                        p2Score = p2WinCount!!.toInt()
                    )
                        .onEach { matchResult ->
                            if (matchResult == null) {
                                throw Exception("게임 정보가 잘못되었어요.")
                            }

                            if (matchResult.matchType == MatchType.RANK) {
                                (interaction.user as Member).guild.run {
                                    val player1Member = getMember(Snowflake(matchResult.player1Id))

                                    val player2Member = getMember(Snowflake(matchResult.player2Id))

                                    val player1Role =
                                        roleManager.getRoleFromGrade(Grade.getGrade(matchResult.player1EloScore.roundToInt()))
                                    val player2Role =
                                        roleManager.getRoleFromGrade(Grade.getGrade(matchResult.player2EloScore.roundToInt()))

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
                            }

                            interaction.getOriginalInteractionResponse().edit {
                                actionRow {
                                    components.apply {
                                        add(okButtonBuilder.apply { this.disabled = true })
                                        add(cancelButtonBuilder.apply { this.disabled = true })
                                    }
                                }

                                if (isDelayed) {
                                    embed {
                                        author {
                                            name = "3분동안 응답이 없어 점수 등록이 완료되었습니다."
                                        }
                                    }
                                }
                            }

                            interaction.channel.createMessage {
                                embed {
                                    author {
                                        name = buildString {
                                            appendLine("대전 결과가 저장되었습니다!")
                                            append("Match result has been saved!")
                                        }
                                    }
                                    description = buildString {
                                        appendLine("${matchResult.player1Name} ${matchResult.player1WinCount} : ${matchResult.player2WinCount} ${matchResult.player2Name}")

                                        appendLine()

                                        val winLose =
                                            if (matchResult.player1WinCount > matchResult.player2WinCount) {
                                                "Win" to "Lose"
                                            } else {
                                                "Lose" to "Win"
                                            }

                                        append("${matchResult.player1Name} (${winLose.first})")

                                        if (matchResult.matchType == MatchType.RANK) {
                                            append(" Score : ${matchResult.player1EloScore.roundToInt()} (${matchResult.player1EloScoreChange.roundToInt()})")
                                        }

                                        appendLine()

                                        append("${matchResult.player2Name} (${winLose.second})")

                                        if (matchResult.matchType == MatchType.RANK) {
                                            append(" Score : ${matchResult.player2EloScore.roundToInt()} (${matchResult.player2EloScoreChange.roundToInt()})")
                                        }
                                    }
                                }
                            }
                        }
                        .collect()

                    if (!job.isCancelled) {
                        job.cancel()
                    }
                }

                val awaitingJob = coroutineScope.launch(BaseCoroutine.default) {
                    delay(1000 * 60 * 3) // 3 minutes
                    job.cancel()

                    registerScore(runningMatch, interaction.user.id.value.toLong(), true)
                }

                val onButtonClickListener : suspend (ButtonInteractionCreateEvent) -> Unit =
                    onButtonClickListener@{ event ->
                        val authorId = event.interaction.user.id.value.toLong()

                        if (authorId != runningMatch.player1.id && authorId != runningMatch.player2.id) {
                            event.interaction.updatePublicMessage { }
                            return@onButtonClickListener
                        }

                        when (event.interaction.component.customId) {
                            cancelButtonId -> {
                                awaitingJob.cancel()

                                event.interaction.updatePublicMessage {
                                    actionRow {
                                        components.apply {
                                            add(okButtonBuilder.apply { this.disabled = true })
                                            add(cancelButtonBuilder.apply { this.disabled = true })
                                        }
                                    }
                                }

                                event.interaction.channel.createMessage {
                                    embed {
                                        author {
                                            name = "점수 등록이 취소되었습니다."
                                        }
                                    }
                                }

                                scoreCheckMap.remove(runningMatch.player1.id)
                                scoreCheckMap.remove(runningMatch.player2.id)

                                job.cancel()
                            }

                            okButtonId -> {
                                if (scoreCheckMap[authorId] == true) {
                                    event.interaction.updatePublicMessage { }
                                    return@onButtonClickListener
                                }

                                scoreCheckMap[authorId] = true

                                event.interaction.channel.createMessage {
                                    embed {
                                        description = buildString {
                                            appendLine("${event.interaction.user.mention}님이 점수를 확인하셨습니다.")
                                            append("${event.interaction.user.mention} confirmed the score.")
                                        }
                                    }
                                }

                                val p1Confirmed = scoreCheckMap[runningMatch.player1.id] ?: false
                                val p2Confirmed = scoreCheckMap[runningMatch.player2.id] ?: false

                                if (!p1Confirmed || !p2Confirmed) {
                                    event.interaction.updatePublicMessage { }
                                    return@onButtonClickListener
                                }

                                awaitingJob.cancel()

                                registerScore(runningMatch, interaction.user.id.value.toLong(), false)
                            }
                        }
                    }

                job = kord.events
                    .filterIsInstance(ButtonInteractionCreateEvent::class)
                    .filter { it.interaction.component.customId in listOf(okButtonId, cancelButtonId) }
                    .onEach(onButtonClickListener)
                    .flowOn(BaseCoroutine.default)
                    .launchIn(coroutineScope)

                interaction.respondPublic {
                    embed {
                        author {
                            name = buildString {
                                appendLine("점수를 확인해주세요!")
                                append("Please, Check the score!")
                            }
                        }

                        this.description = buildString {
                            appendLine("${runningMatch.player1.name} $p1WinCount : $p2WinCount ${runningMatch.player2.name}")
                            appendLine()
                            appendLine("점수가 확정된 이후에는 변경할 수 없습니다! 신중히 검토해주세요!")
                            appendLine("3분동안 두 선수의 확인이 없는 경우 자동으로 등록됩니다!")
                            appendLine("You cannot change the score after it has been confirmed! Please check it carefully!")
                            appendLine("If there is no both players' confirmation, the score will be registered automatically!")
                        }
                    }

                    actionRow {
                        components.apply {
                            add(okButtonBuilder)
                            add(cancelButtonBuilder)
                        }
                    }
                }
            }
            .catch { e ->
                e.printStackTrace()
                interaction.respondEphemeral { embed { description = e.message ?: "점수 등록 요청에 실패했습니다." } }
            }
            .collect()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun showRanking(interaction : ChatInputCommandInteraction) {
        interaction.author
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

    private suspend fun setGrade(interaction : ChatInputCommandInteraction) {
        val target = interaction.command.users[MatchingCommand.SET_GRADE_OPTION1_NAME] ?: throw Exception("사용자를 찾을 수 없습니다.")
        val newRole = interaction.command.roles[MatchingCommand.SET_GRADE_OPTION2_NAME] ?: throw Exception("역할이 잘못되었습니다.")
        val grade = Grade.getFromRole(newRole) ?: throw Exception("역할이 잘못되었습니다.")

        matchMakingManager.setPlayerGrade(target.id.value.toLong(), grade.ordinal)
            .onEach { player ->
                if (player == null) {
                    interaction.respondEphemeral { embed { description = "등급 변경에 실패했습니다." } }
                    return@onEach
                }

                (target as Member).let { member ->
                    roleManager.fighterRoles.forEach { oldRole ->
                        if (member.hasRole(oldRole)) {
                            member.removeRole(oldRole.id)
                        }
                    }

                    member.addRole(newRole.id)
                }

                interaction.respondEphemeral {
                    embed {
                        author {
                            name = "등급이 변경되었습니다."
                        }

                        description = player.format(interaction.locale)
                    }
                }
            }
            .catch { e ->
                interaction.respondEphemeral { embed { description = e.message ?: "등급 변경에 실패했습니다." } }
            }
            .collect()
    }

    private fun PlayerProfile.format(locale : Locale?) : String {
        val gradeLabel : String
        val scoreLabel : String
        val rankMatchLabel : String
        val casualMatchLabel : String
        val matchPlayCountLabel : String
        val winCountLabel : String
        val loseCountLabel : String

        when (locale) {
            Locale.ENGLISH_UNITED_STATES -> {
                gradeLabel = "Grade"
                scoreLabel = "Score"
                rankMatchLabel = "Rank Match"
                casualMatchLabel = "Casual Match"
                matchPlayCountLabel = "Played"
                winCountLabel = "Win"
                loseCountLabel = "Lose"
            }

            Locale.JAPANESE -> {
                gradeLabel = "等級"
                scoreLabel = "点数"
                rankMatchLabel = "ランクマッチ"
                casualMatchLabel = "カジュアルマッチ"
                matchPlayCountLabel = "ゲームの回数"
                winCountLabel = "勝利"
                loseCountLabel = "敗北"
            }

            Locale.CHINESE_TAIWAN -> {
                gradeLabel = "檔次"
                scoreLabel = "分數"
                rankMatchLabel = "排名賽"
                casualMatchLabel = "休閒比賽"
                matchPlayCountLabel = "遊戲次數"
                winCountLabel = "勝利"
                loseCountLabel = "敗北"
            }

            else -> {
                gradeLabel = "등급"
                scoreLabel = "점수"
                rankMatchLabel = "랭크 매치"
                casualMatchLabel = "캐주얼 매치"
                matchPlayCountLabel = "게임 횟수"
                winCountLabel = "승리"
                loseCountLabel = "패배"
            }
        }

        return "$name\n\n" +
                "$gradeLabel : ${grade.gradeName}\n" +
                "$scoreLabel : $eloScore\n" +
                "\n" +
                "$rankMatchLabel\n" +
                "$matchPlayCountLabel : ${rankWinCount + rankLoseCount}\n" +
                "$winCountLabel : $rankWinCount\n" +
                "$loseCountLabel : $rankLoseCount\n" +
                "\n" +
                "$casualMatchLabel\n" +
                "$matchPlayCountLabel : ${casualWinCount + casualLoseCount}\n" +
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

    private val ChatInputCommandInteraction.author : Flow<Member>
        get() = flow {
            val author = user.takeIf { it is Member } as Member? ?: run {
                throw Exception("누가 절 부르신거죠? 부르신 분을 못찾겠어요.")
            }

            emit(author)
        }
}
