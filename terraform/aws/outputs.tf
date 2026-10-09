output "alb_dns" {
  value = aws_lb.lab33.dns_name
}

output "cluster_name" {
  value = aws_ecs_cluster.lab33.name
}

output "rds_endpoint" {
  value = try(aws_db_instance.lab33[0].address, null)
}

output "account_id" {
  value = data.aws_caller_identity.current.account_id
}
