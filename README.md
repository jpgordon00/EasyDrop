# EasyDrop
Direct encrypted file sharing via client/server and cross-platform GUI 

## What it does it do?
- Anonmyous file sharing 

## What technologies/frameworks are involved?
- Java
- JavaFX/OpenJFX for cross-platform UI development
- [KryoNet](https://github.com/EsotericSoftware/kryonet) for high-level networking
- [Kryo](https://github.com/EsotericSoftware/kryo) (Java) for class serialization/deserialization and stream serialization/deserialization
- [Zip4J](https://github.com/srikanth-lingala/zip4j) (Java) for Zipping and Unzippign in Java


## What I learned from this project
- Best practices when searching for libraries to address specific issues.
> At multiple stages 
- XML and FXML
- How to deal with difficulties such as package and version missmatchmaking
- Wrap A JAR file with scripts and build native OSX and Windows applications
> This was neccesary as JavaFX was no longer bundled in with Java the runtime, forcing you to link the new OpenJFX library as a starting argument.
- Application architechture plays an integral role in how well an application can scale. Because this application is completely monolothic, scaling of the application would be done vertically if ever deployed to a Cloud provider. A more sophisticated architecture is needed to address elastic scalability, for example microservices. Monolithic applications are not feesible in a production environment.
