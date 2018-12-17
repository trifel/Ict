[DEPRECATED] Please use https://github.com/iotaledger/ict.

# Ict
Iota Controlled agenT

Software for the IOTA network nodes running on low-end devices, using the final design of the transaction structure,
swarm logic and relying on (static) Economic Clustering instead of the Coordinator for doublespending protection.

Extra service plugins can be attached to icts to allow their operators earn iotas.

Ict is being developed for the Internet, not for the Internet-of-Things.

## Installing

### Compile & Package

Maven and Java 8 is required.

```
$ git clone https://github.com/trifel/Ict.git
$ cd Ict
$ mvn clean compile
$ mvn package
```

This will create a `target` directory in which you will find the executable jar file that you can use.

### Configuration

#### Ict settings

Make sure you create and configure the file `ict.properties` before you continue.

```
// The host IP address is used to create the UDP socket and to provide the IXI API. 
// If the IP address is 0.0.0.0, the socket will be bound to the wildcard address, 
// an IP address chosen by the kernel. Mostly you don`t need to change the host IP.
host = 0.0.0.0

// Defines on which port your Ict instance is listen. The port is used for the UDP 
// socket and the IXI API. The local port must be between 0 and 65535 inclusive. 
// Important: 
// You need to open the port for UDP and TCP in your router and/or firewall settings.
port = 14265

// Insert the IP or domain and the port of your neighbor A.
neighborAHost = ?.?.?.?
neighborAPort = 14265

// Insert the IP or domain and the port of your neighbor B.
neighborBHost = ?.?.?.?
neighborBPort = 14265

// Insert the IP or domain and the port of your neighbor C.
neighborCHost = ?.?.?.?
neighborCPort = 14265
```

#### Optional API settings

To configure the settings of the API, create the file `api.properties` in your ICT installation's folder.

```
// User authentication to access the API. <user>:<password>
remoteAuth = 

// The defined commands are ignored by the API.
remoteLimitApi =

// Limits the number of characters the body of an API call may hold.
maxBodyLength = 1000000

// Limits the number of parameters in an API call. 
maxRequestsList = 1000
```

#### Port forwarding, firewall settings

Your Ict instance will only operate, if you open or forward the UDP and TCP port (defined 
in the file `ict.properties`) in your router and/or firewall settings. 

#### Install additional IXI plugins

Copy the IXI plugin to your ICT installation's `ixi`-folder.
Don't forget to consult the configuration documentation of the IXI plugin you are installing.

## Run Ict

```
$ java -jar ict-0.3.0-ixi-1.1.jar ict.properties
```
