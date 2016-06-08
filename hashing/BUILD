java_library(name = 'hashing',
  dependencies = [
    '3rdparty/jvm/com/google/guava:guava',
    '3rdparty/jvm/it/unimi/dsi:fastutil',
    'finagle/finagle-stats',
    'science/src/java/com/twitter/graphjet/bipartite/api',
  ],
  sources = globs('*.java'),
  provides = artifact(
    org = 'com.twitter.graphjet',
    name = 'hashing',
    repo = artifactory,
  ),
  description='A hashing util package for graphs.'
)
