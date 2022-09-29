#!/bin/bash

error_exit() {
  echo "$1" 1>&2
  exit 1
}

PROJECT_NAME=$1
# get the source version to be built (defaults to main branch if not specified)
SOURCE_VERSION=${2:-main}

echo "Starting CodeBuild project ${PROJECT_NAME}"

# dump all GITHUB_* environment variables to file and pass to start job
jq -n 'env | to_entries | [.[] | select(.key | startswith("GITHUB_"))] | [.[] | {name: .key, value:.value, type:"PLAINTEXT"}]' >/tmp/gh_env_vars.json

START_RESULT=$(
  aws codebuild start-build-batch \
    --project-name ${PROJECT_NAME} \
    --source-version $SOURCE_VERSION \
    --environment-variables-override file:///tmp/gh_env_vars.json
)

if [ "$?" != "0" ]; then
  error_exit "Could not start project. Exiting."
else
  echo "Build started successfully."
fi

BUILD_ID=$(echo ${START_RESULT} | jq '.buildBatch.id' -r)
echo "Build id $BUILD_ID"

BUILD_STATUS="IN_PROGRESS"
while [ "$BUILD_STATUS" == "IN_PROGRESS" ]; do
  echo "Checking build status."
  BUILD=$(aws codebuild batch-get-build-batches --ids ${BUILD_ID})
  BUILD_STATUS=$(echo $BUILD | jq '.buildBatches[0].buildBatchStatus' -r)

  JOBS=$(echo $BUILD | jq '.buildBatches[0].buildGroups | [.[] | {identifier: .identifier, status: .currentBuildSummary.buildStatus} | select(.identifier | startswith("JOB"))]')
  TOTAL_JOBS=$(echo $JOBS | jq 'length')

  SUCCEEDED_CNT=$(echo $JOBS | jq '[.[] | select(.status == "SUCCEEDED")] | length')
  IN_PROGRESS_CNT=$(echo $JOBS | jq '[.[] | select(.status == "IN_PROGRESS")] | length')

  FAILED_CNT=$(($TOTAL_JOBS - $SUCCEEDED_CNT - $IN_PROGRESS_CNT))

  if [ "$BUILD_STATUS" == "IN_PROGRESS" ]; then
    echo "Build is still in progress (failed=$FAILED_CNT; in_progress=$IN_PROGRESS_CNT; succeeded=$SUCCEEDED_CNT; total=$TOTAL_JOBS), waiting..."

  fi
  sleep 10
done

if [ "$BUILD_STATUS" != "SUCCEEDED" ]; then
  BUILD=$(aws codebuild batch-get-build-batches --ids ${BUILD_ID})
  FAILED_BUILDS=$(echo $BUILD | jq '.buildBatches[0].buildGroups | [.[] | {identifier: .identifier, status: .currentBuildSummary.buildStatus} | select(.status == "FAILED")]')
  echo "Failed builds in batch"
  echo $FAILED_BUILDS
  error_exit "Build failed, please review job output"
else
  echo "Build succeeded."
fi
