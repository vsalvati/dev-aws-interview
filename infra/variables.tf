variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "task-manager"
}

variable "db_username" {
  description = "RDS master username"
  type        = string
  default     = "taskmanager"
  sensitive   = true
}

variable "db_password" {
  description = "RDS master password"
  type        = string
  sensitive   = true
}

variable "container_image" {
  description = "Docker image URI for the task manager app"
  type        = string
}

variable "app_port" {
  description = "Application port"
  type        = number
  default     = 8080
}
