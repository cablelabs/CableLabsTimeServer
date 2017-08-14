# CableLabsTimeServer

This is a basic Time Protocol ([RFC868](https://tools.ietf.org/html/rfc868)) implementation.

By default, the server will listen for TCP and UDP time requests (port 37) on all interfaces.  Command line arguments can be supplied to listen for only TCP or only UDP packets on a specific interface/address.

By default, the client will perform a TCP time request against loopback.  Command line arguments can be supplied to perform a UDP time request, and against a remote server.
