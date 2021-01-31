# EasyDrop
Direct encrypted file sharing via client/server and cross-platform GUI 

## What it does it do?
- Anonmyous file sharing 

## What technologies/frameworks are involved?

## What I learned from this project
- How to deal with difficulties such as package and version missmatchmaking
- Wrap A JAR file with scripts and build native OSX and Windows applications
> This was neccesary as JavaFX was no longer bundled in with Java the runtime, forcing you to link the new OpenJFX library as a starting argument.
- Application architechture plays a large role in how well you can scale. This application, for example, is completely monolothic, and scaling would be done vertically (through larger and larger machines). A more sophisticated architecture that can handle those previously stated problems would be needed to address scalability; I.E, monolithic applications are not feesible in a production environment.
