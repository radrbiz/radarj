# radarj -- Radard's java client library
As the core component of the radar network, the **``radard``** (radar-dee) is a series of servers that run peer to peer software.

This **``radarj``** is the client side implementation to submit transactions, query accounts' info, etc.

##1, radar-lib
  Radar java library for account, transactions, ledger and crypto algorithms.

  For details, the whole development documentation was published in [https://radarlab.org/dev/](https://radarlab.org/dev/) .

  Repository contents in src/main/java/ :
####    api:
  implement wrap classes of radard WebSocket API .

  The WebSocket API interactive document is published in [https://radarlab.org/dev/radar-api-tool.html](https://radarlab.org/dev/radar-api-tool.html) .

####    btc:
  some functions of Bitcoin, for comparison
####    client:
  Network communication clients, including WebSocket, ZooKeeper ...
####    core:
  radar java package core, including data structure, hash, serialize ...


##2, radar-info-web

  Realtime data view pages for Radar System. And it also is an example for using radar-lib .

  The corresponding website built by this module is [https://info.radarlab.org/](https://info.radarlab.org/)

##3, ripple-bouncycastle

  Rippls's encrypt/decrypt package, derived from bouncycastle.org.




### License
``radarj`` is open source and permissively licensed under the ISC license.


###By radarlab.org



