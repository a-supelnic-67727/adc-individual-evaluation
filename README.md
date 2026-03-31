## Deploy local:

mvn appengine:run


## Deploy appEngine:

gcloud auth login

gcloud config set project [project-ID]

mvn clean package

mvn appengine:deploy -Dapp.deploy.projectId=individual-evaluation-491114 -Dapp.deploy.version=100
