# EasyDrop
Direct encrypted file sharing via client/server and cross-platform GUI

## What it does it do?
- Anonmyous file sharing via a client-server architechture
- Each user assigned a random PIN and uses that to find/address other users
- Encryption is 256 AES and handshakes are done through the single server application
- Files are zipped before sending and unzipped upon arrival

## What technologies/frameworks are involved?
- Java
- JavaFX/[OpenJFX](https://openjfx.io/) for cross-platform UI development
- [KryoNet](https://github.com/EsotericSoftware/kryonet) for high-level networking
- [Kryo](https://github.com/EsotericSoftware/kryo) (Java) for class serialization/deserialization and stream serialization/deserialization
- [Zip4J](https://github.com/srikanth-lingala/zip4j) (Java) for Zipping and Unzippign in Java


## What I learned from this project
- Best practices when searching for libraries/code to tackle a specific problem.
> At multiple stages in this project, I was constrained by Java and needed additional libraries. It is most important to ensure that a chosen library solves the task which you chose it for, it is just as important to choose a library that will save you development time. By prioritizing ease-of-use and familiarity of the product, for example choosing a native Java library, I saved uncountable hours in development. 
- How to deal with system related errors. I experienced many errors/crashes while deploying this application to OSX and Windows. I found it really important to read the documentation for the libraries and products I used, especially when it comes to best practices for debugging. These practices ended up saving me tens of hours in development time.
- XML and FXML format and popular editors.
- How to deal with difficulties such as package and version missmatchmaking
- Wrap A JAR file with scripts and build native OSX and Windows applications
> This was neccesary as JavaFX was no longer bundled in with Java the runtime, forcing you to link the new OpenJFX library as a starting argument.
- Application architechture plays an integral role in how well an application can scale in a production environment. Because this application is completely monolothic, scaling of this application would be done vertically through larger and larger machines via manual procceeses. A more sophisticated architecture is needed to address elastic scalability, for example, a solution using microservice architechture. Monolithic applications are not feesible in a production environment if elastic scalability is a requirement.

[Client Application Gif](https://i.gyazo.com/346f5f6dda1f1abe48c9c1410de140fa.mp4)

![Client Application](https://i.gyazo.com/f16f55add65bec9996d8c8df7e84d3f1.png)
![Server Application](https://i.gyazo.com/7bf29ff31750435c1a853afdabad946e.png)
