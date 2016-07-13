java_library(name = 'hashing',
  dependencies = [
    '3rdparty/jvm/com/google/guava:guava',
    '3rdparty/jvm/it/unimi/dsi:fastutil',
    '3rdparty/jvm/org/slf4j:slf4j-api',
    'science/src/java/com/twitter/graphjet/bipartite/api',
    'science/src/java/com/twitter/graphjet/stats',
  ],
  sources = globs('*.java'),
  provides = artifact(
    org = 'com.twitter.graphjet',
    name = 'hashing',
    repo = artifactory,
  ),
  description='A hashing util package for graphs.'
)
