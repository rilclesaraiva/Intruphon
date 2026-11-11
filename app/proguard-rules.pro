-dontobfuscate
# R8 only runs on release. If something works in debug but breaks in release
# (usually reflection, e.g. serialization), add a -keep rule here; never disable R8.
