java_library(name = 'stats',
  sources = globs('*.java'),
  provides = artifact(
    org = 'com.twitter.graphjet',
    name = 'stats',
    repo = artifactory,
  ),
  description='A collection of stats interfaces for graphjet offering  \
    flexibility for clients to choose specific stat implementations. A \
    default implementation is also provided, which offers thread safe   \
    counters to be used throughout the library.'
)
