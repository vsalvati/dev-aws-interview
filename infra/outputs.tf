output "alb_dns_name" {
  description = "ALB DNS name — your API endpoint"
  value       = aws_lb.main.dns_name
}

output "ecr_repository_url" {
  description = "ECR repository URL"
  value       = aws_ecr_repository.app.repository_url
}

output "rds_endpoint" {
  description = "RDS instance endpoint"
  value       = aws_db_instance.main.endpoint
}

output "sqs_queue_url" {
  description = "SQS task events queue URL"
  value       = aws_sqs_queue.task_events.url
}
