# NoteBlockRenderer
Library for rendering and playing Minecraft Note Block songs, built on [NoteBlockLib](https://github.com/RaphiMC/NoteBlockLib) and [AudioMixer](https://github.com/RaphiMC/AudioMixer).

To use NoteBlockRenderer in your application, check out the [Usage](#usage) section.

## Features
- Loads all formats supported by NoteBlockLib (.nbs, .mid, .txt, .mcsp and .mcsp2)
- Exports to all formats supported by AudioMixer (.wav, .flac, .mp3, .opus)
- Very accurate, high quality and fast song renderer
    - Supports Custom Instruments
    - Supports Tempo Changers
    - Supports Sound Stoppers
    - Optional timing jitter for a more natural sound
    - Optional multithreaded rendering
- Plays songs in real time using the Java Sound API (SourceDataLine)

## Releases
### Gradle/Maven
To use NoteBlockRenderer with Gradle/Maven you can get it from [Maven Central](https://mvnrepository.com/artifact/net.raphimc/note-block-renderer), [Lenni0451's Maven](https://maven.lenni0451.net/#/releases/net/raphimc/note-block-renderer) or [Jitpack](https://jitpack.io/#RaphiMC/NoteBlockRenderer).
You can also find instructions how to implement it into your build script there.

### Jar File
If you just want the latest jar file you can download it from [GitHub Actions](https://github.com/RaphiMC/NoteBlockRenderer/actions/workflows/build.yml).

## Usage
### Loading instrument sounds
A ``SoundProvider`` supplies the sound samples used for instruments. The following implementations are included:

- ``NoteBlockStudioSoundProvider`` loads sounds from the default Minecraft Note Block Studio sound directory for the current operating system. Use ``DirectorySoundProvider`` for a custom directory.
- ``DirectorySoundProvider`` loads sounds from a local directory, passed as a ``String``, ``File`` or ``Path``.
- ``ResourceSoundProvider`` loads sounds from classpath resources, such as files bundled in your application's JAR. It defaults to the classpath root.
- ``UrlSoundProvider`` downloads Minecraft instrument sounds from a Minecraft assets repository by default. You can pass a different base URI to its constructor.
- ``NoOpSoundProvider`` supplies no sounds and throws an ``IOException`` for every request. Can be used as a last provider in a chain to get a clear error message if a sound is missing.

``DirectorySoundProvider`` and ``ResourceSoundProvider`` look up Minecraft sounds at ``minecraft/<mcSoundPath>.ogg`` relative to their base path.

You can chain providers using ``first.then(second)``. The second provider is used if the first provider throws an ``IOException`` while loading the requested sound sample. For example, try Note Block Studio sounds first, then bundled resources, and finally download missing Minecraft sounds:

```java
SoundProvider soundProvider = new NoteBlockStudioSoundProvider()
    .then(new ResourceSoundProvider("sounds/"))
    .then(new UrlSoundProvider());
```

### Rendering a song
Create a ``NoteBlockRenderer`` with a song, an ``AudioFormat`` and a sound provider.
Call ``renderFully(<progressConsumer>)`` to render the entire song. The progress consumer receives percentages from 0 to 100.

### Playing a song
Create a ``NoteBlockPlayer`` with a ``SourceDataLine``, a song and a sound provider. The player is paused by default. Call ``setPaused(false)`` to begin playback.
You can pause and resume using ``setPaused``, seek using ``setTick``, ``setProgress`` or ``setMillisecondPosition``, and read the current position using ``getTick``, ``getProgress`` or ``getMillisecondPosition``.
Call ``close()`` when you are done with the player to stop its audio writer and close the audio line.

### Configuring rendering
Both renderers accept an ``AudioMixerNoteBlockRenderer.Options`` instance in their constructor. Use its ``with...`` methods to change the options you want.

## Examples
Complete examples with imports can be found in the [src/example](/src/example) directory:

- [FileExample](/src/example/java/FileExample.java) renders a song and saves it as MP3.
- [RealtimeExample](/src/example/java/RealtimeExample.java) plays a song with volume, position and pause controls.

Here is a list of reference implementations that use NoteBlockRenderer:

- [nbs-cli](https://github.com/Lenni0451/nbs-cli): CLI tool for working with NBS files.
- [NoteBlockTool-Web](https://github.com/RaphiMC/NoteBlockTool-Web): Web App for rendering Minecraft Note Block Studio songs to audio files.

## Contact
If you encounter any issues, please report them on the
[issue tracker](https://github.com/RaphiMC/NoteBlockRenderer/issues).  
If you just want to talk or need help implementing NoteBlockRenderer feel free to join my
[Discord](https://raphimc.net/discord).
