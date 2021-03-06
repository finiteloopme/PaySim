SERVICE=paysim
PUBSUB_TOPIC=raw-fin-transactions
PUBSUB_TOPIC_SUB=${PUBSUB_TOPIC}-sub
PROJECT_ID=kl-dev-scratchpad
GCS_STAGING=${PROJECT_ID}-staging
GCS_TEMP=${PROJECT_ID}-tmp
REGION=us-central1

package: clean compile
	mvn package
	#cd dataflow; go build -o ../target/dataflow

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

deploy: build env-setup
	gcloud run deploy --image gcr.io/${PROJECT_ID}/${SERVICE} --platform managed --allow-unauthenticated --quiet

delete: env-clean
	gcloud run services delete ${SERVICE} --quiet

env-setup:
	gcloud pubsub topics create ${PUBSUB_TOPIC}
	gcloud pubsub subscriptions create ${PUBSUB_TOPIC_SUB} --topic=${PUBSUB_TOPIC} --retain-acked-messages
	gsutil mb gs://${GCS_STAGING}
	gsutil mb gs://${GCS_TEMP}

env-clean:
	gcloud pubsub subscriptions delete ${PUBSUB_TOPIC_SUB}
	gcloud pubsub topics delete ${PUBSUB_TOPIC}
	gsutil rm -rf gs://${GCS_STAGING}
	gsutil rm -rf gs://${GCS_TEMP}
