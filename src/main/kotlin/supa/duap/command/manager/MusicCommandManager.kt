package supa.duap.command.manager

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.optional
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.connect
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.create.embed
import dev.kord.voice.AudioFrame
import dev.kord.voice.VoiceConnection
import kotlinx.coroutines.launch
import supa.duap.command.model.Command.MusicCommand
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@KordVoice
class MusicCommandManager(kord : Kord) : CommandManager<MusicCommand>(kord) {

    private val playerInfoMap : MutableMap<Snowflake, PlayerInfo> = mutableMapOf()

    private val lavaPlayerManager = DefaultAudioPlayerManager().also { AudioSourceManagers.registerRemoteSources(it) }

    override suspend fun registerCommand() {
        addCommand(MusicCommand.PLAY) {
            string(
                name = "키워드",
                description = "YouTube 검색 키워드."
            )
        }

        addCommand(MusicCommand.SKIP)

        addCommand(MusicCommand.LIST)

        addCommand(MusicCommand.REMOVE) {
            string(
                name = "키워드",
                description = "재생목록 검색 키워드"
            ).optional()

            integer(
                name = "순번",
                description = "삭제할 음원이 위치한 재생목록 순번"
            ).optional()
        }

        addCommand(MusicCommand.DROP)
    }

    override suspend fun responseCommand(command : MusicCommand, interaction : ChatInputCommandInteraction) {
        when (command) {
            MusicCommand.PLAY -> processPlayCommand(interaction)
            MusicCommand.SKIP -> processSkipCommand(interaction)
            MusicCommand.LIST -> processListCommand(interaction)
            MusicCommand.REMOVE -> processRemoveCommand(interaction)
            MusicCommand.DROP -> processDropCommand(interaction)
        }
    }

    private suspend fun processPlayCommand(interaction : ChatInputCommandInteraction) {
        val author = interaction.user.takeIf { it is Member } as Member? ?: run {
            interaction.respondPublic { embed { description = "누가 절 부르신거죠? 부르신 분을 못찾겠어요." } }
            return
        }

        val channel = author.getChannel() ?: run {
            interaction.respondPublic { embed { description = "접속중이신 채널을 찾지 못했어요. 음성 채널에 입장한 후에 불러주세요." } }
            return
        }

        val keyword = interaction.command.strings["키워드"] ?: run {
            interaction.respondPublic { embed { description = "키워드가 없어요." } }
            return
        }

        val trackInfo = lavaPlayerManager.queryTrack("ytsearch: $keyword", author)
            ?: run {
                interaction.respondPublic { embed { description = "음원을 찾지 못했어요." } }
                return
            }

        val playerInfo = playerInfoMap[channel.guildId]
            ?: run {
                val player = lavaPlayerManager.createPlayer().apply { volume = 10 }
                PlayerInfo(
                    currentChannel = channel,
                    connection = channel.connect {
                        audioProvider { AudioFrame.fromData(player.provide()?.data) }
                    },
                    player = player
                )
                    .apply {
                        val audioListener = object : AudioEventAdapter() {
                            override fun onTrackEnd(
                                player : AudioPlayer?,
                                track : AudioTrack?,
                                endReason : AudioTrackEndReason?
                            ) {
                                if (playlist.isEmpty()) {
                                    isPlaying = false
                                    connection.scope.launch {
                                        currentChannel.createMessage {
                                            embed { description = "음원 재생이 끝났어요." }
                                        }

                                        playerInfoMap.remove(channel.guildId)
                                        connection.leave()
                                    }
                                    return
                                }

                                play(playlist.removeAt(0))
                            }
                        }

                        player.addListener(audioListener)

                        playerInfoMap[channel.guildId] = this
                    }
            }

        playerInfo.addTrack(trackInfo)

        interaction.respondPublic {
            embed {
                author {
                    this.name = "음원을 추가했어요."
                }

                description =
                    "[${trackInfo.track.info?.title}](${trackInfo.track.info?.uri}) - ${trackInfo.regUser.mention}"
                thumbnail { url = "${trackInfo.track.info?.artworkUrl}" }
            }
        }
    }

    private suspend fun processSkipCommand(interaction : ChatInputCommandInteraction) {
        val author = interaction.user.takeIf { it is Member } as Member? ?: run {
            interaction.respondPublic { embed { description = "누가 절 부르신거죠? 부르신 분을 못찾겠어요." } }
            return
        }

        val playerInfo = playerInfoMap[author.guildId] ?: run {
            interaction.respondPublic { embed { description = "재생중인 음원이 없어요." } }
            return
        }

        playerInfo.run {
            player.stopTrack()
            interaction.respondPublic { embed { description = "재생중인 음원을 스킵했어요." } }
        }
    }

