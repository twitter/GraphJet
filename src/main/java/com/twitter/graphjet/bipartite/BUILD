java_library(name = 'bipartite',
  dependencies = [
    '3rdparty/jvm/it/unimi/dsi:fastutil',
    'science/src/java/com/twitter/graphjet/bipartite/api',
    'science/src/java/com/twitter/graphjet/bipartite/edgepool',
    'science/src/java/com/twitter/graphjet/bipartite/optimizer',
    'science/src/java/com/twitter/graphjet/bipartite/segment',
    'science/src/java/com/twitter/graphjet/math',
    'science/src/java/com/twitter/graphjet/stats',
  ],
  sources = globs('*.java'),
  provides = artifact(
    org = 'com.twitter.graphjet',
    name = 'bipartite',
    repo = artifactory,
  ),
  description='A bipartite graph package.'
)
