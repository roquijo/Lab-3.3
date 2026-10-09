data "aws_availability_zones" "available" {
  state = "available"
}

resource "aws_vpc" "lab33" {
  cidr_block           = "10.22.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true
  tags                 = { Name = "lab33" }
}

resource "aws_internet_gateway" "lab33" {
  vpc_id = aws_vpc.lab33.id
  tags   = { Name = "lab33" }
}

resource "aws_subnet" "public" {
  count                   = 2
  vpc_id                  = aws_vpc.lab33.id
  cidr_block              = cidrsubnet(aws_vpc.lab33.cidr_block, 8, count.index)
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true
  tags                    = { Name = "lab33-public-${count.index}" }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.lab33.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.lab33.id
  }
  tags = { Name = "lab33-public" }
}

resource "aws_route_table_association" "public" {
  count          = 2
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_security_group" "alb" {
  name   = "lab33-alb"
  vpc_id = aws_vpc.lab33.id
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "tasks" {
  name   = "lab33-tasks"
  vpc_id = aws_vpc.lab33.id
  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "rds" {
  count  = var.create_rds ? 1 : 0
  name   = "lab33-rds"
  vpc_id = aws_vpc.lab33.id
  ingress {
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.tasks.id]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_db_subnet_group" "lab33" {
  count      = var.create_rds ? 1 : 0
  name       = "lab33"
  subnet_ids = aws_subnet.public[*].id
}

resource "aws_db_instance" "lab33" {
  count                  = var.create_rds ? 1 : 0
  identifier             = "lab33"
  engine                 = "mysql"
  engine_version         = "8.4"
  instance_class         = "db.t4g.micro"
  allocated_storage      = 20
  db_name                = "lab33"
  username               = "lab"
  password               = var.db_password
  db_subnet_group_name   = aws_db_subnet_group.lab33[0].name
  vpc_security_group_ids = [aws_security_group.rds[0].id]
  publicly_accessible    = false
  skip_final_snapshot    = true
  deletion_protection    = false
}
