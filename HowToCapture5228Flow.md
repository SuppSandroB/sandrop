Added is mode that can be used to capture any protocol over ssl and then examined with wireshark

// start sandroproxy with no capture, fake certificates, transparent proxy and only 5228 redirect in APPS tab

// export sandroproxy CA to android store Menu->Export CA

// create dump

_adb shell "tcpdump port 5228 or port 8010 -n -i lo -s 0 -w /mnt/sdcard/sp\_5228.pcap"_

// kill some process that will make new connection to 5228 -> com.google.process.gapps

// finish tcpdump, copy from device to PC

_adb pull /mnt/sdcard/sp\_5228.pcap_

// get sandroproxy certificates

_adb pull sandroproxy\_dir/cache/.keystorecerts_

// use portecle to import(pwd:password)/export cert for 173.194.70.188\_5288 as p12 file and import it to wireshark

// fix it so we have ssl flow that wireshark can decrypt

_tcprewrite --fixcsum --portmap 8010:443,5228:443 --pnat 127.0.0.1:173.194.70.188 --infile sp\_5228.pcap --outfile sp\_fixed\_5228.pcap_

// open sp\_fixed\_5228.pcap in wireshark

What can go wrong:

-full ssl handshake must be captured so wireshark can  be able to decrypt

-client do not support ssl cipher that is used in sandroproxy

http://code.google.com/p/sandrop/source/browse/projects/SandroProxyLib/src/org/sandrop/webscarab/plugin/proxy/ConnectionHandler.java?name=1_4_79_pass_through_fake_certificates#525

This feature of capturing flow will be upgraded to:

_Next phase of this feature will be to make separate file on each tcp stream.
Content will be bytes. Can be also shown as text.
On apps tab you will have new options to select which app or ports you are interested. Capture can be plain or ssl with mitm.
SP knows where is connecting, which process, source port, destination tcpip/port.
All this can be in the name of file or even make sqlite table for mapping.
So there can be then bunch of files and user can decide which to examine.
Probably there should be sqlite table. So you can for example select process and all files for it will be shown._