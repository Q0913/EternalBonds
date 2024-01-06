package com.creator.common.enums

object Enums {
    enum class PlayerRole{
        Client, Server
    }

    enum class FileRequestCode{
        VIDEO,PHOTO
    }

    enum class VideoType{
        MP4,MKV
    }
    enum class PlaybackSource{
        LOCAL_FILES,HTTP,SCREEN_CASTING
    }

    enum class IP{
        ipv4,ipv6,IPV4,IPV6
    }

    enum class PingResult{
        PingSuccess,PingFailed
    }

}