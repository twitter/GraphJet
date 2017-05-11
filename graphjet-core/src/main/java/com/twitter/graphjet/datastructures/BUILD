java_library(name = 'datastructures',
  dependencies = [
    '3rdparty/jvm/com/google/guava:guava',
    '3rdparty/jvm/it/unimi/dsi:fastutil',
    '3rdparty/jvm/org/slf4j:slf4j-api',
    'science/src/java/com/twitter/graphjet/stats',
  ],
  sources = globs('*.java'),
  provides = artifact(
    org = 'com.twitter.graphjet',
    name = 'datastructures',
    repo = artifactory,
  ),
  description='A data structure util package for graphs.'
)
