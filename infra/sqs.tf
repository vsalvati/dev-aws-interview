resource "aws_sqs_queue" "task_events_dlq" {
  name                      = "${var.project_name}-task-events-dlq"
  message_retention_seconds = 1209600 # 14 days

  tags = { Name = "${var.project_name}-task-events-dlq" }
}

resource "aws_sqs_queue" "task_events" {
  name                       = "${var.project_name}-task-events"
  visibility_timeout_seconds = 30
  message_retention_seconds  = 345600 # 4 days

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.task_events_dlq.arn
    maxReceiveCount     = 3
  })

  tags = { Name = "${var.project_name}-task-events" }
}
