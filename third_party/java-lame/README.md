# java-lame (vendored)

The folder `app/src/main/java/net/sourceforge/lame` holds, unchanged, the sources of
https://github.com/nwaldispuehl/java-lame (master), a Java port of the LAME 3.98.4 MP3 encoder made by
Ken Händel for the jump3r project. Only the classes the encoder needs were copied (the command-line
front end and the javax.sound wrapper were left out).

License: GNU LGPL (see `LICENSE`), compatible with this project's GPL-3.0.
Why it is here: Android has no MP3 encoder, and a pure-Java encoder needs no native library.
