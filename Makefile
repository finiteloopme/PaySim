SERVICE=paysim
package: clean compile
	mvn package

clean:
	mvn clean

compile:
	mvn compile

run: package
	mvn compile exec:java

c-build: package
	docker build -t ${SERVICE}:latest .

c-run: #container-build
	docker run --rm -p 8080:8080 ${SERVICE}:latest 

build: package
	gcloud builds submit --tag gcr.io/${PROJECT_ID}/${SERVICE}

deploy: build
	gcloud run deploy --image gcr.io/${PROJECT_ID}/${SERVICE} --platform managed --allow-unauthenticated

delete:
	gcloud run services delete ${SERVICE}