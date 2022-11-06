maven_clean_install = ./mvnw install
maven_clean = ./mvnw clean

build:
	@echo "Building jar"
	@$(maven_clean_install)

run: build
	@java -jar target/paxos-suburbs-council-election-1.0-SNAPSHOT.jar file:///home/vishal/Documents/paxos-suburbs-council-election/config/config_1.json

clean:
	@echo "Cleaning existing resources"
	@$(maven_clean)