# Ict
Iota Controlled agenT

Software for the IOTA network nodes running on low-end devices, using the final design of the transaction structure,
swarm logic and relying on (static) Economic Clustering instead of the Coordinator for doublespending protection.

Extra service plugins can be attached to icts to allow their operators earn iotas.

Ict is being developed for the Internet, not for the Internet-of-Things.

## Installing

### compile & package

Maven and Java 8 is required.

```
$ git clone https://github.com/trifel/Ict.git
$ cd Ict
$ mvn clean compile
$ mvn package
```

This will create a `target` directory in which you will find the executable jar file that you can use.

### configuration

#### ict.properties

Make sure you create and configured the file `ict.properties` before you continue. Further information how you create the file: https://medium.com/@lambtho/iota-ict-installation-tutorial-c079a1ca3b7d

#### ixi plugins

You can find an example plugin here: https://github.com/trifel/Report.ixi

```
$ cd ixi
$ git clone https://github.com/trifel/Report.ixi.git
```

## run Ict

```
$ java -jar target/ict-0.3.1.jar ict.properties
```
