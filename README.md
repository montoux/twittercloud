# TwitterCloud: Clojure stream processing example

This is an example application for demonstrating Clojure lazy data processing
techniques. Clojure provides a lot of useful tools for handling data but
without care these can end up being quite slow. This application processes data
from Twitter's APIs, optionally in real time, to demonstrate handling a large
dynamic data stream. The implementation provided is too slow to process any
large amount of data - and twitter will hang up if you can't keep up.

In order to read data from twitter you'll need to create an account, create an
app, and add the secrets to `.twitter/auth.clj` in your home directory. You can
use `create-example` from a REPL to generate some sample data files. The
`tweet-frequencies` function is the focus of this demo, it has a very naive and
slow implementation of a lazy stream processor that accumulates a map
hashtag->frequency values. You can connect it directly to a twitter stream, but
for testing it's probably best to use an example file.
