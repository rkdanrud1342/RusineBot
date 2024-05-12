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
            this.
            user(
                name = "사용자",
                description = "해당 사용자의 프로필을 출력해요."
            ).optional()
        }

        addCommand(MatchingCommand.CASUAL_GAME) {
            integer(
                name = "등급허용한도",
                description = "자신과 상대방의 등급 차이 허용 한도를 설정해요. 기본값은 1이에요. 설정하지 않으려면 -1을 넣어주세요."
            ).optional()
        }
        addCommand(MatchingCommand.RANK_GAME)
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

    private suspend fun registerProfile(interaction : ChatInputCommandInteraction) {
        val author = interaction.user.takeIf { it is Member } as Member? ?: run {
            interaction.respondPublic { embed { description = "누가 절 부르신거죠? 부르신 분을 못찾겠어요." } }
            return
        }

        val player = matchMakingManager.createProfile(
            author.id.value.toLong(),
            author.nickname
        ) ?: run {
            interaction.respondPublic { embed { description = "프로필 생성에 실패했어요." } }
            return
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

    private suspend fun showProfile(interaction : ChatInputCommandInteraction) {
        val author = interaction.user.takeIf { it is Member } as Member? ?: run {
            interaction.respondPublic {
                embed { description = "누가 절 부르신거죠? 부르신 분을 못찾겠어요." }
            }
            return
        }

        val user = interaction.command.users["프로필 출력 대상"] ?: author

        val player = matchMakingManager.getProfile(user.id.value.toLong()) ?: run {
            interaction.respondPublic { embed { description = "프로필이 등록되지 않았어요." } }
            return
        }

        interaction.respondPublic {
            embed {
                author {
                    name = user.username
                }

                description = player.getPlayerInfo()
            }
        }
    }

    private suspend fun registerGamePool(interaction : ChatInputCommandInteraction, gameType : GameType) {
        val author = interaction.user.takeIf { it is Member } as? Member ?: run {
            interaction.respondPublic {
                embed {
                    description = "누가 절 부르신거죠? 부르신 분을 못찾겠어요."
                }
            }
            return
        }

        val player = matchMakingManager.getProfile(author.id.value.toLong()) ?: run {
            interaction.respondPublic { embed { description = "프로필이 등록되지 않았어요." } }
            return
        }

        val rankAvailableRange = interaction.command.integers["등급 허용 한도"]?.toInt() ?: 1
        val matchArgs = MatchArgs(player.id, rankAvailableRange)

        matchMakingManager.addOnGameCreateListener(key = player) { game ->
            if (game == null) {
                interaction.channel.createMessage {
                    embed {
                        description = "${author.mention} 상대방을 찾지 못해 매칭이 취소되었어요."
                    }
                }
                return@addOnGameCreateListener
            }

            val p1Mention = author.guild.getMemberOrNull(Snowflake(game.player1Id))?.mention
            val p2Mention = author.guild.getMemberOrNull(Snowflake(game.player2Id))?.mention

            interaction.channel.createMessage {
                embed {
                    author {
                        name = "${p1Mention}, $p2Mention 매칭됐어요."
                    }
                    description = "1P : $p1Mention\n2P : $p2Mention\n\n 방을 생성한 후 게임을 진행해주세요."
                }
            }
        }

        matchMakingManager.addQueue(player, matchArgs, gameType)

        interaction.respondPublic {
            val gameTypeName = when (gameType) {
                GameType.CASUAL -> "캐주얼 게임"
                GameType.RANK -> "랭크 게임"
            }

            embed {
                description = "${author.mention}님이 $gameTypeName 매칭에 등록했어요."
            }
        }
    }

    private suspend fun registerScore(interaction : ChatInputCommandInteraction) {
        interaction.respondPublic {
            embed {
                description = "개발중이라고 애송이"
            }
        }
    }

    private fun Player.getPlayerInfo() =
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
