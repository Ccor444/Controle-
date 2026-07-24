package com.example.model

enum class RemoteCommand(val label: String) {
    POWER("Power"),
    POWER_OFF("Desligar"),
    POWER_ON("Ligar"),
    VOLUME_UP("Vol +"),
    VOLUME_DOWN("Vol -"),
    MUTE("Mute"),
    CHANNEL_UP("Ch +"),
    CHANNEL_DOWN("Ch -"),
    
    // Directional
    UP("Cima"),
    DOWN("Baixo"),
    LEFT("Esquerda"),
    RIGHT("Direita"),
    OK("OK"),
    
    // System Navigation
    BACK("Voltar"),
    HOME("Home"),
    MENU("Menu"),
    INFO("Info"),
    INPUT("Entrada/HDMI"),
    
    // Media Playback
    PLAY("Play"),
    PAUSE("Pause"),
    PLAY_PAUSE("Play/Pause"),
    STOP("Parar"),
    FORWARD("Avançar"),
    REWIND("Retroceder"),
    
    // Numbers
    NUM_0("0"),
    NUM_1("1"),
    NUM_2("2"),
    NUM_3("3"),
    NUM_4("4"),
    NUM_5("5"),
    NUM_6("6"),
    NUM_7("7"),
    NUM_8("8"),
    NUM_9("9"),
    
    // Apps
    NETFLIX("Netflix"),
    YOUTUBE("YouTube"),
    PRIME_VIDEO("Prime Video"),
    DISNEY_PLUS("Disney+"),
    SPOTIFY("Spotify"),
    BROWSER("Navegador")
}
