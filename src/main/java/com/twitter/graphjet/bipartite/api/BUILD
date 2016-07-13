java_library(name = 'api',
  dependencies = [
    '3rdparty/jvm/it/unimi/dsi:fastutil',
    '3rdparty/jvm/com/google/code/findbugs:jsr305',
  ],
  sources = globs('*.java'),
  provides = artifact(
    org = 'com.twitter.graphjet.bipartite',
    name = 'api',
    repo = artifactory,
  ),
  description='a public API package for graphs.'
)
