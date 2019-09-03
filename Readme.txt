Coinspermia.

Copyright (c) 2017 Tom Portegys (tom.portegys@ey.com). All rights reserved.
 
Redistribution and use in source and binary forms, with or without modification, are
permitted provided that the following conditions are met:
 
1. Redistributions of source code must retain the above copyright notice, this list of
   conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list
   of conditions and the following disclaimer in the documentation and/or other materials
   provided with the distribution.

THIS SOFTWARE IS PROVIDED BY TOM PORTEGYS "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 
-------------------------------------------------------------------------------
DESCRIPTION

Coinspermia is a cryptocurrency distributed in a network of peer nodes
that support secure and reliable transactions. Transactions are replicated
throughout the network for later query and update. A client can be assured
of the completion of an transaction when a quorum of the nodes validate it.

-------------------------------------------------------------------------------
OPERATION

Use the run scripts to start nodes and clients.
Build script runs the Maven mvn command (maven.apache.org).

Node options:
        [-port <port> (defaults to 8944)]
        [-bootstrapPeer <address of peer to connect to> (repeatable)]
        [-bootstrapPeerFile <file containing peer addresses> (defaults to peers.txt)]
        [-maxPeerConnections <maximum number of peer connections> (defaults to 100)]
        [-password <password> (use password authorization mode; password alternatively read from password.txt)]
        [-randomSeed <random number seed> (defaults to 4517)]
        [-logfile <log file name> (defaults to coinspermia.log) | "none"]
        
The -password option allows a password authorized network to be created. This prevents operation spoofing 
and unauthorized currency minting. See client Readme tab for further instructions.
-------------------------------------------------------------------------------
DEVELOPMENT

Coinspermia is an Eclipse Java project (eclipse.org)

Source repository: bitbucket.org/portnoid/coinspermia
(git clone https://portnoid@bitbucket.org/portnoid/coinspermia.git)

-------------------------------------------------------------------------------
REFERENCE

Thomas E. Portegys, "Coinspermia: a cryptocurrency unchained", the Future Technologies Conference (FTC) 2017, 29-30 November 2017 in Vancouver, BC, Canada.
tom.portegys.com/research.html#coinspermia

-------------------------------------------------------------------------------
RELEASE INFORMATION

v1.0
