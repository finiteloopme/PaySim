build: clean compile
	mvn package

clean:
	mvn clean

compile:
	mvn compile

run: build
	mvn compile exec:java

