java_library(name = 'segment',
  dependencies = [
    '3rdparty/jvm/com/google/code/findbugs:jsr305',
    '3rdparty/jvm/com/google/guava',
    '3rdparty/jvm/it/unimi/dsi:fastutil',
    'science/src/java/com/twitter/graphjet/bipartite/api',
    'science/src/java/com/twitter/graphjet/bipartite/edgepool',
    'science/src/java/com/twitter/graphjet/hashing',
    'science/src/java/com/twitter/graphjet/math',
    'science/src/java/com/twitter/graphjet/stats',
  ],
  sources = globs('*.java'),
  provides = artifact(
    org = 'com.twitter.graphjet.bipartite',
    name = 'segment',
    repo = artifactory,
  ),
  description='Segments for a bipartite graph.'
)
