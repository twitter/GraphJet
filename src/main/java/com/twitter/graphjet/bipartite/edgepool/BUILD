java_library(name = 'edgepool',
  dependencies = [
    '3rdparty/jvm/com/google/guava',
    '3rdparty/jvm/it/unimi/dsi:fastutil',
    'science/src/java/com/twitter/graphjet/bipartite/api',
    'science/src/java/com/twitter/graphjet/hashing',
    'science/src/java/com/twitter/graphjet/stats',
  ],
  sources = globs('*.java'),
  provides = artifact(
    org = 'com.twitter.graphjet.bipartite',
    name = 'edgepool',
    repo = artifactory,
  ),
  description='Edge pools for a bipartite graph.'
)
