java_library(name = 'optimizer',
  dependencies = [
    'science/src/java/com/twitter/graphjet/bipartite/api',
    'science/src/java/com/twitter/graphjet/bipartite/edgepool',
    'science/src/java/com/twitter/graphjet/bipartite/segment',
  ],
  sources = globs('*.java'),
  provides = artifact(
    org = 'com.twitter.graphjet.bipartite',
    name = 'optimizer',
    repo = artifactory,
  ),
  description='Optimizers for a bipartite graph.'
)