    private suspend fun processListCommand(interaction : ChatInputCommandInteraction) {
        val author = interaction.user.takeIf { it is Member } as Member? ?: run {
            interaction.respondPublic { embed { description = "누가 절 부르신거죠? 부르신 분을 못찾겠어요." } }
            return
        }

        val playerInfo = playerInfoMap[author.guildId] ?: run {
            interaction.respondPublic { embed { description = "재생중인 음원이 없어요." } }
            return
        }

        val currentTrack = playerInfo.currentTrack ?: run {
            interaction.respondPublic { embed { description = "재생중인 음원이 없어요." } }
            return
        }

        interaction.respondPublic {
            embed {
                author {
                    this.name = "▶ [재생목록]"
                }

                field(
                    name = "재생중",
                    value = { "[${currentTrack.track.info?.title}](${currentTrack.track.info?.uri}) - ${currentTrack.regUser.mention}\n" }
                )

                thumbnail { this.url = "${currentTrack.track.info?.artworkUrl}" }

                if (playerInfo.playlist.isEmpty()) {
                    return@embed
                }

                field { } // for space between current track and playlist

                val playlistString = buildString {
                    playerInfo.playlist.forEachIndexed { index, trackInfo ->
                        appendLine("`#$index` [${trackInfo.track.info?.title}](${trackInfo.track.info?.uri}) - ${trackInfo.regUser.mention}\n")
                    }
                }

                field(
                    name = "대기열",
                    value = { playlistString }
                )
            }
        }
    }

    private suspend fun processRemoveCommand(interaction : ChatInputCommandInteraction) {
        val author = interaction.user.takeIf { it is Member } as Member? ?: run {
            interaction.respondPublic {
                embed { description = "누가 절 부르신거죠? 부르신 분을 못찾겠어요." }
            }
            return
        }

        val playerInfo = playerInfoMap[author.guildId] ?: run {
            interaction.respondPublic { embed { description = "재생 목록이 없어요." } }
            return
        }

        val index = interaction.command.integers["순번"] ?: run {
            interaction.respondPublic { embed { description = "음원 순번을 입력해주세요." } }
            return
        }

        if (playerInfo.playlist.size <= index) {
            interaction.respondPublic { embed { description = "음원을 찾지 못했어요." } }
            return
        }

        val trackInfo = playerInfo.playlist.removeAt(index.toInt())
        interaction.respondPublic {
            embed {
                author {
                    name = "음원을 지웠어요."
                }

                description = "[${trackInfo.track.info?.title}](${trackInfo.track.info?.uri})"
            }
        }
    }

    private suspend fun processDropCommand(interaction : ChatInputCommandInteraction) {
        val author = interaction.user.takeIf { it is Member } as Member? ?: run {
            interaction.respondPublic { embed { description = "누가 절 부르신거죠? 부르신 분을 못찾겠어요." } }
            return
        }

        val playerInfo = playerInfoMap[author.guildId] ?: run {
            interaction.respondPublic { embed { description = "재생 목록이 없어요." } }
            return
        }

        playerInfo.playlist.clear()
        interaction.respondPublic { embed { description = "재생 목록의 음원을 모두 지웠어요." } }
        return
    }

    private suspend fun DefaultAudioPlayerManager.queryTrack(query : String, member : Member) : TrackInfo? {
        val track = suspendCoroutine {
            this.loadItem(query, object : AudioLoadResultHandler {
                override fun trackLoaded(track : AudioTrack) {
                    it.resume(TrackInfo(track, member))
                }

                override fun playlistLoaded(playlist : AudioPlaylist) {
                    it.resume(TrackInfo(playlist.tracks.first(), member))
                }

                override fun noMatches() {
                    it.resume(null)
                }

                override fun loadFailed(exception : FriendlyException?) {
                    it.resume(null)
                }
            })
        }

        return track
    }

    private suspend fun Member.getChannel() : VoiceChannel? =
        getVoiceStateOrNull()?.channelId?.let {
            kord.getChannelOf<VoiceChannel>(it)
        }

    private fun PlayerInfo.play(trackInfo : TrackInfo) {
        isPlaying = true
        player.playTrack(trackInfo.track)

        currentTrack = trackInfo

        connection.scope.launch {
            currentChannel.createMessage {
                embed {
                    author {
                        this.name = "다음 음원을 재생할게요."
                    }

                    description =
                        "[${trackInfo.track.info?.title}](${trackInfo.track.info?.uri}) - ${trackInfo.regUser.mention}"
                    thumbnail { url = "${trackInfo.track.info?.artworkUrl}" }
                }
            }
        }
    }

    private fun PlayerInfo.addTrack(trackInfo : TrackInfo) {
        if (!isPlaying && playlist.isEmpty()) {
            play(trackInfo)
        } else {
            playlist.add(trackInfo)
        }
    }

    data class PlayerInfo(
        val connection : VoiceConnection,
        var currentChannel : VoiceChannel,
        val player : AudioPlayer,
        val playlist : MutableList<TrackInfo> = mutableListOf(),
    ) {
        var isPlaying : Boolean = false
        var currentTrack : TrackInfo? = null
    }

    data class TrackInfo(
        val track : AudioTrack,
        val regUser : Member
    )
}
