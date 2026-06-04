#!/bin/bash
awslocal sqs create-queue --queue-name task-events-dlq
awslocal sqs create-queue --queue-name task-events \
  --attributes '{
    "RedrivePolicy": "{\"deadLetterTargetArn\":\"arn:aws:sqs:us-east-1:000000000000:task-events-dlq\",\"maxReceiveCount\":\"3\"}"
  }'
echo "SQS queues created."
