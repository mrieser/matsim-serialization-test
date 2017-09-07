# MATSim Serialization Test

This repository contains some sample code that serializes a MATSim population
in various formats:

* XML (Standard MATSim)
* [Kryo ](https://github.com/EsotericSoftware/kryo)
* [Protocol Buffers](https://developers.google.com/protocol-buffers/)
* [Avro](https://avro.apache.org)

The avro-implementation is provided by [michaz/avromatsim](https://github.com/michaz/matsimavro/).

As a test file, I used an output_plans.xml.gz from the open Santiago v1 scenario.

On my machine with a local SSD, I got the following results:

|   | time [s] | filesize [MB] |
|---|---|---|
|xml|4.044|440|
|xml.gz|7.869|20|
|kryo|9.613|319|
|kryo.lz4|8.980|31|
|pbf|3.168|261|
|pbf.lz4|2.795|28|
|pbf.gz|5.755|18|
|avro|4.665|276|

In general, the overhead of gzip-compression is pretty obvious in these results.
Lz4 compression is so fast, it is able to compensate the overhead by writing smaller files, resulting in faster operations when using lz4 than without.

Kryo is a kind of drop-in replacement for java serialization, very easy to use, but not
that great in performance or filesize.

Protocol Buffers (pbf) is the fastest and creates the smallest files (uncompressed).

Avro by itself is a bit slower than MATSim's XML encoding, but creates a smaller file.
On the other hand, a smaller file does not matter that much as long as some compression
is applied afterwards.
