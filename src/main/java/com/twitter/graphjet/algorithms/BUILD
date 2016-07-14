java_library(name = 'algorithms',
  dependencies = [
    '3rdparty/jvm/com/google/guava',
    '3rdparty/jvm/it/unimi/dsi:fastutil',
    'science/src/java/com/twitter/graphjet/bipartite',
    'science/src/java/com/twitter/graphjet/hashing',
  ],
  sources = rglobs('*.java'),
  provides = artifact(
    org = 'com.twitter.graphjet',
    name = 'algorithms',
    repo = artifactory,
  ),
  description='Real-time Content Suggestions based on heterogeneous graphs.'
)
